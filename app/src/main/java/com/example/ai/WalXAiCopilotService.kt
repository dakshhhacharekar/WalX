package com.example.ai

import android.util.Log
import com.example.domain.Category
import com.example.domain.CurrencyCode
import com.example.domain.DashboardMetrics
import com.example.domain.FinanceMath
import com.example.domain.ParsedAiTransaction
import com.example.domain.TransactionType
import com.example.domain.WalXDateUtils
import com.example.domain.Wallet
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val AI_TAG = "WalXAiCopilot"
private const val DEFAULT_MODEL = "gemini-3.8-flash"

class WalXAiCopilotService {

    private val quickLogSchema = Schema.obj(
        properties = mapOf(
            "type" to Schema.enumeration(
                values = listOf("EXPENSE", "INCOME", "TRANSFER"),
                description = "Whether this is an EXPENSE, INCOME, or TRANSFER"
            ),
            "amount" to Schema.double(description = "Positive numeric transaction amount"),
            "title" to Schema.string(description = "Short merchant or transaction title"),
            "categoryName" to Schema.string(description = "Best matching category name from the user's available categories"),
            "walletName" to Schema.string(description = "Best matching wallet name from the user's available wallets"),
            "date" to Schema.string(description = "Transaction date in YYYY-MM-DD format"),
            "notes" to Schema.string(description = "Brief helpful note if extra context exists")
        ),
        optionalProperties = listOf("notes")
    )

    private val quickLogModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quickLogSchema
                temperature = 0.1f
            }
        )
    }

    private val insightsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                temperature = 0.5f
            }
        )
    }

    suspend fun parseNaturalLanguageTransaction(
        userText: String,
        categories: List<Category>,
        wallets: List<Wallet>,
        currency: CurrencyCode
    ): Result<ParsedAiTransaction> {
        val today = WalXDateUtils.todayIso()
        val catList = categories.joinToString(", ") { "${it.name} (${it.type.name})" }
        val walletList = wallets.joinToString(", ") { "${it.name} (${it.type.displayName})" }

        val prompt = """
            Parse this personal finance natural language note into a structured transaction.
            Today's date is $today. User currency is ${currency.code} (${currency.symbol}).
            Available categories: [$catList]
            Available wallets: [$walletList]
            User input: "$userText"
            Pick the closest matching categoryName and walletName from the lists above.
            If no date is mentioned, use $today. If 'yesterday', subtract 1 day from $today.
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) {
                quickLogModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val json = JSONObject(rawJson)

            val parsedType = TransactionType.fromName(json.optString("type", "EXPENSE"))
            val parsedAmount = FinanceMath.roundCurrency(json.optDouble("amount", 0.0).coerceAtLeast(0.0))
            if (parsedAmount <= 0.0) {
                return Result.failure(IllegalArgumentException("Could not detect a valid amount. Try including a number like '540'."))
            }
            val parsedTitle = json.optString("title", "").trim().ifBlank { userText.take(40) }
            val parsedCat = json.optString("categoryName", "")
            val parsedWallet = json.optString("walletName", "")
            val rawDate = json.optString("date", today)
            val safeDate = if (WalXDateUtils.isValidIsoDate(rawDate)) rawDate else today
            val parsedNotes = json.optString("notes", "")

            Result.success(
                ParsedAiTransaction(
                    type = parsedType,
                    amount = parsedAmount,
                    title = parsedTitle,
                    categoryName = parsedCat,
                    walletName = parsedWallet,
                    date = safeDate,
                    notes = parsedNotes
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Quick-log timed out", e)
            Result.failure(Exception("AI Quick-Log timed out after 45s. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Quick-log failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI Quick-Log failed. Check your connection."))
        }
    }

    fun streamSpendingInsights(
        metrics: DashboardMetrics,
        currency: CurrencyCode,
        wallets: List<Wallet>
    ): Flow<String> = flow {
        val topCategories = metrics.categoryBreakdown.take(5).joinToString("; ") {
            "${it.categoryName}: ${FinanceMath.formatCurrency(it.amount, currency)} (${it.percentage}%)"
        }.ifBlank { "No category spending logged this month." }

        val creditSummary = wallets.filter { it.type.name == "CREDIT_CARD" }.joinToString("; ") {
            "${it.name}: ${FinanceMath.formatCurrency(it.balance, currency)} / ${FinanceMath.formatCurrency(it.creditLimit, currency)} (${FinanceMath.safePercent(it.balance, it.creditLimit)}%)"
        }.ifBlank { "No credit cards." }

        val prompt = """
            You are WalX AI Copilot, a crisp personal finance analyst.
            Analyze this user's current month financial snapshot (${currency.label}):
            - Net Worth: ${FinanceMath.formatCurrency(metrics.totalNetWorth, currency)}
            - Month Income: ${FinanceMath.formatCurrency(metrics.monthIncome, currency)} (MoM: ${metrics.incomeMomPercent}%)
            - Month Expenses: ${FinanceMath.formatCurrency(metrics.monthExpense, currency)} (MoM: ${metrics.expenseMomPercent}%)
            - Savings Rate: ${metrics.savingsRatePercent}%
            - Monthly Budget: ${FinanceMath.formatCurrency(metrics.budgetLimit, currency)} (${metrics.budgetUsedPercent}% used, Daily safe-spend: ${FinanceMath.formatCurrency(metrics.dailySafeSpend, currency)} for ${metrics.daysLeftInMonth} days left)
            - Recurring Fixed Burn Rate: ${FinanceMath.formatCurrency(metrics.monthlyFixedBurnRate, currency)}/mo
            - Top Categories: $topCategories
            - Credit Cards: $creditSummary

            Provide 3 concise, high-signal, actionable bullet points (plain text, no markdown asterisks) covering:
            1) Budget pacing & daily runway
            2) Top spending concentration or credit utilization
            3) One concrete money-saving move this week.
            Keep the total response under 110 words.
        """.trimIndent()

        val sb = StringBuilder()
        withTimeout(60_000L) {
            insightsModel.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    sb.append(text)
                    emit(sb.toString())
                }
            }
        }
        if (sb.isBlank()) {
            throw IllegalStateException("AI Copilot returned an empty analysis.")
        }
    }
}
