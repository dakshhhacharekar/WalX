package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.Category
import com.example.domain.CurrencyCode
import com.example.domain.FinanceMath
import com.example.domain.FinanceTransaction
import com.example.domain.RecurringBill
import com.example.domain.RecurringFrequency
import com.example.domain.SavingsGoal
import com.example.domain.TransactionType
import com.example.domain.WalXDateUtils
import com.example.domain.Wallet
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.WalXCard
import com.example.ui.components.categoryIcon
import com.example.ui.theme.LocalWalXColors
import com.example.ui.theme.TabularAmountMedium
import com.example.ui.theme.TabularAmountSmall
import com.example.ui.viewmodel.WalXUiState

@Composable
fun PlanningScreen(
    state: WalXUiState,
    currency: CurrencyCode,
    onAddCategory: (name: String, type: TransactionType, monthlyLimit: Double, colorHex: Long) -> Unit,
    onUpdateCategoryBudget: (categoryId: String, monthlyLimit: Double) -> Unit,
    onDeleteCategorySafely: (categoryId: String, fallbackCategoryId: String?) -> Unit,
    onSaveGoal: (name: String, target: Double, saved: Double, deadline: String, colorHex: Long) -> Unit,
    onContributeGoal: (goalId: String, walletId: String, amount: Double) -> Unit,
    onDeleteGoal: (goalId: String) -> Unit,
    onSaveRecurring: (title: String, amount: Double, categoryId: String?, walletId: String, frequency: RecurringFrequency, nextDueDate: String, notes: String) -> Unit,
    onPayRecurring: (billId: String) -> Unit,
    onDeleteRecurring: (billId: String) -> Unit
) {
    val extended = LocalWalXColors.current
    var subTab by remember { mutableIntStateOf(0) } // 0 = Budgets, 1 = Vaults, 2 = Recurring

    // Dialog states
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingBudgetCat by remember { mutableStateOf<Category?>(null) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showAddRecurringDialog by remember { mutableStateOf(false) }

    val curMonth = remember { WalXDateUtils.currentYearMonth() }
    val expenseCategories = remember(state.categories) {
        state.categories.filter { it.type == TransactionType.EXPENSE }
    }
    val categorySpendMap = remember(state.transactions, curMonth) {
        state.transactions
            .filter { it.type == TransactionType.EXPENSE && it.date.startsWith(curMonth) }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> FinanceMath.roundCurrency(list.sumOf { it.amount }) }
    }
    val walletMap = remember(state.wallets) { state.wallets.associateBy { it.id } }
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }

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
                Text(
                    text = "Budgets, Vaults & Bills",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Segmented Sub-Tab Switcher
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(extended.cardElevated, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Budgets", "Vaults", "Recurring").forEachIndexed { idx, label ->
                        val active = subTab == idx
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) extended.accent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plan_tab_${label.lowercase()}")
                                .clickable { subTab = idx }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) Color.White else extended.textMuted
                                )
                            }
                        }
                    }
                }
            }

            when (subTab) {
                0 -> {
                    // CATEGORY BUDGETS
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category Monthly Caps",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            OutlinedButton(
                                onClick = { showAddCategoryDialog = true },
                                modifier = Modifier.testTag("add_category_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Category")
                            }
                        }
                    }

                    items(expenseCategories, key = { it.id }) { cat ->
                        val spent = categorySpendMap[cat.id] ?: 0.0
                        val pct = if (cat.monthlyLimit > 0.0) FinanceMath.safePercent(spent, cat.monthlyLimit) else 0.0
                        val isOver = cat.monthlyLimit > 0.0 && spent > cat.monthlyLimit
                        val barColor = when {
                            isOver -> extended.expense
                            pct >= 75.0 -> extended.warning
                            else -> Color(cat.colorHex)
                        }

                        WalXCard(contentPadding = 14.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(cat.colorHex).copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        categoryIcon(cat.iconKey),
                                        contentDescription = cat.name,
                                        tint = Color(cat.colorHex),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isOver) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Rounded.WarningAmber,
                                                contentDescription = "Over budget",
                                                tint = extended.expense,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (cat.monthlyLimit > 0.0) {
                                            "${FinanceMath.formatCurrency(spent, currency)} of ${FinanceMath.formatCurrency(cat.monthlyLimit, currency)}"
                                        } else {
                                            "Spent ${FinanceMath.formatCurrency(spent, currency)} • No cap set"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOver) extended.expense else extended.textMuted
                                    )
                                }
                                IconButton(onClick = { editingBudgetCat = cat }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit budget cap", tint = extended.accent)
                                }
                                if (expenseCategories.size > 1) {
                                    IconButton(onClick = {
                                        val fallback = expenseCategories.firstOrNull { it.id != cat.id }?.id
                                        onDeleteCategorySafely(cat.id, fallback)
                                    }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete category", tint = extended.textMuted)
                                    }
                                }
                            }
                            if (cat.monthlyLimit > 0.0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(extended.border)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((pct / 100.0).toFloat().coerceIn(0f, 1f))
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(barColor)
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // SAVINGS GOALS (VAULTS)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Savings Vaults (${state.goals.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Button(
                                onClick = { showAddGoalDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = extended.income),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("add_goal_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF042217), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Vault", color = Color(0xFF042217), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (state.goals.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Savings,
                                title = "No savings vaults yet",
                                subtitle = "Create a target vault for your emergency fund, travel, or next big upgrade."
                            )
                        }
                    } else {
                        items(state.goals, key = { it.id }) { goal ->
                            val pct = FinanceMath.safePercent(goal.savedAmount, goal.targetAmount)
                            val accent = Color(goal.colorHex)
                            WalXCard(borderColor = accent.copy(alpha = 0.4f), contentPadding = 16.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = goal.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Target date: ${goal.deadline}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.textMuted
                                        )
                                    }
                                    StatusPill("${pct}%", accent.copy(alpha = 0.18f), accent)
                                    IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete goal", tint = extended.textMuted)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = FinanceMath.formatCurrency(goal.savedAmount, currency),
                                        style = TabularAmountMedium,
                                        color = accent
                                    )
                                    Text(
                                        text = "of ${FinanceMath.formatCurrency(goal.targetAmount, currency)}",
                                        style = TabularAmountSmall,
                                        color = extended.textMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(extended.border)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((pct / 100.0).toFloat().coerceIn(0f, 1f))
                                            .height(9.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(accent)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { contributingGoal = goal },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contribute_goal_${goal.id}")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ Contribute from Wallet")
                                }
                            }
                        }
                    }
                }

                else -> {
                    // RECURRING BILLS & SUBSCRIPTIONS
                    item {
                        WalXCard(
                            borderColor = extended.accent.copy(alpha = 0.42f),
                            contentPadding = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Monthly Fixed Burn Rate", style = MaterialTheme.typography.labelMedium, color = extended.textMuted)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = FinanceMath.formatCurrency(state.metrics.monthlyFixedBurnRate, currency),
                                        style = TabularAmountMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Button(
                                    onClick = { showAddRecurringDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("add_recurring_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Bill", color = Color.White)
                                }
                            }
                        }
                    }

                    if (state.recurringBills.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Autorenew,
                                title = "No recurring bills scheduled",
                                subtitle = "Track rent, broadband, or subscriptions and log payments in 1 tap."
                            )
                        }
                    } else {
                        items(state.recurringBills, key = { it.id }) { bill ->
                            val wName = walletMap[bill.walletId]?.name ?: "Wallet"
                            val cName = bill.categoryId?.let { catMap[it]?.name } ?: "General"
                            WalXCard(contentPadding = 14.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = bill.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            StatusPill(bill.frequency.displayName, extended.accentContainer, extended.accent)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$cName • $wName • Due ${bill.nextDueDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.textMuted
                                        )
                                    }
                                    Text(
                                        text = FinanceMath.formatCurrency(bill.amount, currency),
                                        style = TabularAmountSmall,
                                        color = extended.expense
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onDeleteRecurring(bill.id) }) {
                                        Text("Delete", color = extended.textMuted)
                                    }
                                    Button(
                                        onClick = { onPayRecurring(bill.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = extended.income),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("pay_recurring_${bill.id}")
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF032117), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pay & Log", color = Color(0xFF032117), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    editingBudgetCat?.let { cat ->
        EditBudgetCapDialog(
            category = cat,
            currency = currency,
            onDismiss = { editingBudgetCat = null },
            onSave = { newCap ->
                onUpdateCategoryBudget(cat.id, newCap)
                editingBudgetCat = null
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            currency = currency,
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { name, type, cap ->
                onAddCategory(name, type, cap, 0xFF6366F1L)
                showAddCategoryDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            currency = currency,
            onDismiss = { showAddGoalDialog = false },
            onSave = { name, target, saved, deadline ->
                onSaveGoal(name, target, saved, deadline, 0xFF10B981L)
                showAddGoalDialog = false
            }
        )
    }

    contributingGoal?.let { goal ->
        ContributeGoalDialog(
            goal = goal,
            wallets = state.wallets,
            currency = currency,
            onDismiss = { contributingGoal = null },
            onContribute = { walletId, amount ->
                onContributeGoal(goal.id, walletId, amount)
                contributingGoal = null
            }
        )
    }

    if (showAddRecurringDialog) {
        AddRecurringDialog(
            wallets = state.wallets,
            categories = expenseCategories,
            currency = currency,
            onDismiss = { showAddRecurringDialog = false },
            onSave = { title, amt, catId, wId, freq, due, notes ->
                onSaveRecurring(title, amt, catId, wId, freq, due, notes)
                showAddRecurringDialog = false
            }
        )
    }
}

@Composable
private fun EditBudgetCapDialog(
    category: Category,
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var capText by remember(category) { mutableStateOf(category.monthlyLimit.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly Cap: ${category.name}") },
        text = {
            OutlinedTextField(
                value = capText,
                onValueChange = { capText = it },
                label = { Text("Monthly Limit (${currency.symbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val parsed = capText.trim().toDoubleOrNull() ?: 0.0
                onSave(FinanceMath.roundCurrency(parsed.coerceAtLeast(0.0)))
            }) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddCategoryDialog(
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onAdd: (name: String, type: TransactionType, cap: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var capText by remember { mutableStateOf("5000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") }
                    )
                }
                if (type == TransactionType.EXPENSE) {
                    OutlinedTextField(
                        value = capText,
                        onValueChange = { capText = it },
                        label = { Text("Monthly Budget Cap (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.trim().isNotEmpty()) {
                    onAdd(name.trim(), type, capText.trim().toDoubleOrNull() ?: 0.0)
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddGoalDialog(
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onSave: (name: String, target: Double, saved: Double, deadline: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("50000") }
    var saved by remember { mutableStateOf("0") }
    var deadline by remember { mutableStateOf(WalXDateUtils.daysFromNowIso(120)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Savings Vault") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Vault Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target Amount (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = saved, onValueChange = { saved = it }, label = { Text("Initial Saved (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Deadline (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val t = target.toDoubleOrNull() ?: return@Button
                if (name.trim().isNotEmpty() && t > 0) {
                    onSave(name.trim(), t, saved.toDoubleOrNull() ?: 0.0, deadline.trim())
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ContributeGoalDialog(
    goal: SavingsGoal,
    wallets: List<Wallet>,
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onContribute: (walletId: String, amount: Double) -> Unit
) {
    var amountText by remember { mutableStateOf("2500") }
    var selectedWalletId by remember(wallets) { mutableStateOf(wallets.firstOrNull()?.id.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contribute to ${goal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Contribution Amount (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Deduct from Wallet:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wallets.forEach { w ->
                        FilterChip(
                            selected = selectedWalletId == w.id,
                            onClick = { selectedWalletId = w.id },
                            label = { Text(w.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (amt > 0.0 && selectedWalletId.isNotBlank()) {
                    onContribute(selectedWalletId, amt)
                }
            }) { Text("Contribute") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddRecurringDialog(
    wallets: List<Wallet>,
    categories: List<Category>,
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, categoryId: String?, walletId: String, freq: RecurringFrequency, due: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var nextDue by remember { mutableStateOf(WalXDateUtils.daysFromNowIso(15)) }
    var selectedWalletId by remember(wallets) { mutableStateOf(wallets.firstOrNull()?.id.orEmpty()) }
    var selectedCatId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Recurring Bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Bill Title") }, placeholder = { Text("e.g. Netflix, Rent") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurringFrequency.entries.forEach { f ->
                        FilterChip(selected = freq == f, onClick = { freq = f }, label = { Text(f.displayName) })
                    }
                }
                OutlinedTextField(value = nextDue, onValueChange = { nextDue = it }, label = { Text("Next Due Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    wallets.forEach { w ->
                        FilterChip(selected = selectedWalletId == w.id, onClick = { selectedWalletId = w.id }, label = { Text(w.name) })
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { c ->
                        FilterChip(selected = selectedCatId == c.id, onClick = { selectedCatId = c.id }, label = { Text(c.name) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (title.trim().isNotEmpty() && amt > 0.0 && selectedWalletId.isNotBlank()) {
                    onSave(title.trim(), amt, selectedCatId, selectedWalletId, freq, nextDue.trim(), "")
                }
            }) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
