package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BackupExportManager
import com.example.domain.WalXDateUtils
import com.example.ui.components.TransactionModalSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.PlanningScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WalletsScreen
import com.example.ui.theme.LocalWalXColors
import com.example.ui.theme.WalXTheme
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.WalXTab
import com.example.ui.viewmodel.WalXViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WalXApp(viewModel: WalXViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.bannerMessage) {
        state.bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    // Zero-permission Android SAF launchers for CSV/JSON export and JSON import
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val snap = viewModel.exportSnapshot() ?: return@launch
                val csv = BackupExportManager.buildCsv(snap)
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                }
                viewModel.showBanner(if (ok) "CSV ledger exported successfully." else "Failed to write CSV file.")
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val snap = viewModel.exportSnapshot() ?: return@launch
                val json = BackupExportManager.buildJsonBackup(snap)
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                }
                viewModel.showBanner(if (ok) "JSON backup exported successfully." else "Failed to write JSON backup.")
            }
        }
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val raw = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    }.getOrNull()
                }
                if (raw != null) {
                    viewModel.importJsonBackup(raw)
                } else {
                    viewModel.showBanner("Could not read selected backup file.")
                }
            }
        }
    }

    WalXTheme(darkTheme = state.darkMode) {
        val extended = LocalWalXColors.current
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = extended.canvas
        ) {
            when (val auth = state.authState) {
                AuthUiState.Checking -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = extended.accent)
                    }
                }

                AuthUiState.Unauthenticated -> {
                    Scaffold(
                        containerColor = extended.canvas,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            AuthScreen(
                                authError = state.authError,
                                authBusy = state.authBusy,
                                onClearError = viewModel::clearAuthError,
                                onAttemptAutoSignIn = { viewModel.attemptAutoSignInIfEligible(context) },
                                onContinueWithGoogle = { name, currency, openingBalance ->
                                    viewModel.signInWithGoogle(
                                        activityContext = context,
                                        preferredDisplayName = name,
                                        startingCurrency = currency,
                                        startingBalance = openingBalance
                                    )
                                }
                            )
                        }
                    }
                }

                is AuthUiState.Authenticated -> {
                    val user = auth.user

                    key(user.id) {
                        // Return to Dashboard on Back press from secondary tabs
                        BackHandler(enabled = state.activeTab != WalXTab.DASHBOARD) {
                            viewModel.selectTab(WalXTab.DASHBOARD)
                        }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useRail = maxWidth >= 700.dp

                        Scaffold(
                            containerColor = extended.canvas,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            contentWindowInsets = WindowInsets.safeDrawing,
                            floatingActionButton = {
                                if (state.activeTab != WalXTab.SETTINGS) {
                                    ExtendedFloatingActionButton(
                                        onClick = { viewModel.openNewTransactionModal() },
                                        containerColor = extended.accent,
                                        contentColor = Color.White,
                                        icon = { Icon(Icons.Rounded.Add, contentDescription = "Log Transaction") },
                                        text = { Text("Log Transaction") },
                                        modifier = Modifier.testTag("fab_log_transaction")
                                    )
                                }
                            },
                            bottomBar = {
                                if (!useRail) {
                                    NavigationBar(
                                        containerColor = extended.card,
                                        windowInsets = WindowInsets.navigationBars
                                    ) {
                                        WalXTab.entries.forEach { tab ->
                                            val selected = state.activeTab == tab
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { viewModel.selectTab(tab) },
                                                icon = {
                                                    Icon(
                                                        imageVector = tabIcon(tab, selected),
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (useRail) {
                                    NavigationRail(
                                        containerColor = extended.card,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        WalXTab.entries.forEach { tab ->
                                            val selected = state.activeTab == tab
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = { viewModel.selectTab(tab) },
                                                icon = {
                                                    Icon(
                                                        imageVector = tabIcon(tab, selected),
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = { Text(tab.label) },
                                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    when (state.activeTab) {
                                        WalXTab.DASHBOARD -> DashboardScreen(
                                            user = user,
                                            state = state,
                                            onToggleTheme = viewModel::toggleDarkMode,
                                            onSignOut = { viewModel.signOut(context) },
                                            onQuickAddTransaction = viewModel::openNewTransactionModal,
                                            onInspectWallet = viewModel::inspectWalletInLedger,
                                            onSelectCashflowRange = viewModel::setCashflowRange,
                                            onRunAiQuickLog = viewModel::runAiQuickLog,
                                            onGenerateInsights = viewModel::generateSpendingInsights,
                                            onViewAllLedger = { viewModel.selectTab(WalXTab.LEDGER) },
                                            onEditTransaction = viewModel::openEditTransactionModal,
                                            onDeleteTransaction = viewModel::deleteTransaction
                                        )

                                        WalXTab.LEDGER -> {
                                            val filtered = remember(
                                                state.transactions,
                                                state.searchQuery,
                                                state.filterType,
                                                state.filterWalletId,
                                                state.filterCategoryId,
                                                state.filterDateRange,
                                                state.sortOption
                                            ) {
                                                viewModel.getFilteredTransactions(state)
                                            }
                                            LedgerScreen(
                                                state = state,
                                                filteredTransactions = filtered,
                                                currency = user.currency,
                                                onUpdateFilters = viewModel::updateLedgerFilters,
                                                onClearFilters = viewModel::clearLedgerFilters,
                                                onEditTransaction = viewModel::openEditTransactionModal,
                                                onDeleteTransaction = viewModel::deleteTransaction
                                            )
                                        }

                                        WalXTab.WALLETS -> WalletsScreen(
                                            wallets = state.wallets,
                                            transactions = state.transactions,
                                            metrics = state.metrics,
                                            currency = user.currency,
                                            onSaveWallet = viewModel::saveWallet,
                                            onDeleteWalletSafely = viewModel::deleteWalletSafely,
                                            onInspectWalletActivity = viewModel::inspectWalletInLedger
                                        )

                                        WalXTab.PLANNING -> PlanningScreen(
                                            state = state,
                                            currency = user.currency,
                                            onAddCategory = viewModel::addCategory,
                                            onUpdateCategoryBudget = viewModel::updateCategoryBudget,
                                            onDeleteCategorySafely = viewModel::deleteCategorySafely,
                                            onSaveGoal = viewModel::saveGoal,
                                            onContributeGoal = viewModel::contributeToGoal,
                                            onDeleteGoal = viewModel::deleteGoal,
                                            onSaveRecurring = viewModel::saveRecurringBill,
                                            onPayRecurring = viewModel::payRecurringBill,
                                            onDeleteRecurring = viewModel::deleteRecurringBill
                                        )

                                        WalXTab.SETTINGS -> SettingsScreen(
                                            user = user,
                                            onSaveProfile = viewModel::updateProfileSettings,
                                            onExportCsv = {
                                                csvExportLauncher.launch("WalX_Ledger_${WalXDateUtils.todayIso()}.csv")
                                            },
                                            onExportJson = {
                                                jsonExportLauncher.launch("WalX_Backup_${WalXDateUtils.todayIso()}.json")
                                            },
                                            onImportJson = {
                                                jsonImportLauncher.launch(arrayOf("application/json", "text/plain"))
                                            },
                                            onPrintStatement = {
                                                scope.launch {
                                                    val snap = viewModel.exportSnapshot() ?: return@launch
                                                    BackupExportManager.printFinancialStatement(context, snap, state.metrics)
                                                }
                                            },
                                            onClearAllData = viewModel::clearAllUserData,
                                            onSignOut = { viewModel.signOut(context) }
                                        )
                                    }
                                }
                            }
                        }

                        if (state.showTransactionSheet) {
                            TransactionModalSheet(
                                draft = state.transactionDraft,
                                wallets = state.wallets,
                                categories = state.categories,
                                currency = user.currency,
                                onDismiss = viewModel::closeTransactionModal,
                                onSave = viewModel::saveTransaction
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: WalXTab, selected: Boolean): ImageVector = when (tab) {
    WalXTab.DASHBOARD -> if (selected) Icons.Rounded.Dashboard else Icons.Outlined.Dashboard
    WalXTab.LEDGER -> if (selected) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong
    WalXTab.WALLETS -> if (selected) Icons.Rounded.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
    WalXTab.PLANNING -> if (selected) Icons.Rounded.Savings else Icons.Outlined.Savings
    WalXTab.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
}
