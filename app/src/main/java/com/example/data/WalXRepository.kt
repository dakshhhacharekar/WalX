package com.example.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.domain.Category
import com.example.domain.CurrencyCode
import com.example.domain.FinanceMath
import com.example.domain.FinanceTransaction
import com.example.domain.PasswordHasher
import com.example.domain.RecurringBill
import com.example.domain.RecurringFrequency
import com.example.domain.SavingsGoal
import com.example.domain.TransactionType
import com.example.domain.UserProfile
import com.example.domain.WalXDateUtils
import com.example.domain.Wallet
import com.example.domain.WalletType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface AuthResult {
    data class Success(val user: UserProfile) : AuthResult
    data class Error(val message: String) : AuthResult
}

class WalXRepository(
    private val db: WalXDatabase,
    private val sessionManager: SessionManager
) {
    private val dao = db.walXDao()

    // --- Auth & Session ---

    /**
     * Removes any leftover legacy demo account from earlier builds so only real user accounts exist.
     */
    suspend fun purgeLegacyDemoAccountIfPresent() {
        val legacyId = DefaultAccountTemplates.LEGACY_DEMO_USER_ID
        val legacy = dao.getUserById(legacyId)
        if (legacy != null) {
            if (sessionManager.getRememberedUserId() == legacyId) {
                sessionManager.clearSession()
            }
            db.withTransaction {
                dao.deleteAllTransactionsForUser(legacyId)
                dao.deleteAllRecurringBillsForUser(legacyId)
                dao.deleteAllGoalsForUser(legacyId)
                dao.deleteAllWalletsForUser(legacyId)
                dao.deleteAllCategoriesForUser(legacyId)
                dao.deleteUserById(legacyId)
            }
        }
    }

    suspend fun hasRegisteredAccounts(): Boolean = dao.getUserCount() > 0

    suspend fun getRememberedUser(): UserProfile? {
        val userId = sessionManager.getRememberedUserId() ?: return null
        val user = dao.getUserById(userId)
        if (user == null) {
            sessionManager.clearSession()
            return null
        }
        return user.toDomain()
    }

    suspend fun signUp(
        fullName: String,
        email: String,
        password: String,
        resetPin: String,
        currency: CurrencyCode,
        startingBalance: Double = 0.0
    ): AuthResult {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPin = resetPin.trim()

        if (cleanName.length < 2) return AuthResult.Error("Please enter your full name (at least 2 characters).")
        if (!isValidEmail(cleanEmail)) return AuthResult.Error("Please enter a valid email address.")
        if (password.length < 6) return AuthResult.Error("Password must be at least 6 characters.")
        if (cleanPin.length < 4) return AuthResult.Error("Recovery PIN must be at least 4 characters.")

        if (dao.getUserByEmail(cleanEmail) != null) {
            return AuthResult.Error("An account with $cleanEmail already exists. Please log in instead.")
        }

        val userId = "usr_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val (passHash, pinHash) = withContext(Dispatchers.Default) {
            PasswordHasher.hash(password) to PasswordHasher.hash(cleanPin)
        }
        val defaultBudget = when (currency) {
            CurrencyCode.INR -> 50000.0
            CurrencyCode.JPY -> 200000.0
            else -> 2500.0
        }

        val entity = UserEntity(
            id = userId,
            fullName = cleanName,
            email = cleanEmail,
            passwordHash = passHash.hashHex,
            passwordSalt = passHash.saltHex,
            resetPinHash = pinHash.hashHex,
            resetPinSalt = pinHash.saltHex,
            currencyCode = currency.code,
            monthlyBudget = defaultBudget,
            darkMode = true,
            createdAt = System.currentTimeMillis()
        )

        return try {
            db.withTransaction {
                dao.insertUser(entity)
                dao.upsertCategories(DefaultAccountTemplates.createDefaultCategories(userId))
                dao.upsertWallet(
                    DefaultAccountTemplates.createStarterWallet(
                        userId = userId,
                        initialBalance = FinanceMath.roundCurrency(startingBalance.coerceAtLeast(0.0))
                    )
                )
            }
            sessionManager.saveSession(userId, rememberMe = true)
            AuthResult.Success(entity.toDomain())
        } catch (_: SQLiteConstraintException) {
            AuthResult.Error("An account with $cleanEmail already exists. Please log in instead.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Unable to create account. Please try again.")
        }
    }

    suspend fun logIn(email: String, password: String, rememberMe: Boolean): AuthResult {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) return AuthResult.Error("Please enter a valid email address.")
        if (password.isEmpty()) return AuthResult.Error("Please enter your password.")

        val user = dao.getUserByEmail(cleanEmail)
            ?: return AuthResult.Error("No WalX account found for $cleanEmail. Check your email or create an account.")

        val valid = withContext(Dispatchers.Default) {
            PasswordHasher.verify(password, user.passwordHash, user.passwordSalt)
        }
        if (!valid) {
            return AuthResult.Error("Incorrect password. Please try again or use Forgot Password.")
        }

        sessionManager.saveSession(user.id, rememberMe)
        return AuthResult.Success(user.toDomain())
    }

    suspend fun resetPasswordWithPin(email: String, resetPin: String, newPassword: String): AuthResult {
        val cleanEmail = email.trim().lowercase()
        val cleanPin = resetPin.trim()
        if (!isValidEmail(cleanEmail)) return AuthResult.Error("Please enter a valid email address.")
        if (cleanPin.length < 4) return AuthResult.Error("Enter your 4+ character Recovery PIN.")
        if (newPassword.length < 6) return AuthResult.Error("New password must be at least 6 characters.")

        val user = dao.getUserByEmail(cleanEmail)
            ?: return AuthResult.Error("No account registered with $cleanEmail.")

        val pinValid = withContext(Dispatchers.Default) {
            PasswordHasher.verify(cleanPin, user.resetPinHash, user.resetPinSalt)
        }
        if (!pinValid) {
            return AuthResult.Error("Recovery PIN did not match our records.")
        }

        val newHash = withContext(Dispatchers.Default) {
            PasswordHasher.hash(newPassword)
        }
        val updated = user.copy(passwordHash = newHash.hashHex, passwordSalt = newHash.saltHex)
        dao.updateUser(updated)
        return AuthResult.Success(updated.toDomain())
    }

    fun signOut() {
        sessionManager.clearSession()
    }

    // --- Reactive Observations (Strictly Scoped to userId) ---

    fun observeUser(userId: String): Flow<UserProfile?> =
        dao.observeUser(userId).map { it?.toDomain() }

    fun observeWallets(userId: String): Flow<List<Wallet>> =
        dao.observeWallets(userId).map { list -> list.map { it.toDomain() } }

    fun observeCategories(userId: String): Flow<List<Category>> =
        dao.observeCategories(userId).map { list -> list.map { it.toDomain() } }

    fun observeTransactions(userId: String): Flow<List<FinanceTransaction>> =
        dao.observeTransactions(userId).map { list -> list.map { it.toDomain() } }

    fun observeGoals(userId: String): Flow<List<SavingsGoal>> =
        dao.observeGoals(userId).map { list -> list.map { it.toDomain() } }

    fun observeRecurringBills(userId: String): Flow<List<RecurringBill>> =
        dao.observeRecurringBills(userId).map { list -> list.map { it.toDomain() } }

    // --- Profile & Preferences ---

    suspend fun updateProfile(
        userId: String,
        fullName: String,
        currency: CurrencyCode,
        monthlyBudget: Double,
        darkMode: Boolean
    ) {
        val existing = dao.getUserById(userId) ?: return
        dao.updateUser(
            existing.copy(
                fullName = fullName.trim().ifBlank { existing.fullName },
                currencyCode = currency.code,
                monthlyBudget = FinanceMath.roundCurrency(monthlyBudget.coerceAtLeast(0.0)),
                darkMode = darkMode
            )
        )
    }

    suspend fun toggleTheme(userId: String, darkMode: Boolean) {
        val existing = dao.getUserById(userId) ?: return
        dao.updateUser(existing.copy(darkMode = darkMode))
    }

    // --- Atomic Transaction Engine (Zero Balance Drift) ---

    suspend fun addTransaction(tx: FinanceTransaction) {
        val roundedAmount = FinanceMath.roundCurrency(tx.amount.coerceAtLeast(0.0))
        if (roundedAmount <= 0.0) return

        db.withTransaction {
            val entity = TransactionEntity(
                id = tx.id.ifBlank { UUID.randomUUID().toString() },
                userId = tx.userId,
                type = tx.type.name,
                amount = roundedAmount,
                title = tx.title.trim(),
                categoryId = if (tx.type == TransactionType.TRANSFER) null else tx.categoryId,
                walletId = tx.walletId,
                toWalletId = if (tx.type == TransactionType.TRANSFER) tx.toWalletId else null,
                date = tx.date,
                notes = tx.notes.trim(),
                tagsCsv = tx.tags.joinToString(","),
                createdAt = tx.createdAt
            )
            dao.upsertTransaction(entity)
            applyWalletEffect(entity, reverse = false)
        }
    }

    suspend fun updateTransaction(updatedTx: FinanceTransaction) {
        val roundedAmount = FinanceMath.roundCurrency(updatedTx.amount.coerceAtLeast(0.0))
        if (roundedAmount <= 0.0) return

        db.withTransaction {
            val oldEntity = dao.getTransactionById(updatedTx.id, updatedTx.userId) ?: return@withTransaction
            // Step 1: Reverse old balance effect atomically
            applyWalletEffect(oldEntity, reverse = true)

            // Step 2: Persist updated transaction and apply new balance effect
            val newEntity = TransactionEntity(
                id = oldEntity.id,
                userId = oldEntity.userId,
                type = updatedTx.type.name,
                amount = roundedAmount,
                title = updatedTx.title.trim(),
                categoryId = if (updatedTx.type == TransactionType.TRANSFER) null else updatedTx.categoryId,
                walletId = updatedTx.walletId,
                toWalletId = if (updatedTx.type == TransactionType.TRANSFER) updatedTx.toWalletId else null,
                date = updatedTx.date,
                notes = updatedTx.notes.trim(),
                tagsCsv = updatedTx.tags.joinToString(","),
                createdAt = oldEntity.createdAt
            )
            dao.upsertTransaction(newEntity)
            applyWalletEffect(newEntity, reverse = false)
        }
    }

    suspend fun deleteTransaction(userId: String, txId: String) {
        db.withTransaction {
            val oldEntity = dao.getTransactionById(txId, userId) ?: return@withTransaction
            applyWalletEffect(oldEntity, reverse = true)
            dao.deleteTransaction(txId, userId)
        }
    }

    /**
     * Applies or reverses a transaction's balance delta on liquid vs credit card wallets.
     * - Liquid wallets (BANK, CASH, DIGITAL_WALLET): Expense subtracts, Income adds.
     * - Credit Cards (CREDIT_CARD): balance represents owed debt, so Expense adds to debt,
     *   Income/Refund subtracts from debt, Transfer-To pays off card debt (subtracts).
     */
    private suspend fun applyWalletEffect(tx: TransactionEntity, reverse: Boolean) {
        val sign = if (reverse) -1.0 else 1.0
        val amount = FinanceMath.roundCurrency(tx.amount)
        val type = TransactionType.fromName(tx.type)

        val sourceWallet = dao.getWalletById(tx.walletId, tx.userId)
        if (sourceWallet != null) {
            val isCredit = WalletType.fromName(sourceWallet.type) == WalletType.CREDIT_CARD
            val delta = when (type) {
                TransactionType.EXPENSE -> if (isCredit) amount else -amount
                TransactionType.INCOME -> if (isCredit) -amount else amount
                TransactionType.TRANSFER -> if (isCredit) amount else -amount
            }
            val newBal = FinanceMath.roundCurrency(sourceWallet.balance + (delta * sign))
            dao.upsertWallet(sourceWallet.copy(balance = newBal))
        }

        if (type == TransactionType.TRANSFER && !tx.toWalletId.isNullOrBlank()) {
            val destWallet = dao.getWalletById(tx.toWalletId, tx.userId)
            if (destWallet != null) {
                val isDestCredit = WalletType.fromName(destWallet.type) == WalletType.CREDIT_CARD
                val destDelta = if (isDestCredit) -amount else amount
                val newDestBal = FinanceMath.roundCurrency(destWallet.balance + (destDelta * sign))
                dao.upsertWallet(destWallet.copy(balance = newDestBal))
            }
        }
    }

    // --- Wallets (With Referential Integrity) ---

    suspend fun upsertWallet(wallet: Wallet) {
        dao.upsertWallet(
            WalletEntity(
                id = wallet.id.ifBlank { UUID.randomUUID().toString() },
                userId = wallet.userId,
                name = wallet.name.trim(),
                type = wallet.type.name,
                balance = FinanceMath.roundCurrency(wallet.balance),
                creditLimit = FinanceMath.roundCurrency(wallet.creditLimit.coerceAtLeast(0.0)),
                colorHex = wallet.colorHex
            )
        )
    }

    suspend fun deleteWalletSafely(userId: String, walletId: String, fallbackWalletId: String): Result<Unit> {
        if (walletId == fallbackWalletId) {
            return Result.failure(IllegalArgumentException("Select a different fallback wallet."))
        }
        val wallets = dao.getWallets(userId)
        if (wallets.size <= 1) {
            return Result.failure(IllegalStateException("You must keep at least one active wallet."))
        }
        db.withTransaction {
            dao.reassignTransactionSourceWallet(walletId, fallbackWalletId, userId)
            dao.reassignTransactionDestWallet(walletId, fallbackWalletId, userId)
            dao.reassignRecurringWallet(walletId, fallbackWalletId, userId)
            dao.deleteWallet(walletId, userId)
        }
        return Result.success(Unit)
    }

    // --- Categories & Budgets (With Referential Integrity) ---

    suspend fun upsertCategory(category: Category) {
        dao.upsertCategory(
            CategoryEntity(
                id = category.id.ifBlank { UUID.randomUUID().toString() },
                userId = category.userId,
                name = category.name.trim(),
                type = category.type.name,
                monthlyLimit = FinanceMath.roundCurrency(category.monthlyLimit.coerceAtLeast(0.0)),
                colorHex = category.colorHex,
                iconKey = category.iconKey
            )
        )
    }

    suspend fun updateCategoryBudget(userId: String, categoryId: String, monthlyLimit: Double) {
        val cats = dao.getCategories(userId)
        val target = cats.firstOrNull { it.id == categoryId } ?: return
        dao.upsertCategory(target.copy(monthlyLimit = FinanceMath.roundCurrency(monthlyLimit.coerceAtLeast(0.0))))
    }

    suspend fun deleteCategorySafely(userId: String, categoryId: String, fallbackCategoryId: String?) {
        db.withTransaction {
            dao.reassignTransactionCategory(categoryId, fallbackCategoryId, userId)
            dao.reassignRecurringCategory(categoryId, fallbackCategoryId, userId)
            dao.deleteCategory(categoryId, userId)
        }
    }

    // --- Savings Goals ---

    suspend fun upsertGoal(goal: SavingsGoal) {
        dao.upsertGoal(
            SavingsGoalEntity(
                id = goal.id.ifBlank { UUID.randomUUID().toString() },
                userId = goal.userId,
                name = goal.name.trim(),
                targetAmount = FinanceMath.roundCurrency(goal.targetAmount.coerceAtLeast(1.0)),
                savedAmount = FinanceMath.roundCurrency(goal.savedAmount.coerceAtLeast(0.0)),
                deadline = goal.deadline,
                colorHex = goal.colorHex
            )
        )
    }

    suspend fun contributeToGoal(userId: String, goalId: String, walletId: String, amount: Double) {
        val cleanAmt = FinanceMath.roundCurrency(amount.coerceAtLeast(0.0))
        if (cleanAmt <= 0.0) return

        db.withTransaction {
            val goal = dao.getGoalById(goalId, userId) ?: return@withTransaction
            val wallet = dao.getWalletById(walletId, userId) ?: return@withTransaction
            val isCredit = WalletType.fromName(wallet.type) == WalletType.CREDIT_CARD
            val newWalletBalance = FinanceMath.roundCurrency(
                if (isCredit) wallet.balance + cleanAmt else wallet.balance - cleanAmt
            )
            dao.upsertWallet(wallet.copy(balance = newWalletBalance))
            dao.upsertGoal(goal.copy(savedAmount = FinanceMath.roundCurrency(goal.savedAmount + cleanAmt)))

            // Also log a clear transfer/vault contribution record so the ledger stays auditable
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = TransactionType.TRANSFER.name,
                amount = cleanAmt,
                title = "Vault Contribution: ${goal.name}",
                categoryId = null,
                walletId = walletId,
                toWalletId = null,
                date = WalXDateUtils.todayIso(),
                notes = "Saved into ${goal.name}",
                tagsCsv = "vault,savings",
                createdAt = System.currentTimeMillis()
            )
            dao.upsertTransaction(tx)
        }
    }

    suspend fun deleteGoal(userId: String, goalId: String) {
        dao.deleteGoal(goalId, userId)
    }

    // --- Recurring Bills ---

    suspend fun upsertRecurringBill(bill: RecurringBill) {
        dao.upsertRecurringBill(
            RecurringBillEntity(
                id = bill.id.ifBlank { UUID.randomUUID().toString() },
                userId = bill.userId,
                title = bill.title.trim(),
                amount = FinanceMath.roundCurrency(bill.amount.coerceAtLeast(0.0)),
                categoryId = bill.categoryId,
                walletId = bill.walletId,
                frequency = bill.frequency.name,
                nextDueDate = bill.nextDueDate,
                notes = bill.notes.trim()
            )
        )
    }

    suspend fun payRecurringBill(userId: String, billId: String) {
        db.withTransaction {
            val bill = dao.getRecurringBillById(billId, userId) ?: return@withTransaction
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = TransactionType.EXPENSE.name,
                amount = FinanceMath.roundCurrency(bill.amount),
                title = bill.title,
                categoryId = bill.categoryId,
                walletId = bill.walletId,
                toWalletId = null,
                date = WalXDateUtils.todayIso(),
                notes = bill.notes.ifBlank { "Recurring ${bill.frequency.lowercase()} payment" },
                tagsCsv = "recurring,bill",
                createdAt = System.currentTimeMillis()
            )
            dao.upsertTransaction(tx)
            applyWalletEffect(tx, reverse = false)

            val freq = RecurringFrequency.fromName(bill.frequency)
            val nextDate = WalXDateUtils.advanceDate(bill.nextDueDate, freq)
            dao.upsertRecurringBill(bill.copy(nextDueDate = nextDate))
        }
    }

    suspend fun deleteRecurringBill(userId: String, billId: String) {
        dao.deleteRecurringBill(billId, userId)
    }

    // --- Data Management (Clear, Export/Import) ---

    suspend fun clearUserData(userId: String) {
        db.withTransaction {
            dao.deleteAllTransactionsForUser(userId)
            dao.deleteAllRecurringBillsForUser(userId)
            dao.deleteAllGoalsForUser(userId)
            dao.deleteAllWalletsForUser(userId)
            dao.deleteAllCategoriesForUser(userId)

            // Always restore default categories and 1 starter wallet so user can log immediately
            dao.upsertCategories(DefaultAccountTemplates.createDefaultCategories(userId))
            dao.upsertWallet(DefaultAccountTemplates.createStarterWallet(userId))
        }
    }

    suspend fun getSnapshotForExport(userId: String): UserExportSnapshot? {
        val user = dao.getUserById(userId)?.toDomain() ?: return null
        return UserExportSnapshot(
            user = user,
            wallets = dao.getWallets(userId).map { it.toDomain() },
            categories = dao.getCategories(userId).map { it.toDomain() },
            transactions = dao.getTransactions(userId).map { it.toDomain() },
            goals = dao.getGoals(userId).map { it.toDomain() },
            recurringBills = dao.getRecurringBills(userId).map { it.toDomain() }
        )
    }

    suspend fun restoreFromBackup(userId: String, backup: ParsedBackupPayload) {
        db.withTransaction {
            dao.deleteAllTransactionsForUser(userId)
            dao.deleteAllRecurringBillsForUser(userId)
            dao.deleteAllGoalsForUser(userId)
            dao.deleteAllWalletsForUser(userId)
            dao.deleteAllCategoriesForUser(userId)

            val wallets = backup.wallets.ifEmpty {
                listOf(DefaultAccountTemplates.createStarterWallet(userId).toDomain())
            }
            val categories = backup.categories.ifEmpty {
                DefaultAccountTemplates.createDefaultCategories(userId).map { it.toDomain() }
            }

            dao.upsertWallets(wallets.map {
                WalletEntity(it.id, userId, it.name, it.type.name, FinanceMath.roundCurrency(it.balance), FinanceMath.roundCurrency(it.creditLimit), it.colorHex)
            })
            dao.upsertCategories(categories.map {
                CategoryEntity(it.id, userId, it.name, it.type.name, FinanceMath.roundCurrency(it.monthlyLimit), it.colorHex, it.iconKey)
            })
            dao.upsertTransactions(backup.transactions.map {
                TransactionEntity(it.id, userId, it.type.name, FinanceMath.roundCurrency(it.amount), it.title, it.categoryId, it.walletId, it.toWalletId, it.date, it.notes, it.tags.joinToString(","), it.createdAt)
            })
            dao.upsertGoals(backup.goals.map {
                SavingsGoalEntity(it.id, userId, it.name, FinanceMath.roundCurrency(it.targetAmount), FinanceMath.roundCurrency(it.savedAmount), it.deadline, it.colorHex)
            })
            dao.upsertRecurringBills(backup.recurringBills.map {
                RecurringBillEntity(it.id, userId, it.title, FinanceMath.roundCurrency(it.amount), it.categoryId, it.walletId, it.frequency.name, it.nextDueDate, it.notes)
            })

            dao.getUserById(userId)?.let { u ->
                dao.updateUser(
                    u.copy(
                        currencyCode = backup.currency.code,
                        monthlyBudget = FinanceMath.roundCurrency(backup.monthlyBudget.coerceAtLeast(0.0))
                    )
                )
            }
        }
    }

    private fun isValidEmail(email: String): Boolean =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email)
}

data class UserExportSnapshot(
    val user: UserProfile,
    val wallets: List<Wallet>,
    val categories: List<Category>,
    val transactions: List<FinanceTransaction>,
    val goals: List<SavingsGoal>,
    val recurringBills: List<RecurringBill>
)

data class ParsedBackupPayload(
    val currency: CurrencyCode,
    val monthlyBudget: Double,
    val wallets: List<Wallet>,
    val categories: List<Category>,
    val transactions: List<FinanceTransaction>,
    val goals: List<SavingsGoal>,
    val recurringBills: List<RecurringBill>
)
