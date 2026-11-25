package com.example.domain

enum class CurrencyCode(val code: String, val symbol: String, val label: String) {
    INR("INR", "₹", "INR (₹)"),
    USD("USD", "$", "USD ($)"),
    EUR("EUR", "€", "EUR (€)"),
    GBP("GBP", "£", "GBP (£)"),
    JPY("JPY", "¥", "JPY (¥)");

    companion object {
        fun fromCode(code: String?): CurrencyCode =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INR
    }
}

enum class WalletType(val displayName: String) {
    BANK("Bank"),
    CASH("Cash"),
    CREDIT_CARD("Credit Card"),
    DIGITAL_WALLET("Digital Wallet");

    companion object {
        fun fromName(value: String?): WalletType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BANK
    }
}

enum class TransactionType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer");

    companion object {
        fun fromName(value: String?): TransactionType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXPENSE
    }
}

enum class RecurringFrequency(val displayName: String, val monthlyMultiplier: Double) {
    WEEKLY("Weekly", 4.3333),
    MONTHLY("Monthly", 1.0),
    YEARLY("Yearly", 1.0 / 12.0);

    companion object {
        fun fromName(value: String?): RecurringFrequency =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
    }
}

enum class DateRangeFilter(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_90_DAYS("Last 90 Days"),
    ALL_TIME("All Time")
}

enum class LedgerSortOption(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount")
}

enum class CashflowRange(val label: String) {
    LAST_14_DAYS("Last 14 Days"),
    LAST_6_MONTHS("Last 6 Months")
}

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val currency: CurrencyCode,
    val monthlyBudget: Double,
    val darkMode: Boolean,
    val createdAt: Long
) {
    val initials: String
        get() = fullName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "WX" }
}

data class Wallet(
    val id: String,
    val userId: String,
    val name: String,
    val type: WalletType,
    val balance: Double,
    val creditLimit: Double = 0.0,
    val colorHex: Long = 0xFF6366F1L
)

data class Category(
    val id: String,
    val userId: String,
    val name: String,
    val type: TransactionType,
    val monthlyLimit: Double = 0.0,
    val colorHex: Long = 0xFF6366F1L,
    val iconKey: String = "category"
)

data class FinanceTransaction(
    val id: String,
    val userId: String,
    val type: TransactionType,
    val amount: Double,
    val title: String,
    val categoryId: String?,
    val walletId: String,
    val toWalletId: String? = null,
    val date: String, // YYYY-MM-DD
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class SavingsGoal(
    val id: String,
    val userId: String,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val deadline: String, // YYYY-MM-DD
    val colorHex: Long = 0xFF10B981L
)

data class RecurringBill(
    val id: String,
    val userId: String,
    val title: String,
    val amount: Double,
    val categoryId: String?,
    val walletId: String,
    val frequency: RecurringFrequency,
    val nextDueDate: String, // YYYY-MM-DD
    val notes: String = ""
)

data class CashflowPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

data class CategorySpendSlice(
    val categoryId: String,
    val categoryName: String,
    val colorHex: Long,
    val amount: Double,
    val percentage: Double
)

data class DashboardMetrics(
    val totalNetWorth: Double = 0.0,
    val totalLiquidAssets: Double = 0.0,
    val totalCreditDebt: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val prevMonthIncome: Double = 0.0,
    val prevMonthExpense: Double = 0.0,
    val savingsRatePercent: Double = 0.0,
    val prevSavingsRatePercent: Double = 0.0,
    val incomeMomPercent: Double = 0.0,
    val expenseMomPercent: Double = 0.0,
    val budgetLimit: Double = 0.0,
    val budgetUsedPercent: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val dailySafeSpend: Double = 0.0,
    val daysLeftInMonth: Int = 1,
    val cashflow14Days: List<CashflowPoint> = emptyList(),
    val cashflow6Months: List<CashflowPoint> = emptyList(),
    val categoryBreakdown: List<CategorySpendSlice> = emptyList(),
    val monthlyFixedBurnRate: Double = 0.0
)

data class ParsedAiTransaction(
    val type: TransactionType,
    val amount: Double,
    val title: String,
    val categoryName: String,
    val walletName: String,
    val date: String,
    val notes: String
)
