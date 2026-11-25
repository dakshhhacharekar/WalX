package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.WalXAiCopilotService
import com.example.auth.AuthSessionEvent
import com.example.auth.AuthenticationProvider
import com.example.data.AuthResult
import com.example.data.BackupExportManager
import com.example.data.SessionManager
import com.example.data.UserExportSnapshot
import com.example.data.WalXDatabase
import com.example.data.WalXRepository
import com.example.domain.CashflowPoint
import com.example.domain.CashflowRange
import com.example.domain.Category
import com.example.domain.CategorySpendSlice
import com.example.domain.CurrencyCode
import com.example.domain.DashboardMetrics
import com.example.domain.DateRangeFilter
import com.example.domain.FinanceMath
import com.example.domain.FinanceTransaction
import com.example.domain.LedgerSortOption
import com.example.domain.ParsedAiTransaction
import com.example.domain.RecurringBill
import com.example.domain.RecurringFrequency
import com.example.domain.SavingsGoal
import com.example.domain.TransactionType
import com.example.domain.UserProfile
import com.example.domain.WalXDateUtils
import com.example.domain.Wallet
import com.example.domain.WalletType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WalXTab(val label: String) {
    DASHBOARD("Dashboard"),
    LEDGER("Ledger"),
    WALLETS("Wallets"),
    PLANNING("Plan"),
    SETTINGS("Settings")
}

sealed interface AuthUiState {
    data object Checking : AuthUiState
    data object Unauthenticated : AuthUiState
    data class Authenticated(val user: UserProfile) : AuthUiState
}

sealed interface AiInsightsState {
    data object Idle : AiInsightsState
    data object Loading : AiInsightsState
    data class Streaming(val text: String) : AiInsightsState
    data class Success(val text: String) : AiInsightsState
    data class Error(val message: String) : AiInsightsState
}

sealed interface AiQuickLogState {
    data object Idle : AiQuickLogState
    data object Loading : AiQuickLogState
    data class Error(val message: String) : AiQuickLogState
}

data class TransactionDraft(
    val editingId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountExpr: String = "",
    val title: String = "",
    val categoryId: String? = null,
    val walletId: String = "",
    val toWalletId: String? = null,
    val date: String = WalXDateUtils.todayIso(),
    val notes: String = "",
    val tagsText: String = ""
)

data class WalXUiState(
    val authState: AuthUiState = AuthUiState.Checking,
    val activeTab: WalXTab = WalXTab.DASHBOARD,
    val darkMode: Boolean = true,
    val hasRegisteredAccounts: Boolean = false,
    val authError: String? = null,
    val authBusy: Boolean = false,
    val securityError: String? = null,
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<FinanceTransaction> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val recurringBills: List<RecurringBill> = emptyList(),
    val metrics: DashboardMetrics = DashboardMetrics(),
    val cashflowRange: CashflowRange = CashflowRange.LAST_14_DAYS,
    // Ledger filters
    val searchQuery: String = "",
    val filterType: TransactionType? = null,
    val filterWalletId: String? = null,
    val filterCategoryId: String? = null,
    val filterDateRange: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val sortOption: LedgerSortOption = LedgerSortOption.DATE_DESC,
    // Transaction sheet
    val showTransactionSheet: Boolean = false,
    val transactionDraft: TransactionDraft = TransactionDraft(),
    // AI Copilot
    val aiQuickLogState: AiQuickLogState = AiQuickLogState.Idle,
    val aiInsightsState: AiInsightsState = AiInsightsState.Idle,
    // Toast / Snackbar
    val bannerMessage: String? = null
)

private const val VM_TAG = "WalXViewModel"

class WalXViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WalXDatabase.getInstance(application)
    private val sessionManager = SessionManager(application)
    private val repository = WalXRepository(database, sessionManager)
    private val authProvider = AuthenticationProvider(repository)
    private val aiCopilot = WalXAiCopilotService()

    private val _uiState = MutableStateFlow(WalXUiState())
    val uiState: StateFlow<WalXUiState> = _uiState.asStateFlow()

    private var userDataJob: Job? = null
    private var aiInsightsJob: Job? = null

    init {
        viewModelScope.launch {
            authProvider.isLoading.collect { loading ->
                _uiState.update { it.copy(authBusy = loading) }
            }
        }

        viewModelScope.launch {
            authProvider.authState.collect { event ->
                when (event) {
                    AuthSessionEvent.Checking -> {
                        _uiState.update { it.copy(authState = AuthUiState.Checking) }
                    }

                    is AuthSessionEvent.Authenticated -> {
                        _uiState.update { it.copy(hasRegisteredAccounts = true) }
                        startObservingUser(event.user)
                    }

                    AuthSessionEvent.Unauthenticated -> {
                        userDataJob?.cancel()
                        aiInsightsJob?.cancel()
                        val hasAccounts = repository.hasRegisteredAccounts()
                        _uiState.update { state ->
                            WalXUiState(
                                authState = AuthUiState.Unauthenticated,
                                darkMode = state.darkMode,
                                hasRegisteredAccounts = hasAccounts,
                                authBusy = authProvider.isLoading.value,
                                authError = state.authError,
                                bannerMessage = state.bannerMessage
                            )
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            val hasAccounts = authProvider.restoreSession()
            _uiState.update { it.copy(hasRegisteredAccounts = hasAccounts) }
        }
    }

    private fun startObservingUser(initialUser: UserProfile) {
        userDataJob?.cancel()
        aiInsightsJob?.cancel()
        _uiState.update {
            it.copy(
                authState = AuthUiState.Authenticated(initialUser),
                darkMode = initialUser.darkMode,
                activeTab = WalXTab.DASHBOARD,
                authError = null,
                authBusy = false,
                aiInsightsState = AiInsightsState.Idle,
                aiQuickLogState = AiQuickLogState.Idle,
                filterWalletId = null,
                filterCategoryId = null,
                filterType = null,
                searchQuery = ""
            )
        }

        userDataJob = viewModelScope.launch {
            val baseFlows = combine(
                repository.observeUser(initialUser.id),
                repository.observeWallets(initialUser.id),
                repository.observeCategories(initialUser.id),
                repository.observeTransactions(initialUser.id),
                repository.observeGoals(initialUser.id)
            ) { user, wallets, categories, transactions, goals ->
                Tuple5(user, wallets, categories, transactions, goals)
            }

            combine(
                baseFlows,
                repository.observeRecurringBills(initialUser.id)
            ) { base, recurring ->
                val activeUser = base.a ?: initialUser
                val metrics = computeDashboardMetrics(
                    user = activeUser,
                    wallets = base.b,
                    categories = base.c,
                    transactions = base.d,
                    recurringBills = recurring
                )
                Triple(activeUser, base, Pair(recurring, metrics))
            }.collect { (activeUser, base, extra) ->
                val (recurring, metrics) = extra
                _uiState.update { state ->
                    state.copy(
                        authState = AuthUiState.Authenticated(activeUser),
                        darkMode = activeUser.darkMode,
                        wallets = base.b,
                        categories = base.c,
                        transactions = base.d,
                        goals = base.e,
                        recurringBills = recurring,
                        metrics = metrics
                    )
                }
            }
        }
    }

    private fun activeUserId(): String? =
        (_uiState.value.authState as? AuthUiState.Authenticated)?.user?.id

    // --- Email & Password Authentication via AuthenticationProvider ---

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun clearSecurityError() {
        _uiState.update { it.copy(securityError = null) }
    }

    fun logIn(email: String, password: String, rememberMe: Boolean) {
        if (authProvider.isLoading.value) return
        viewModelScope.launch {
            _uiState.update { it.copy(authError = null) }
            when (val res = authProvider.logIn(email, password, rememberMe)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(hasRegisteredAccounts = true, authError = null) }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(authError = res.message) }
                }
            }
        }
    }

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        resetPin: String,
        currency: CurrencyCode,
        startingBalance: Double = 0.0
    ) {
        if (authProvider.isLoading.value) return
        viewModelScope.launch {
            _uiState.update { it.copy(authError = null) }
            when (
                val res = authProvider.signUp(
                    fullName = fullName,
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    resetPin = resetPin,
                    currency = currency,
                    startingBalance = startingBalance
                )
            ) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            hasRegisteredAccounts = true,
                            authError = null,
                            bannerMessage = "Welcome to WalX, ${res.user.fullName}!"
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(authError = res.message) }
                }
            }
        }
    }

    fun resetPasswordWithPin(
        email: String,
        resetPin: String,
        newPassword: String,
        confirmNewPassword: String,
        onSuccess: () -> Unit
    ) {
        if (authProvider.isLoading.value) return
        viewModelScope.launch {
            _uiState.update { it.copy(securityError = null) }
            when (
                val res = authProvider.resetPasswordWithPin(
                    email = email,
                    resetPin = resetPin,
                    newPassword = newPassword,
                    confirmNewPassword = confirmNewPassword
                )
            ) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(securityError = null) }
                    showBanner("Password reset for ${res.user.email}.")
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(securityError = res.message) }
                }
            }
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String,
        onSuccess: () -> Unit
    ) {
        val uid = activeUserId() ?: return
        if (authProvider.isLoading.value) return
        viewModelScope.launch {
            _uiState.update { it.copy(securityError = null) }
            when (
                val res = authProvider.changePassword(
                    userId = uid,
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmNewPassword = confirmNewPassword
                )
            ) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(securityError = null) }
                    showBanner("Password updated successfully.")
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(securityError = res.message) }
                }
            }
        }
    }

    fun updateRecoveryPin(
        currentPassword: String,
        newPin: String,
        onSuccess: () -> Unit
    ) {
        val uid = activeUserId() ?: return
        if (authProvider.isLoading.value) return
        viewModelScope.launch {
            _uiState.update { it.copy(securityError = null) }
            when (val res = authProvider.updateRecoveryPin(uid, currentPassword, newPin)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(securityError = null) }
                    showBanner("Recovery PIN updated successfully.")
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(securityError = res.message) }
                }
            }
        }
    }

    fun signOut() {
        userDataJob?.cancel()
        aiInsightsJob?.cancel()
        viewModelScope.launch {
            authProvider.signOut()
            showBanner("Signed out cleanly.")
        }
    }

    // --- Navigation & UI Helpers ---

    fun selectTab(tab: WalXTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun inspectWalletInLedger(walletId: String) {
        _uiState.update {
            it.copy(
                activeTab = WalXTab.LEDGER,
                filterWalletId = walletId,
                filterDateRange = DateRangeFilter.ALL_TIME
            )
        }
    }

    fun setCashflowRange(range: CashflowRange) {
        _uiState.update { it.copy(cashflowRange = range) }
    }

    fun updateLedgerFilters(
        search: String = _uiState.value.searchQuery,
        type: TransactionType? = _uiState.value.filterType,
        walletId: String? = _uiState.value.filterWalletId,
        categoryId: String? = _uiState.value.filterCategoryId,
        dateRange: DateRangeFilter = _uiState.value.filterDateRange,
        sort: LedgerSortOption = _uiState.value.sortOption
    ) {
        _uiState.update {
            it.copy(
                searchQuery = search,
                filterType = type,
                filterWalletId = walletId,
                filterCategoryId = categoryId,
                filterDateRange = dateRange,
                sortOption = sort
            )
        }
    }

    fun clearLedgerFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                filterType = null,
                filterWalletId = null,
                filterCategoryId = null,
                filterDateRange = DateRangeFilter.ALL_TIME,
                sortOption = LedgerSortOption.DATE_DESC
            )
        }
    }

    fun showBanner(message: String) {
        _uiState.update { it.copy(bannerMessage = message) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    // --- Transaction Modal & Atomic CRUD ---

    fun openNewTransactionModal(preselectedType: TransactionType = TransactionType.EXPENSE) {
        val state = _uiState.value
        val defaultWallet = state.wallets.firstOrNull()?.id.orEmpty()
        val defaultDest = state.wallets.getOrNull(1)?.id
        val defaultCat = state.categories.firstOrNull { it.type == preselectedType }?.id
        _uiState.update {
            it.copy(
                showTransactionSheet = true,
                transactionDraft = TransactionDraft(
                    type = preselectedType,
                    walletId = defaultWallet,
                    toWalletId = defaultDest,
                    categoryId = defaultCat,
                    date = WalXDateUtils.todayIso()
                )
            )
        }
    }

    fun openEditTransactionModal(tx: FinanceTransaction) {
        _uiState.update {
            it.copy(
                showTransactionSheet = true,
                transactionDraft = TransactionDraft(
                    editingId = tx.id,
                    type = tx.type,
                    amountExpr = FinanceMath.roundCurrency(tx.amount).toString(),
                    title = tx.title,
                    categoryId = tx.categoryId,
                    walletId = tx.walletId,
                    toWalletId = tx.toWalletId,
                    date = tx.date,
                    notes = tx.notes,
                    tagsText = tx.tags.joinToString(", ")
                )
            )
        }
    }

    fun closeTransactionModal() {
        _uiState.update { it.copy(showTransactionSheet = false) }
    }

    fun saveTransaction(
        editingId: String?,
        type: TransactionType,
        amount: Double,
        title: String,
        categoryId: String?,
        walletId: String,
        toWalletId: String?,
        date: String,
        notes: String,
        tagsText: String
    ) {
        val uid = activeUserId() ?: return
        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val tx = FinanceTransaction(
            id = editingId.orEmpty(),
            userId = uid,
            type = type,
            amount = FinanceMath.roundCurrency(amount),
            title = title.trim(),
            categoryId = if (type == TransactionType.TRANSFER) null else categoryId,
            walletId = walletId,
            toWalletId = if (type == TransactionType.TRANSFER) toWalletId else null,
            date = if (WalXDateUtils.isValidIsoDate(date)) date else WalXDateUtils.todayIso(),
            notes = notes.trim(),
            tags = tags
        )
        viewModelScope.launch {
            if (editingId.isNullOrBlank()) {
                repository.addTransaction(tx)
                showBanner("Logged ${tx.title}")
            } else {
                repository.updateTransaction(tx)
                showBanner("Updated ${tx.title}")
            }
            _uiState.update { it.copy(showTransactionSheet = false) }
        }
    }

    fun deleteTransaction(txId: String) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.deleteTransaction(uid, txId)
            showBanner("Transaction deleted and wallet balance restored.")
        }
    }

    // --- Wallets ---

    fun saveWallet(
        editingId: String?,
        name: String,
        type: WalletType,
        balance: Double,
        creditLimit: Double,
        colorHex: Long
    ) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.upsertWallet(
                Wallet(
                    id = editingId.orEmpty(),
                    userId = uid,
                    name = name,
                    type = type,
                    balance = balance,
                    creditLimit = if (type == WalletType.CREDIT_CARD) creditLimit else 0.0,
                    colorHex = colorHex
                )
            )
            showBanner(if (editingId == null) "Wallet '$name' created." else "Wallet '$name' updated.")
        }
    }

    fun deleteWalletSafely(walletId: String, fallbackWalletId: String) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.deleteWalletSafely(uid, walletId, fallbackWalletId)
                .onSuccess { showBanner("Wallet deleted. Linked activity reassigned safely.") }
                .onFailure { showBanner(it.message ?: "Unable to delete wallet.") }
        }
    }

    // --- Categories & Budgets ---

    fun addCategory(name: String, type: TransactionType, monthlyLimit: Double, colorHex: Long) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.upsertCategory(
                Category(
                    id = "",
                    userId = uid,
                    name = name,
                    type = type,
                    monthlyLimit = if (type == TransactionType.EXPENSE) monthlyLimit else 0.0,
                    colorHex = colorHex
                )
            )
            showBanner("Category '$name' added.")
        }
    }

    fun updateCategoryBudget(categoryId: String, monthlyLimit: Double) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.updateCategoryBudget(uid, categoryId, monthlyLimit)
            showBanner("Category budget updated.")
        }
    }

    fun deleteCategorySafely(categoryId: String, fallbackCategoryId: String?) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.deleteCategorySafely(uid, categoryId, fallbackCategoryId)
            showBanner("Category removed safely.")
        }
    }

    // --- Savings Goals ---

    fun saveGoal(name: String, targetAmount: Double, savedAmount: Double, deadline: String, colorHex: Long) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.upsertGoal(
                SavingsGoal(
                    id = "",
                    userId = uid,
                    name = name,
                    targetAmount = targetAmount,
                    savedAmount = savedAmount,
                    deadline = if (WalXDateUtils.isValidIsoDate(deadline)) deadline else WalXDateUtils.daysFromNowIso(90),
                    colorHex = colorHex
                )
            )
            showBanner("Savings vault '$name' created.")
        }
    }

    fun contributeToGoal(goalId: String, walletId: String, amount: Double) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.contributeToGoal(uid, goalId, walletId, amount)
            showBanner("Contribution added to vault.")
        }
    }

    fun deleteGoal(goalId: String) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.deleteGoal(uid, goalId)
            showBanner("Savings vault removed.")
        }
    }

    // --- Recurring Bills ---

    fun saveRecurringBill(
        title: String,
        amount: Double,
        categoryId: String?,
        walletId: String,
        frequency: RecurringFrequency,
        nextDueDate: String,
        notes: String
    ) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.upsertRecurringBill(
                RecurringBill(
                    id = "",
                    userId = uid,
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    walletId = walletId,
                    frequency = frequency,
                    nextDueDate = if (WalXDateUtils.isValidIsoDate(nextDueDate)) nextDueDate else WalXDateUtils.daysFromNowIso(30),
                    notes = notes
                )
            )
            showBanner("Recurring bill '$title' scheduled.")
        }
    }

    fun payRecurringBill(billId: String) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.payRecurringBill(uid, billId)
            showBanner("Bill paid, logged to ledger, and next due date advanced.")
        }
    }

    fun deleteRecurringBill(billId: String) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.deleteRecurringBill(uid, billId)
            showBanner("Recurring bill deleted.")
        }
    }

    // --- Profile, Theme, Demo & Backup ---

    fun toggleDarkMode() {
        val uid = activeUserId() ?: return
        val next = !_uiState.value.darkMode
        viewModelScope.launch {
            repository.toggleTheme(uid, next)
        }
    }

    fun updateProfileSettings(fullName: String, currency: CurrencyCode, monthlyBudget: Double, darkMode: Boolean) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.updateProfile(uid, fullName, currency, monthlyBudget, darkMode)
            showBanner("Profile & preferences saved.")
        }
    }

    fun clearAllUserData() {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            repository.clearUserData(uid)
            showBanner("Cleared financial records. Starter wallet & default categories restored.")
        }
    }

    suspend fun exportSnapshot(): UserExportSnapshot? {
        val uid = activeUserId() ?: return null
        return repository.getSnapshotForExport(uid)
    }

    fun importJsonBackup(rawJson: String) {
        val uid = activeUserId() ?: return
        viewModelScope.launch {
            BackupExportManager.parseJsonBackup(uid, rawJson)
                .onSuccess { payload ->
                    repository.restoreFromBackup(uid, payload)
                    showBanner("Backup imported: ${payload.transactions.size} transactions & ${payload.wallets.size} wallets.")
                }
                .onFailure { err ->
                    showBanner(err.message ?: "Failed to import backup JSON.")
                }
        }
    }

    // --- WalX AI Copilot (Firebase AI Logic) ---

    fun runAiQuickLog(promptText: String) {
        val cleaned = promptText.trim()
        if (cleaned.isEmpty()) return
        val state = _uiState.value
        val user = (state.authState as? AuthUiState.Authenticated)?.user ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(aiQuickLogState = AiQuickLogState.Loading) }
            val result = aiCopilot.parseNaturalLanguageTransaction(
                userText = cleaned,
                categories = state.categories,
                wallets = state.wallets,
                currency = user.currency
            )
            result.onSuccess { parsed ->
                openPreFilledFromAi(parsed)
                _uiState.update { it.copy(aiQuickLogState = AiQuickLogState.Idle) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(aiQuickLogState = AiQuickLogState.Error(err.message ?: "AI Quick-Log failed."))
                }
            }
        }
    }

    private fun openPreFilledFromAi(parsed: ParsedAiTransaction) {
        val state = _uiState.value
        val matchedWallet = state.wallets.firstOrNull {
            it.name.contains(parsed.walletName, ignoreCase = true) ||
                parsed.walletName.contains(it.name, ignoreCase = true)
        } ?: state.wallets.firstOrNull()

        val matchedCategory = state.categories.firstOrNull {
            it.type == parsed.type && (
                it.name.contains(parsed.categoryName, ignoreCase = true) ||
                    parsed.categoryName.contains(it.name, ignoreCase = true)
                )
        } ?: state.categories.firstOrNull { it.type == parsed.type }

        val destWallet = if (parsed.type == TransactionType.TRANSFER) {
            state.wallets.firstOrNull { it.id != matchedWallet?.id }
        } else null

        _uiState.update {
            it.copy(
                showTransactionSheet = true,
                transactionDraft = TransactionDraft(
                    editingId = null,
                    type = parsed.type,
                    amountExpr = parsed.amount.toString(),
                    title = parsed.title,
                    categoryId = matchedCategory?.id,
                    walletId = matchedWallet?.id.orEmpty(),
                    toWalletId = destWallet?.id,
                    date = parsed.date,
                    notes = parsed.notes,
                    tagsText = "ai-quicklog"
                ),
                bannerMessage = "AI parsed '${parsed.title}' — review & confirm."
            )
        }
    }

    fun generateSpendingInsights() {
        val state = _uiState.value
        val user = (state.authState as? AuthUiState.Authenticated)?.user ?: return

        aiInsightsJob?.cancel()
        aiInsightsJob = viewModelScope.launch {
            _uiState.update { it.copy(aiInsightsState = AiInsightsState.Loading) }
            var finalText = ""
            try {
                aiCopilot.streamSpendingInsights(
                    metrics = state.metrics,
                    currency = user.currency,
                    wallets = state.wallets
                ).collect { accumulated ->
                    finalText = accumulated
                    _uiState.update { it.copy(aiInsightsState = AiInsightsState.Streaming(accumulated)) }
                }
                _uiState.update {
                    it.copy(
                        aiInsightsState = if (finalText.isNotBlank()) {
                            AiInsightsState.Success(finalText)
                        } else {
                            AiInsightsState.Error("No insights generated. Please try again.")
                        }
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(VM_TAG, "AI Insights timed out", e)
                _uiState.update { it.copy(aiInsightsState = AiInsightsState.Error("Insights request timed out. Try again.")) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "AI Insights failed", e)
                _uiState.update {
                    it.copy(aiInsightsState = AiInsightsState.Error(e.localizedMessage ?: "Unable to fetch AI insights."))
                }
            }
        }
    }

    // --- Filtered Ledger Selector ---

    fun getFilteredTransactions(state: WalXUiState = _uiState.value): List<FinanceTransaction> {
        val curMonth = WalXDateUtils.currentYearMonth()
        val prevMonth = WalXDateUtils.previousYearMonth()
        val cutoff90 = WalXDateUtils.daysAgoIso(90)
        val query = state.searchQuery.trim().lowercase()

        val filtered = state.transactions.filter { tx ->
            val matchesQuery = if (query.isEmpty()) true else {
                tx.title.lowercase().contains(query) ||
                    tx.notes.lowercase().contains(query) ||
                    tx.tags.any { it.lowercase().contains(query) }
            }
            val matchesType = state.filterType == null || tx.type == state.filterType
            val matchesWallet = state.filterWalletId == null ||
                tx.walletId == state.filterWalletId ||
                tx.toWalletId == state.filterWalletId
            val matchesCategory = state.filterCategoryId == null || tx.categoryId == state.filterCategoryId
            val matchesDate = when (state.filterDateRange) {
                DateRangeFilter.THIS_MONTH -> tx.date.startsWith(curMonth)
                DateRangeFilter.LAST_MONTH -> tx.date.startsWith(prevMonth)
                DateRangeFilter.LAST_90_DAYS -> tx.date >= cutoff90
                DateRangeFilter.ALL_TIME -> true
            }
            matchesQuery && matchesType && matchesWallet && matchesCategory && matchesDate
        }

        return when (state.sortOption) {
            LedgerSortOption.DATE_DESC -> filtered.sortedWith(compareByDescending<FinanceTransaction> { it.date }.thenByDescending { it.createdAt })
            LedgerSortOption.DATE_ASC -> filtered.sortedWith(compareBy<FinanceTransaction> { it.date }.thenBy { it.createdAt })
            LedgerSortOption.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            LedgerSortOption.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }
    }

    // --- Dashboard Analytics Computation ---

    private fun computeDashboardMetrics(
        user: UserProfile,
        wallets: List<Wallet>,
        categories: List<Category>,
        transactions: List<FinanceTransaction>,
        recurringBills: List<RecurringBill>
    ): DashboardMetrics {
        val liquid = FinanceMath.roundCurrency(
            wallets.filter { it.type != WalletType.CREDIT_CARD }.sumOf { it.balance }
        )
        val creditDebt = FinanceMath.roundCurrency(
            wallets.filter { it.type == WalletType.CREDIT_CARD }.sumOf { it.balance }
        )
        val netWorth = FinanceMath.roundCurrency(liquid - creditDebt)

        val curMonth = WalXDateUtils.currentYearMonth()
        val prevMonth = WalXDateUtils.previousYearMonth()

        val curMonthTxs = transactions.filter { it.date.startsWith(curMonth) }
        val prevMonthTxs = transactions.filter { it.date.startsWith(prevMonth) }

        val monthIncome = FinanceMath.roundCurrency(
            curMonthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        )
        val monthExpense = FinanceMath.roundCurrency(
            curMonthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        )
        val prevIncome = FinanceMath.roundCurrency(
            prevMonthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        )
        val prevExpense = FinanceMath.roundCurrency(
            prevMonthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        )

        val savingsRate = if (monthIncome > 0.0) {
            FinanceMath.safePercent(monthIncome - monthExpense, monthIncome)
        } else 0.0
        val prevSavingsRate = if (prevIncome > 0.0) {
            FinanceMath.safePercent(prevIncome - prevExpense, prevIncome)
        } else 0.0

        val budgetLimit = FinanceMath.roundCurrency(user.monthlyBudget)
        val budgetUsedPct = FinanceMath.safePercent(monthExpense, budgetLimit)
        val budgetRemaining = FinanceMath.roundCurrency(budgetLimit - monthExpense)
        val daysLeft = WalXDateUtils.daysRemainingInCurrentMonth()
        val dailySafe = if (budgetRemaining > 0.0) {
            FinanceMath.roundCurrency(budgetRemaining / daysLeft.toDouble())
        } else 0.0

        // 14-day series
        val days14 = WalXDateUtils.lastNDaysIso(14)
        val cashflow14 = days14.map { iso ->
            val dayTxs = transactions.filter { it.date == iso }
            CashflowPoint(
                label = iso.takeLast(2),
                income = FinanceMath.roundCurrency(dayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }),
                expense = FinanceMath.roundCurrency(dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
            )
        }

        // 6-month series
        val months6 = WalXDateUtils.lastNYearMonths(6)
        val cashflow6 = months6.map { ym ->
            val mTxs = transactions.filter { it.date.startsWith(ym) }
            CashflowPoint(
                label = WalXDateUtils.formatMonthLabel(ym),
                income = FinanceMath.roundCurrency(mTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }),
                expense = FinanceMath.roundCurrency(mTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
            )
        }

        // Category breakdown (current month expenses)
        val catMap = categories.associateBy { it.id }
        val grouped = curMonthTxs
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId ?: "uncategorized" }
            .map { (catId, list) ->
                val cat = catMap[catId]
                val total = FinanceMath.roundCurrency(list.sumOf { it.amount })
                CategorySpendSlice(
                    categoryId = catId,
                    categoryName = cat?.name ?: "Uncategorized",
                    colorHex = cat?.colorHex ?: 0xFF64748BL,
                    amount = total,
                    percentage = FinanceMath.safePercent(total, monthExpense)
                )
            }
            .sortedByDescending { it.amount }

        val burnRate = FinanceMath.roundCurrency(
            recurringBills.sumOf { it.amount * it.frequency.monthlyMultiplier }
        )

        return DashboardMetrics(
            totalNetWorth = netWorth,
            totalLiquidAssets = liquid,
            totalCreditDebt = creditDebt,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            prevMonthIncome = prevIncome,
            prevMonthExpense = prevExpense,
            savingsRatePercent = savingsRate,
            prevSavingsRatePercent = prevSavingsRate,
            incomeMomPercent = FinanceMath.safeMomChange(monthIncome, prevIncome),
            expenseMomPercent = FinanceMath.safeMomChange(monthExpense, prevExpense),
            budgetLimit = budgetLimit,
            budgetUsedPercent = budgetUsedPct,
            budgetRemaining = budgetRemaining,
            dailySafeSpend = dailySafe,
            daysLeftInMonth = daysLeft,
            cashflow14Days = cashflow14,
            cashflow6Months = cashflow6,
            categoryBreakdown = grouped,
            monthlyFixedBurnRate = burnRate
        )
    }

    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
