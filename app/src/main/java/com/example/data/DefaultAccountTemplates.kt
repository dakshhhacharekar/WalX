package com.example.data

import com.example.domain.FinanceMath
import com.example.domain.TransactionType
import com.example.domain.WalletType

/**
 * Provides clean starter categories and an initial primary wallet for newly registered users.
 */
object DefaultAccountTemplates {
    const val LEGACY_DEMO_USER_ID = "walx_demo_user_001"

    fun createDefaultCategories(userId: String): List<CategoryEntity> {
        fun id(slug: String) = "${userId}_cat_$slug"
        return listOf(
            CategoryEntity(id("food"), userId, "Food & Dining", TransactionType.EXPENSE.name, 0.0, 0xFFF43F5EL, "restaurant"),
            CategoryEntity(id("groceries"), userId, "Groceries", TransactionType.EXPENSE.name, 0.0, 0xFF10B981L, "shopping_cart"),
            CategoryEntity(id("transport"), userId, "Transport", TransactionType.EXPENSE.name, 0.0, 0xFF38BDF8L, "directions_car"),
            CategoryEntity(id("housing"), userId, "Housing & Rent", TransactionType.EXPENSE.name, 0.0, 0xFF6366F1L, "home"),
            CategoryEntity(id("utilities"), userId, "Utilities & Bills", TransactionType.EXPENSE.name, 0.0, 0xFFF59E0BL, "bolt"),
            CategoryEntity(id("shopping"), userId, "Shopping", TransactionType.EXPENSE.name, 0.0, 0xFFEC4899L, "shopping_bag"),
            CategoryEntity(id("entertainment"), userId, "Entertainment", TransactionType.EXPENSE.name, 0.0, 0xFFA855F7L, "movie"),
            CategoryEntity(id("health"), userId, "Health & Fitness", TransactionType.EXPENSE.name, 0.0, 0xFF14B8A6L, "favorite"),
            CategoryEntity(id("subscriptions"), userId, "Subscriptions", TransactionType.EXPENSE.name, 0.0, 0xFF8B5CF6L, "subscriptions"),
            CategoryEntity(id("salary"), userId, "Salary", TransactionType.INCOME.name, 0.0, 0xFF10B981L, "payments"),
            CategoryEntity(id("freelance"), userId, "Freelance", TransactionType.INCOME.name, 0.0, 0xFF38BDF8L, "work"),
            CategoryEntity(id("investments"), userId, "Investments", TransactionType.INCOME.name, 0.0, 0xFF6366F1L, "trending_up")
        )
    }

    fun createStarterWallet(userId: String, initialBalance: Double = 0.0): WalletEntity = WalletEntity(
        id = "${userId}_w_main",
        userId = userId,
        name = "Primary Bank",
        type = WalletType.BANK.name,
        balance = FinanceMath.roundCurrency(initialBalance),
        creditLimit = 0.0,
        colorHex = 0xFF6366F1L
    )
}
