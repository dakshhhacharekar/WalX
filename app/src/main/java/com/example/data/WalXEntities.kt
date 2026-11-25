package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.Category
import com.example.domain.CurrencyCode
import com.example.domain.FinanceTransaction
import com.example.domain.RecurringBill
import com.example.domain.RecurringFrequency
import com.example.domain.SavingsGoal
import com.example.domain.TransactionType
import com.example.domain.UserProfile
import com.example.domain.Wallet
import com.example.domain.WalletType

@Entity(
    tableName = "walx_users_v1",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val resetPinHash: String,
    val resetPinSalt: String,
    val currencyCode: String,
    val monthlyBudget: Double,
    val darkMode: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "walx_wallets",
    indices = [Index(value = ["userId"])]
)
data class WalletEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val type: String,
    val balance: Double,
    val creditLimit: Double,
    val colorHex: Long
)

@Entity(
    tableName = "walx_categories",
    indices = [Index(value = ["userId"])]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val type: String,
    val monthlyLimit: Double,
    val colorHex: Long,
    val iconKey: String
)

@Entity(
    tableName = "walx_transactions",
    indices = [Index(value = ["userId"]), Index(value = ["date"])]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val amount: Double,
    val title: String,
    val categoryId: String?,
    val walletId: String,
    val toWalletId: String?,
    val date: String,
    val notes: String,
    val tagsCsv: String,
    val createdAt: Long
)

@Entity(
    tableName = "walx_goals",
    indices = [Index(value = ["userId"])]
)
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val deadline: String,
    val colorHex: Long
)

@Entity(
    tableName = "walx_recurring",
    indices = [Index(value = ["userId"])]
)
data class RecurringBillEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val amount: Double,
    val categoryId: String?,
    val walletId: String,
    val frequency: String,
    val nextDueDate: String,
    val notes: String
)

fun UserEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    fullName = fullName,
    email = email,
    currency = CurrencyCode.fromCode(currencyCode),
    monthlyBudget = monthlyBudget,
    darkMode = darkMode,
    createdAt = createdAt
)

fun WalletEntity.toDomain(): Wallet = Wallet(
    id = id,
    userId = userId,
    name = name,
    type = WalletType.fromName(type),
    balance = balance,
    creditLimit = creditLimit,
    colorHex = colorHex
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    userId = userId,
    name = name,
    type = TransactionType.fromName(type),
    monthlyLimit = monthlyLimit,
    colorHex = colorHex,
    iconKey = iconKey
)

fun TransactionEntity.toDomain(): FinanceTransaction = FinanceTransaction(
    id = id,
    userId = userId,
    type = TransactionType.fromName(type),
    amount = amount,
    title = title,
    categoryId = categoryId,
    walletId = walletId,
    toWalletId = toWalletId,
    date = date,
    notes = notes,
    tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    createdAt = createdAt
)

fun SavingsGoalEntity.toDomain(): SavingsGoal = SavingsGoal(
    id = id,
    userId = userId,
    name = name,
    targetAmount = targetAmount,
    savedAmount = savedAmount,
    deadline = deadline,
    colorHex = colorHex
)

fun RecurringBillEntity.toDomain(): RecurringBill = RecurringBill(
    id = id,
    userId = userId,
    title = title,
    amount = amount,
    categoryId = categoryId,
    walletId = walletId,
    frequency = RecurringFrequency.fromName(frequency),
    nextDueDate = nextDueDate,
    notes = notes
)
