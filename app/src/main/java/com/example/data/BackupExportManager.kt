package com.example.data

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.domain.Category
import com.example.domain.CurrencyCode
import com.example.domain.DashboardMetrics
import com.example.domain.FinanceMath
import com.example.domain.FinanceTransaction
import com.example.domain.RecurringBill
import com.example.domain.RecurringFrequency
import com.example.domain.SavingsGoal
import com.example.domain.TransactionType
import com.example.domain.WalXDateUtils
import com.example.domain.Wallet
import com.example.domain.WalletType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object BackupExportManager {

    fun buildCsv(snapshot: UserExportSnapshot): String {
        val catNames = snapshot.categories.associate { it.id to it.name }
        val walletNames = snapshot.wallets.associate { it.id to it.name }
        val sb = StringBuilder()
        sb.appendLine("Date,Type,Title,Amount,Currency,Category,Wallet,ToWallet,Tags,Notes")
        snapshot.transactions.forEach { tx ->
            val row = listOf(
                tx.date,
                tx.type.displayName,
                tx.title,
                FinanceMath.roundCurrency(tx.amount).toString(),
                snapshot.user.currency.code,
                tx.categoryId?.let { catNames[it] }.orEmpty(),
                walletNames[tx.walletId].orEmpty(),
                tx.toWalletId?.let { walletNames[it] }.orEmpty(),
                tx.tags.joinToString(";"),
                tx.notes
            ).joinToString(",") { escapeCsv(it) }
            sb.appendLine(row)
        }
        return sb.toString()
    }

    fun buildJsonBackup(snapshot: UserExportSnapshot): String {
        val root = JSONObject().apply {
            put("app", "WalX")
            put("creator", "Nor Gaxmer Builds")
            put("schemaVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("currency", snapshot.user.currency.code)
            put("monthlyBudget", FinanceMath.roundCurrency(snapshot.user.monthlyBudget))

            put("wallets", JSONArray().apply {
                snapshot.wallets.forEach { w ->
                    put(JSONObject().apply {
                        put("id", w.id)
                        put("name", w.name)
                        put("type", w.type.name)
                        put("balance", FinanceMath.roundCurrency(w.balance))
                        put("creditLimit", FinanceMath.roundCurrency(w.creditLimit))
                        put("colorHex", w.colorHex)
                    })
                }
            })

            put("categories", JSONArray().apply {
                snapshot.categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("type", c.type.name)
                        put("monthlyLimit", FinanceMath.roundCurrency(c.monthlyLimit))
                        put("colorHex", c.colorHex)
                        put("iconKey", c.iconKey)
                    })
                }
            })

            put("transactions", JSONArray().apply {
                snapshot.transactions.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("type", t.type.name)
                        put("amount", FinanceMath.roundCurrency(t.amount))
                        put("title", t.title)
                        put("categoryId", t.categoryId ?: JSONObject.NULL)
                        put("walletId", t.walletId)
                        put("toWalletId", t.toWalletId ?: JSONObject.NULL)
                        put("date", t.date)
                        put("notes", t.notes)
                        put("tags", JSONArray(t.tags))
                        put("createdAt", t.createdAt)
                    })
                }
            })

            put("goals", JSONArray().apply {
                snapshot.goals.forEach { g ->
                    put(JSONObject().apply {
                        put("id", g.id)
                        put("name", g.name)
                        put("targetAmount", FinanceMath.roundCurrency(g.targetAmount))
                        put("savedAmount", FinanceMath.roundCurrency(g.savedAmount))
                        put("deadline", g.deadline)
                        put("colorHex", g.colorHex)
                    })
                }
            })

            put("recurringBills", JSONArray().apply {
                snapshot.recurringBills.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("title", r.title)
                        put("amount", FinanceMath.roundCurrency(r.amount))
                        put("categoryId", r.categoryId ?: JSONObject.NULL)
                        put("walletId", r.walletId)
                        put("frequency", r.frequency.name)
                        put("nextDueDate", r.nextDueDate)
                        put("notes", r.notes)
                    })
                }
            })
        }
        return root.toString(2)
    }

    fun parseJsonBackup(userId: String, rawJson: String): Result<ParsedBackupPayload> {
        return try {
            val root = JSONObject(rawJson)
            if (root.optString("app") != "WalX") {
                return Result.failure(IllegalArgumentException("File is not a valid WalX backup JSON."))
            }
            val currency = CurrencyCode.fromCode(root.optString("currency", "INR"))
            val monthlyBudget = FinanceMath.roundCurrency(root.optDouble("monthlyBudget", 50000.0))

            // Remap imported IDs with userId namespace to prevent cross-user primary key collisions
            val walletIdMap = mutableMapOf<String, String>()
            val catIdMap = mutableMapOf<String, String>()

            val wallets = mutableListOf<Wallet>()
            val wArr = root.optJSONArray("wallets") ?: JSONArray()
            for (i in 0 until wArr.length()) {
                val obj = wArr.getJSONObject(i)
                val oldId = obj.optString("id", UUID.randomUUID().toString())
                val newId = "${userId}_imp_w_${UUID.randomUUID().toString().take(8)}"
                walletIdMap[oldId] = newId
                wallets.add(
                    Wallet(
                        id = newId,
                        userId = userId,
                        name = obj.optString("name", "Wallet").take(40),
                        type = WalletType.fromName(obj.optString("type")),
                        balance = FinanceMath.roundCurrency(obj.optDouble("balance", 0.0)),
                        creditLimit = FinanceMath.roundCurrency(obj.optDouble("creditLimit", 0.0)),
                        colorHex = obj.optLong("colorHex", 0xFF6366F1L)
                    )
                )
            }

            val categories = mutableListOf<Category>()
            val cArr = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until cArr.length()) {
                val obj = cArr.getJSONObject(i)
                val oldId = obj.optString("id", UUID.randomUUID().toString())
                val newId = "${userId}_imp_c_${UUID.randomUUID().toString().take(8)}"
                catIdMap[oldId] = newId
                categories.add(
                    Category(
                        id = newId,
                        userId = userId,
                        name = obj.optString("name", "Category").take(40),
                        type = TransactionType.fromName(obj.optString("type")),
                        monthlyLimit = FinanceMath.roundCurrency(obj.optDouble("monthlyLimit", 0.0)),
                        colorHex = obj.optLong("colorHex", 0xFF6366F1L),
                        iconKey = obj.optString("iconKey", "category")
                    )
                )
            }

            val fallbackWalletId = wallets.firstOrNull()?.id ?: "${userId}_w_main"
            val transactions = mutableListOf<FinanceTransaction>()
            val tArr = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until tArr.length()) {
                val obj = tArr.getJSONObject(i)
                val tagsJson = obj.optJSONArray("tags") ?: JSONArray()
                val tags = (0 until tagsJson.length()).mapNotNull { idx ->
                    tagsJson.optString(idx).trim().takeIf { it.isNotEmpty() }
                }
                val date = obj.optString("date", WalXDateUtils.todayIso())
                val safeDate = if (WalXDateUtils.isValidIsoDate(date)) date else WalXDateUtils.todayIso()
                val rawCat = obj.optString("categoryId", "")
                val rawTo = obj.optString("toWalletId", "")
                transactions.add(
                    FinanceTransaction(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        type = TransactionType.fromName(obj.optString("type")),
                        amount = FinanceMath.roundCurrency(obj.optDouble("amount", 0.0).coerceAtLeast(0.0)),
                        title = obj.optString("title", "Transaction").take(80),
                        categoryId = if (rawCat.isNotBlank() && rawCat != "null") catIdMap[rawCat] else null,
                        walletId = walletIdMap[obj.optString("walletId")] ?: fallbackWalletId,
                        toWalletId = if (rawTo.isNotBlank() && rawTo != "null") walletIdMap[rawTo] else null,
                        date = safeDate,
                        notes = obj.optString("notes", "").take(300),
                        tags = tags,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val goals = mutableListOf<SavingsGoal>()
            val gArr = root.optJSONArray("goals") ?: JSONArray()
            for (i in 0 until gArr.length()) {
                val obj = gArr.getJSONObject(i)
                goals.add(
                    SavingsGoal(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        name = obj.optString("name", "Savings Goal").take(60),
                        targetAmount = FinanceMath.roundCurrency(obj.optDouble("targetAmount", 1000.0).coerceAtLeast(1.0)),
                        savedAmount = FinanceMath.roundCurrency(obj.optDouble("savedAmount", 0.0).coerceAtLeast(0.0)),
                        deadline = obj.optString("deadline", WalXDateUtils.daysFromNowIso(90)),
                        colorHex = obj.optLong("colorHex", 0xFF10B981L)
                    )
                )
            }

            val recurring = mutableListOf<RecurringBill>()
            val rArr = root.optJSONArray("recurringBills") ?: JSONArray()
            for (i in 0 until rArr.length()) {
                val obj = rArr.getJSONObject(i)
                val rawCat = obj.optString("categoryId", "")
                recurring.add(
                    RecurringBill(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = obj.optString("title", "Recurring Bill").take(60),
                        amount = FinanceMath.roundCurrency(obj.optDouble("amount", 0.0).coerceAtLeast(0.0)),
                        categoryId = if (rawCat.isNotBlank() && rawCat != "null") catIdMap[rawCat] else null,
                        walletId = walletIdMap[obj.optString("walletId")] ?: fallbackWalletId,
                        frequency = RecurringFrequency.fromName(obj.optString("frequency")),
                        nextDueDate = obj.optString("nextDueDate", WalXDateUtils.daysFromNowIso(30)),
                        notes = obj.optString("notes", "").take(200)
                    )
                )
            }

            Result.success(
                ParsedBackupPayload(
                    currency = currency,
                    monthlyBudget = monthlyBudget,
                    wallets = wallets,
                    categories = categories,
                    transactions = transactions,
                    goals = goals,
                    recurringBills = recurring
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid JSON backup file: ${e.localizedMessage}"))
        }
    }

    fun printFinancialStatement(
        context: Context,
        snapshot: UserExportSnapshot,
        metrics: DashboardMetrics
    ) {
        val html = buildPrintableHtml(snapshot, metrics)
        val webView = WebView(context)
        webView.settings.allowFileAccess = false
        webView.settings.javaScriptEnabled = false
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val jobName = "WalX_Statement_${WalXDateUtils.todayIso()}"
                val adapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun buildPrintableHtml(snapshot: UserExportSnapshot, metrics: DashboardMetrics): String {
        val cur = snapshot.user.currency
        val catNames = snapshot.categories.associate { it.id to it.name }
        val walletNames = snapshot.wallets.associate { it.id to it.name }

        val walletRows = snapshot.wallets.joinToString("") { w ->
            val extra = if (w.type == WalletType.CREDIT_CARD && w.creditLimit > 0.0) {
                " (${FinanceMath.safePercent(w.balance, w.creditLimit)}% of ${FinanceMath.formatCurrency(w.creditLimit, cur)})"
            } else ""
            "<tr><td>${escapeHtml(w.name)}</td><td>${escapeHtml(w.type.displayName)}</td><td class='num'>${escapeHtml(FinanceMath.formatCurrency(w.balance, cur))}$extra</td></tr>"
        }

        val txRows = snapshot.transactions.take(50).joinToString("") { tx ->
            val cat = tx.categoryId?.let { catNames[it] } ?: if (tx.type == TransactionType.TRANSFER) "Transfer" else "General"
            val wName = walletNames[tx.walletId] ?: "-"
            val cls = when (tx.type) {
                TransactionType.INCOME -> "inc"
                TransactionType.EXPENSE -> "exp"
                TransactionType.TRANSFER -> "trf"
            }
            "<tr><td>${escapeHtml(tx.date)}</td><td>${escapeHtml(tx.title)}</td><td>${escapeHtml(cat)}</td><td>${escapeHtml(wName)}</td><td class='num $cls'>${escapeHtml(FinanceMath.formatCurrency(tx.amount, cur))}</td></tr>"
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8"/>
            <style>
              body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #0F172A; margin: 28px; }
              .header { display: flex; justify-content: space-between; align-items: baseline; border-bottom: 2px solid #4F46E5; padding-bottom: 12px; margin-bottom: 20px; }
              .brand { font-size: 24px; font-weight: 800; color: #0F172A; letter-spacing: -0.5px; }
              .sub { font-size: 12px; color: #475569; margin-top: 2px; }
              .meta { text-align: right; font-size: 12px; color: #475569; }
              .grid { display: flex; gap: 12px; margin-bottom: 20px; }
              .kpi { flex: 1; border: 1px solid #E2E8F0; border-radius: 10px; padding: 12px; background: #F8FAFC; }
              .kpi-label { font-size: 11px; color: #64748B; text-transform: uppercase; letter-spacing: 0.5px; }
              .kpi-val { font-size: 18px; font-weight: 700; margin-top: 4px; font-variant-numeric: tabular-nums; }
              h2 { font-size: 14px; text-transform: uppercase; letter-spacing: 0.6px; color: #334155; margin: 20px 0 8px 0; }
              table { width: 100%; border-collapse: collapse; font-size: 12px; }
              th { text-align: left; background: #F1F5F9; padding: 8px; border-bottom: 1px solid #CBD5E1; color: #334155; }
              td { padding: 7px 8px; border-bottom: 1px solid #E2E8F0; }
              .num { text-align: right; font-variant-numeric: tabular-nums; font-weight: 600; }
              .inc { color: #059669; } .exp { color: #E11D48; } .trf { color: #0284C7; }
              .footer { margin-top: 28px; padding-top: 10px; border-top: 1px solid #E2E8F0; font-size: 11px; color: #64748B; text-align: center; }
            </style>
            </head>
            <body>
              <div class="header">
                <div>
                  <div class="brand">WalX Financial Statement</div>
                  <div class="sub">by Nor Gaxmer Builds</div>
                </div>
                <div class="meta">
                  <div><strong>${escapeHtml(snapshot.user.fullName)}</strong> (${escapeHtml(snapshot.user.email)})</div>
                  <div>Generated: ${WalXDateUtils.todayIso()} | Currency: ${cur.label}</div>
                </div>
              </div>
              <div class="grid">
                <div class="kpi"><div class="kpi-label">Total Net Worth</div><div class="kpi-val">${escapeHtml(FinanceMath.formatCurrency(metrics.totalNetWorth, cur))}</div></div>
                <div class="kpi"><div class="kpi-label">Month Income</div><div class="kpi-val inc">${escapeHtml(FinanceMath.formatCurrency(metrics.monthIncome, cur))}</div></div>
                <div class="kpi"><div class="kpi-label">Month Expenses</div><div class="kpi-val exp">${escapeHtml(FinanceMath.formatCurrency(metrics.monthExpense, cur))}</div></div>
                <div class="kpi"><div class="kpi-label">Savings Rate</div><div class="kpi-val">${metrics.savingsRatePercent}%</div></div>
              </div>
              <h2>Wallets & Accounts</h2>
              <table><thead><tr><th>Wallet</th><th>Type</th><th class="num">Balance</th></tr></thead><tbody>$walletRows</tbody></table>
              <h2>Recent Ledger Activity</h2>
              <table><thead><tr><th>Date</th><th>Title</th><th>Category</th><th>Wallet</th><th class="num">Amount</th></tr></thead><tbody>$txRows</tbody></table>
              <div class="footer">WalX Personal Finance Command Center &bull; Crafted by Nor Gaxmer Builds</div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun escapeHtml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
