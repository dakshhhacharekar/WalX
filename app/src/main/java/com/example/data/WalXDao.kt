package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalXDao {
    // Users
    @Query("SELECT * FROM walx_users_v1 WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM walx_users_v1 WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM walx_users_v1 WHERE id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT COUNT(*) FROM walx_users_v1")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM walx_users_v1 WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    // Wallets
    @Query("SELECT * FROM walx_wallets WHERE userId = :userId ORDER BY name ASC")
    fun observeWallets(userId: String): Flow<List<WalletEntity>>

    @Query("SELECT * FROM walx_wallets WHERE userId = :userId")
    suspend fun getWallets(userId: String): List<WalletEntity>

    @Query("SELECT * FROM walx_wallets WHERE id = :walletId AND userId = :userId LIMIT 1")
    suspend fun getWalletById(walletId: String, userId: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallet(wallet: WalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallets(wallets: List<WalletEntity>)

    @Query("DELETE FROM walx_wallets WHERE id = :walletId AND userId = :userId")
    suspend fun deleteWallet(walletId: String, userId: String)

    @Query("DELETE FROM walx_wallets WHERE userId = :userId")
    suspend fun deleteAllWalletsForUser(userId: String)

    // Categories
    @Query("SELECT * FROM walx_categories WHERE userId = :userId ORDER BY type ASC, name ASC")
    fun observeCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM walx_categories WHERE userId = :userId")
    suspend fun getCategories(userId: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM walx_categories WHERE id = :categoryId AND userId = :userId")
    suspend fun deleteCategory(categoryId: String, userId: String)

    @Query("DELETE FROM walx_categories WHERE userId = :userId")
    suspend fun deleteAllCategoriesForUser(userId: String)

    // Transactions
    @Query("SELECT * FROM walx_transactions WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    fun observeTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM walx_transactions WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    suspend fun getTransactions(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM walx_transactions WHERE id = :txId AND userId = :userId LIMIT 1")
    suspend fun getTransactionById(txId: String, userId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(txs: List<TransactionEntity>)

    @Query("DELETE FROM walx_transactions WHERE id = :txId AND userId = :userId")
    suspend fun deleteTransaction(txId: String, userId: String)

    @Query("DELETE FROM walx_transactions WHERE userId = :userId")
    suspend fun deleteAllTransactionsForUser(userId: String)

    @Query("UPDATE walx_transactions SET walletId = :fallbackWalletId WHERE walletId = :deletedWalletId AND userId = :userId")
    suspend fun reassignTransactionSourceWallet(deletedWalletId: String, fallbackWalletId: String, userId: String)

    @Query("UPDATE walx_transactions SET toWalletId = :fallbackWalletId WHERE toWalletId = :deletedWalletId AND userId = :userId")
    suspend fun reassignTransactionDestWallet(deletedWalletId: String, fallbackWalletId: String, userId: String)

    @Query("UPDATE walx_transactions SET categoryId = :fallbackCategoryId WHERE categoryId = :deletedCategoryId AND userId = :userId")
    suspend fun reassignTransactionCategory(deletedCategoryId: String, fallbackCategoryId: String?, userId: String)

    @Query("UPDATE walx_recurring SET walletId = :fallbackWalletId WHERE walletId = :deletedWalletId AND userId = :userId")
    suspend fun reassignRecurringWallet(deletedWalletId: String, fallbackWalletId: String, userId: String)

    @Query("UPDATE walx_recurring SET categoryId = :fallbackCategoryId WHERE categoryId = :deletedCategoryId AND userId = :userId")
    suspend fun reassignRecurringCategory(deletedCategoryId: String, fallbackCategoryId: String?, userId: String)

    // Savings Goals
    @Query("SELECT * FROM walx_goals WHERE userId = :userId ORDER BY deadline ASC")
    fun observeGoals(userId: String): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM walx_goals WHERE userId = :userId")
    suspend fun getGoals(userId: String): List<SavingsGoalEntity>

    @Query("SELECT * FROM walx_goals WHERE id = :goalId AND userId = :userId LIMIT 1")
    suspend fun getGoalById(goalId: String, userId: String): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: SavingsGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: List<SavingsGoalEntity>)

    @Query("DELETE FROM walx_goals WHERE id = :goalId AND userId = :userId")
    suspend fun deleteGoal(goalId: String, userId: String)

    @Query("DELETE FROM walx_goals WHERE userId = :userId")
    suspend fun deleteAllGoalsForUser(userId: String)

    // Recurring Bills
    @Query("SELECT * FROM walx_recurring WHERE userId = :userId ORDER BY nextDueDate ASC")
    fun observeRecurringBills(userId: String): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM walx_recurring WHERE userId = :userId")
    suspend fun getRecurringBills(userId: String): List<RecurringBillEntity>

    @Query("SELECT * FROM walx_recurring WHERE id = :billId AND userId = :userId LIMIT 1")
    suspend fun getRecurringBillById(billId: String, userId: String): RecurringBillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringBill(bill: RecurringBillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringBills(bills: List<RecurringBillEntity>)

    @Query("DELETE FROM walx_recurring WHERE id = :billId AND userId = :userId")
    suspend fun deleteRecurringBill(billId: String, userId: String)

    @Query("DELETE FROM walx_recurring WHERE userId = :userId")
    suspend fun deleteAllRecurringBillsForUser(userId: String)
}
