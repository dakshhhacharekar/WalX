package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CashflowRange
import com.example.domain.CurrencyCode
import com.example.domain.DashboardMetrics
import com.example.domain.FinanceMath
import com.example.domain.FinanceTransaction
import com.example.domain.TransactionType
import com.example.domain.UserProfile
import com.example.domain.Wallet
import com.example.domain.WalletType
import com.example.ui.components.CashflowTrendCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpenseBreakdownCard
import com.example.ui.components.MonthlyBudgetCard
import com.example.ui.components.StatusPill
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.WalXBrandLockup
import com.example.ui.components.WalXCard
import com.example.ui.components.walletTypeIcon
import com.example.ui.theme.LocalWalXColors
import com.example.ui.theme.TabularAmountLarge
import com.example.ui.theme.TabularAmountMedium
import com.example.ui.theme.TabularAmountSmall
import com.example.ui.viewmodel.AiInsightsState
import com.example.ui.viewmodel.AiQuickLogState
import com.example.ui.viewmodel.WalXUiState

@Composable
fun DashboardScreen(
    user: UserProfile,
    state: WalXUiState,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit,
    onQuickAddTransaction: (TransactionType) -> Unit,
    onInspectWallet: (String) -> Unit,
    onSelectCashflowRange: (CashflowRange) -> Unit,
    onRunAiQuickLog: (String) -> Unit,
    onGenerateInsights: () -> Unit,
    onViewAllLedger: () -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    val extended = LocalWalXColors.current
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val walletMap = remember(state.wallets) { state.wallets.associateBy { it.id } }
    val recentSix = remember(state.transactions) { state.transactions.take(6) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 780.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Command Header: WalX Brand + Theme + Sign Out
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WalXBrandLockup(compact = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.testTag("dashboard_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (state.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Toggle Dark or Light Mode",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("dashboard_signout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "Sign Out",
                                tint = extended.textMuted
                            )
                        }
                    }
                }
            }

            // 2. Sculpted Obsidian Hero Financial Overview Banner (Net Worth + Quick Actions)
            item {
                ObsidianNetWorthHeroCard(
                    user = user,
                    metrics = state.metrics,
                    darkMode = state.darkMode,
                    onQuickAddTransaction = onQuickAddTransaction
                )
            }

            // 3. Core 2x2 Obsidian KPI Cards Grid (Income, Expenses, Savings Rate, Fixed Burn)
            item {
                FinancialKpiGrid(
                    metrics = state.metrics,
                    currency = user.currency
                )
            }

            // 4. Connected Wallets Pulse Strip
            if (state.wallets.isNotEmpty()) {
                item {
                    WalletPulseStrip(
                        wallets = state.wallets,
                        currency = user.currency,
                        onInspectWallet = onInspectWallet
                    )
                }
            }

            // 5. Monthly Overall Budget Pacing Card
            item {
                MonthlyBudgetCard(
                    metrics = state.metrics,
                    currency = user.currency
                )
            }

            // 6. WalX AI Copilot Card (Gemini Natural Language Quick-Log & Smart Insights)
            item {
                WalXAiCopilotCard(
                    quickLogState = state.aiQuickLogState,
                    insightsState = state.aiInsightsState,
                    onRunQuickLog = onRunAiQuickLog,
                    onGenerateInsights = onGenerateInsights
                )
            }

            // 7. Interactive Cashflow Trend Chart (14D / 6M)
            item {
                CashflowTrendCard(
                    points = if (state.cashflowRange == CashflowRange.LAST_14_DAYS) {
                        state.metrics.cashflow14Days
                    } else {
                        state.metrics.cashflow6Months
                    },
                    selectedRange = state.cashflowRange,
                    currency = user.currency,
                    onSelectRange = onSelectCashflowRange
                )
            }

            // 8. Interactive Expense Breakdown Donut Chart
            item {
                ExpenseBreakdownCard(
                    slices = state.metrics.categoryBreakdown,
                    totalExpense = state.metrics.monthExpense,
                    currency = user.currency
                )
            }

            // 9. Recent Transactions Feed (Latest 6)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = onViewAllLedger,
                        modifier = Modifier.testTag("dashboard_view_all_ledger")
                    ) {
                        Text("Full Ledger", color = extended.accent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (recentSix.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "No transactions recorded yet",
                        subtitle = "Log your first expense, income, or transfer to populate your live charts and KPIs.",
                        action = {
                            Button(
                                onClick = { onQuickAddTransaction(TransactionType.EXPENSE) },
                                colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("empty_log_first_tx_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log First Transaction", color = Color.White)
                            }
                        }
                    )
                }
            } else {
                items(recentSix, key = { it.id }) { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        category = tx.categoryId?.let { catMap[it] },
                        wallet = walletMap[tx.walletId],
                        toWallet = tx.toWalletId?.let { walletMap[it] },
                        currency = user.currency,
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { onDeleteTransaction(tx.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ObsidianNetWorthHeroCard(
    user: UserProfile,
    metrics: DashboardMetrics,
    darkMode: Boolean,
    onQuickAddTransaction: (TransactionType) -> Unit
) {
    val extended = LocalWalXColors.current
    val heroBrush = if (darkMode) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF161B2E),
                Color(0xFF12151E),
                Color(0xFF0E1420)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFEEF2FF),
                Color(0xFFFFFFFF),
                Color(0xFFF0FDF4)
            )
        )
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, extended.accent.copy(alpha = 0.42f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_net_worth_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroBrush)
                .padding(18.dp)
        ) {
            // Active account row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(extended.accent, extended.income))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.initials,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                StatusPill(
                    text = user.currency.label,
                    containerColor = extended.accentContainer,
                    contentColor = extended.accent
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "TOTAL NET WORTH",
                style = MaterialTheme.typography.labelMedium,
                color = extended.textMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = FinanceMath.formatCurrency(metrics.totalNetWorth, user.currency),
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = TabularAmountLarge.fontFamily),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Liquid vs Credit Debt split bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = extended.incomeContainer.copy(alpha = 0.65f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("Liquid Assets", style = MaterialTheme.typography.labelSmall, color = extended.income)
                        Text(
                            text = FinanceMath.formatCurrency(metrics.totalLiquidAssets, user.currency),
                            style = TabularAmountSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (metrics.totalCreditDebt > 0.0) extended.expenseContainer.copy(alpha = 0.65f) else extended.cardElevated,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "Credit Liabilities",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (metrics.totalCreditDebt > 0.0) extended.expense else extended.textMuted
                        )
                        Text(
                            text = FinanceMath.formatCurrency(metrics.totalCreditDebt, user.currency),
                            style = TabularAmountSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1-Tap Quick Actions Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionPill(
                    label = "Expense",
                    icon = Icons.Rounded.ArrowUpward,
                    tint = extended.expense,
                    bg = extended.expenseContainer,
                    onClick = { onQuickAddTransaction(TransactionType.EXPENSE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_expense")
                )
                QuickActionPill(
                    label = "Income",
                    icon = Icons.Rounded.ArrowDownward,
                    tint = extended.income,
                    bg = extended.incomeContainer,
                    onClick = { onQuickAddTransaction(TransactionType.INCOME) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_income")
                )
                QuickActionPill(
                    label = "Transfer",
                    icon = Icons.Rounded.SwapHoriz,
                    tint = extended.transfer,
                    bg = extended.transferContainer,
                    onClick = { onQuickAddTransaction(TransactionType.TRANSFER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_transfer")
                )
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg.copy(alpha = 0.85f),
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FinancialKpiGrid(
    metrics: DashboardMetrics,
    currency: CurrencyCode
) {
    val extended = LocalWalXColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Month Income",
                value = FinanceMath.formatCurrency(metrics.monthIncome, currency),
                icon = Icons.Rounded.ArrowDownward,
                accentColor = extended.income,
                deltaText = "${if (metrics.incomeMomPercent >= 0) "+" else ""}${metrics.incomeMomPercent}% MoM",
                deltaPositive = metrics.incomeMomPercent >= 0.0,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_month_income")
            )
            KpiMetricCard(
                title = "Month Expenses",
                value = FinanceMath.formatCurrency(metrics.monthExpense, currency),
                icon = Icons.Rounded.ArrowUpward,
                accentColor = extended.expense,
                deltaText = "${if (metrics.expenseMomPercent >= 0) "+" else ""}${metrics.expenseMomPercent}% MoM",
                deltaPositive = metrics.expenseMomPercent <= 0.0,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_month_expenses")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Net Savings Rate",
                value = "${metrics.savingsRatePercent}%",
                icon = Icons.Rounded.Savings,
                accentColor = if (metrics.savingsRatePercent >= 20.0) extended.income else extended.warning,
                deltaText = "Prev mo ${metrics.prevSavingsRatePercent}%",
                deltaPositive = metrics.savingsRatePercent >= metrics.prevSavingsRatePercent,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_savings_rate")
            )
            KpiMetricCard(
                title = "Fixed Burn / Mo",
                value = FinanceMath.formatCurrency(metrics.monthlyFixedBurnRate, currency),
                icon = Icons.Rounded.Autorenew,
                accentColor = extended.transfer,
                deltaText = "Recurring bills",
                deltaPositive = true,
                showTrendArrow = false,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_fixed_burn")
            )
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    deltaText: String,
    deltaPositive: Boolean,
    showTrendArrow: Boolean = true,
    modifier: Modifier = Modifier
) {
    val extended = LocalWalXColors.current
    WalXCard(
        modifier = modifier,
        contentPadding = 14.dp,
        borderColor = extended.border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = extended.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = TabularAmountMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showTrendArrow) {
                Icon(
                    imageVector = if (deltaPositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = if (deltaPositive) extended.income else extended.expense,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = deltaText,
                style = MaterialTheme.typography.labelSmall,
                color = if (!showTrendArrow) extended.textMuted else if (deltaPositive) extended.income else extended.expense,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WalletPulseStrip(
    wallets: List<Wallet>,
    currency: CurrencyCode,
    onInspectWallet: (String) -> Unit
) {
    val extended = LocalWalXColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Wallets Overview",
            style = MaterialTheme.typography.titleSmall,
            color = extended.textMuted
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            wallets.forEach { w ->
                val accent = Color(w.colorHex)
                val isCredit = w.type == WalletType.CREDIT_CARD
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = extended.card,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .widthIn(min = 148.dp)
                        .clickable { onInspectWallet(w.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                walletTypeIcon(w.type),
                                contentDescription = w.name,
                                tint = accent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = w.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = FinanceMath.formatCurrency(w.balance, currency),
                                style = TabularAmountSmall,
                                color = if (isCredit && w.balance > 0.0) extended.expense else accent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalXAiCopilotCard(
    quickLogState: AiQuickLogState,
    insightsState: AiInsightsState,
    onRunQuickLog: (String) -> Unit,
    onGenerateInsights: () -> Unit
) {
    val extended = LocalWalXColors.current
    var quickLogText by remember { mutableStateOf("") }

    WalXCard(
        borderColor = extended.accent.copy(alpha = 0.45f),
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(extended.accentContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = "WalX AI Copilot",
                        tint = extended.accent,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "WalX AI Copilot",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Natural Language Quick-Log & Smart Insights",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                }
            }
            StatusPill("Gemini", extended.accentContainer, extended.accent)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = quickLogText,
                onValueChange = { quickLogText = it },
                placeholder = {
                    Text(
                        "Spent 540 on dinner via Primary Bank",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_quicklog_input")
            )
            Button(
                onClick = {
                    val text = quickLogText.trim()
                    if (text.isNotEmpty()) {
                        onRunQuickLog(text)
                        quickLogText = ""
                    }
                },
                enabled = quickLogState !is AiQuickLogState.Loading && quickLogText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                modifier = Modifier.testTag("ai_quicklog_submit")
            ) {
                if (quickLogState is AiQuickLogState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Send, contentDescription = "Parse with AI", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (quickLogState is AiQuickLogState.Error) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = quickLogState.message,
                style = MaterialTheme.typography.bodySmall,
                color = extended.expense
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monthly Spending Intelligence",
                style = MaterialTheme.typography.labelLarge,
                color = extended.textMuted
            )
            OutlinedButton(
                onClick = onGenerateInsights,
                enabled = insightsState !is AiInsightsState.Loading && insightsState !is AiInsightsState.Streaming,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("ai_insights_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    when (insightsState) {
                        is AiInsightsState.Loading, is AiInsightsState.Streaming -> "Analyzing..."
                        is AiInsightsState.Success -> "Refresh Insights"
                        else -> "Analyze Spending"
                    }
                )
            }
        }

        AnimatedVisibility(visible = insightsState !is AiInsightsState.Idle) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = extended.cardElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    when (insightsState) {
                        is AiInsightsState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Evaluating budget pacing & category burn...", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                            }
                        }
                        is AiInsightsState.Streaming -> {
                            Text(
                                text = insightsState.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is AiInsightsState.Success -> {
                            Text(
                                text = insightsState.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is AiInsightsState.Error -> {
                            Text(
                                text = insightsState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.expense
                            )
                        }
                        AiInsightsState.Idle -> Unit
                    }
                }
            }
        }
    }
}
