package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.CurrencyCode
import com.example.domain.DateRangeFilter
import com.example.domain.FinanceMath
import com.example.domain.FinanceTransaction
import com.example.domain.LedgerSortOption
import com.example.domain.TransactionType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.WalXCard
import com.example.ui.theme.LocalWalXColors
import com.example.ui.theme.TabularAmountSmall
import com.example.ui.viewmodel.WalXUiState

@Composable
fun LedgerScreen(
    state: WalXUiState,
    filteredTransactions: List<FinanceTransaction>,
    currency: CurrencyCode,
    onUpdateFilters: (
        search: String,
        type: TransactionType?,
        walletId: String?,
        categoryId: String?,
        dateRange: DateRangeFilter,
        sort: LedgerSortOption
    ) -> Unit,
    onClearFilters: () -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    val extended = LocalWalXColors.current
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val walletMap = remember(state.wallets) { state.wallets.associateBy { it.id } }

    var showSortMenu by remember { mutableStateOf(false) }
    var showWalletMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val filteredIncome = remember(filteredTransactions) {
        FinanceMath.roundCurrency(filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount })
    }
    val filteredExpense = remember(filteredTransactions) {
        FinanceMath.roundCurrency(filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
    }

    val hasActiveFilters = state.searchQuery.isNotBlank() ||
        state.filterType != null ||
        state.filterWalletId != null ||
        state.filterCategoryId != null ||
        state.filterDateRange != DateRangeFilter.ALL_TIME

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Full Ledger",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${filteredTransactions.size} entries shown",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textMuted
                        )
                    }
                    Box {
                        OutlinedButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("ledger_sort_button")
                        ) {
                            Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(state.sortOption.label)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            LedgerSortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        showSortMenu = false
                                        onUpdateFilters(
                                            state.searchQuery,
                                            state.filterType,
                                            state.filterWalletId,
                                            state.filterCategoryId,
                                            state.filterDateRange,
                                            opt
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Instant Search across Title, Notes, and Tags
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { q ->
                        onUpdateFilters(
                            q,
                            state.filterType,
                            state.filterWalletId,
                            state.filterCategoryId,
                            state.filterDateRange,
                            state.sortOption
                        )
                    },
                    placeholder = { Text("Search title, notes, or #tags...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                onUpdateFilters(
                                    "",
                                    state.filterType,
                                    state.filterWalletId,
                                    state.filterCategoryId,
                                    state.filterDateRange,
                                    state.sortOption
                                )
                            }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_search_input")
                )
            }

            // Filter Chips: Type + Date Range + Wallet + Category
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.filterType == null,
                            onClick = {
                                onUpdateFilters(state.searchQuery, null, state.filterWalletId, state.filterCategoryId, state.filterDateRange, state.sortOption)
                            },
                            label = { Text("All Types") }
                        )
                        TransactionType.entries.forEach { t ->
                            FilterChip(
                                selected = state.filterType == t,
                                onClick = {
                                    onUpdateFilters(state.searchQuery, t, state.filterWalletId, state.filterCategoryId, state.filterDateRange, state.sortOption)
                                },
                                label = { Text(t.displayName) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DateRangeFilter.entries.forEach { range ->
                            FilterChip(
                                selected = state.filterDateRange == range,
                                onClick = {
                                    onUpdateFilters(state.searchQuery, state.filterType, state.filterWalletId, state.filterCategoryId, range, state.sortOption)
                                },
                                label = { Text(range.label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wallet Dropdown Filter
                        Box {
                            val activeWalletName = state.filterWalletId?.let { walletMap[it]?.name } ?: "All Wallets"
                            FilterChip(
                                selected = state.filterWalletId != null,
                                onClick = { showWalletMenu = true },
                                label = { Text(activeWalletName) }
                            )
                            DropdownMenu(expanded = showWalletMenu, onDismissRequest = { showWalletMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("All Wallets") },
                                    onClick = {
                                        showWalletMenu = false
                                        onUpdateFilters(state.searchQuery, state.filterType, null, state.filterCategoryId, state.filterDateRange, state.sortOption)
                                    }
                                )
                                state.wallets.forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text(w.name) },
                                        onClick = {
                                            showWalletMenu = false
                                            onUpdateFilters(state.searchQuery, state.filterType, w.id, state.filterCategoryId, state.filterDateRange, state.sortOption)
                                        }
                                    )
                                }
                            }
                        }

                        // Category Dropdown Filter
                        Box {
                            val activeCatName = state.filterCategoryId?.let { catMap[it]?.name } ?: "All Categories"
                            FilterChip(
                                selected = state.filterCategoryId != null,
                                onClick = { showCategoryMenu = true },
                                label = { Text(activeCatName) }
                            )
                            DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("All Categories") },
                                    onClick = {
                                        showCategoryMenu = false
                                        onUpdateFilters(state.searchQuery, state.filterType, state.filterWalletId, null, state.filterDateRange, state.sortOption)
                                    }
                                )
                                state.categories.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name) },
                                        onClick = {
                                            showCategoryMenu = false
                                            onUpdateFilters(state.searchQuery, state.filterType, state.filterWalletId, c.id, state.filterDateRange, state.sortOption)
                                        }
                                    )
                                }
                            }
                        }

                        if (hasActiveFilters) {
                            TextButton(onClick = onClearFilters) {
                                Text("Reset", color = extended.expense)
                            }
                        }
                    }
                }
            }

            // Filtered Summary Strip
            item {
                WalXCard(contentPadding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Filtered Inflow", style = MaterialTheme.typography.labelMedium, color = extended.textMuted)
                            Text(
                                "+${FinanceMath.formatCurrency(filteredIncome, currency)}",
                                style = TabularAmountSmall,
                                color = extended.income
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Filtered Outflow", style = MaterialTheme.typography.labelMedium, color = extended.textMuted)
                            Text(
                                "-${FinanceMath.formatCurrency(filteredExpense, currency)}",
                                style = TabularAmountSmall,
                                color = extended.expense
                            )
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.FilterListOff,
                        title = "No matching transactions",
                        subtitle = "Try broadening your filters or log a new transaction.",
                        action = if (hasActiveFilters) {
                            { OutlinedButton(onClick = onClearFilters) { Text("Clear Filters") } }
                        } else null
                    )
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        category = tx.categoryId?.let { catMap[it] },
                        wallet = walletMap[tx.walletId],
                        toWallet = tx.toWalletId?.let { walletMap[it] },
                        currency = currency,
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { onDeleteTransaction(tx.id) }
                    )
                }
            }
        }
    }
}
