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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.domain.CurrencyCode
import com.example.domain.DashboardMetrics
import com.example.domain.FinanceMath
import com.example.domain.FinanceTransaction
import com.example.domain.Wallet
import com.example.domain.WalletType
import com.example.ui.components.StatusPill
import com.example.ui.components.WalXCard
import com.example.ui.components.walletTypeIcon
import com.example.ui.theme.LocalWalXColors
import com.example.ui.theme.TabularAmountLarge
import com.example.ui.theme.TabularAmountMedium
import com.example.ui.theme.TabularAmountSmall

private val walletColorChoices = listOf(
    0xFF6366F1L, 0xFF10B981L, 0xFFF43F5EL, 0xFF38BDF8L, 0xFFF59E0BL, 0xFFA855F7L
)

@Composable
fun WalletsScreen(
    wallets: List<Wallet>,
    transactions: List<FinanceTransaction>,
    metrics: DashboardMetrics,
    currency: CurrencyCode,
    onSaveWallet: (id: String?, name: String, type: WalletType, balance: Double, creditLimit: Double, colorHex: Long) -> Unit,
    onDeleteWalletSafely: (walletId: String, fallbackWalletId: String) -> Unit,
    onInspectWalletActivity: (walletId: String) -> Unit
) {
    val extended = LocalWalXColors.current
    var editingWallet by remember { mutableStateOf<Wallet?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var deletingWallet by remember { mutableStateOf<Wallet?>(null) }

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
                            text = "Multi-Wallet Manager",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${wallets.size} connected accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textMuted
                        )
                    }
                    Button(
                        onClick = {
                            editingWallet = null
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_wallet_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Wallet", color = Color.White)
                    }
                }
            }

            // Net Liquid vs Credit Liability Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WalXCard(modifier = Modifier.weight(1f), contentPadding = 14.dp) {
                        Text("Liquid Assets", style = MaterialTheme.typography.labelMedium, color = extended.textMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            FinanceMath.formatCurrency(metrics.totalLiquidAssets, currency),
                            style = TabularAmountMedium,
                            color = extended.income
                        )
                    }
                    WalXCard(modifier = Modifier.weight(1f), contentPadding = 14.dp) {
                        Text("Credit Card Debt", style = MaterialTheme.typography.labelMedium, color = extended.textMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            FinanceMath.formatCurrency(metrics.totalCreditDebt, currency),
                            style = TabularAmountMedium,
                            color = if (metrics.totalCreditDebt > 0.0) extended.expense else extended.textMuted
                        )
                    }
                }
            }

            items(wallets, key = { it.id }) { wallet ->
                val txCount = remember(transactions, wallet.id) {
                    transactions.count { it.walletId == wallet.id || it.toWalletId == wallet.id }
                }
                WalletDetailCard(
                    wallet = wallet,
                    txCount = txCount,
                    currency = currency,
                    canDelete = wallets.size > 1,
                    onEdit = {
                        editingWallet = wallet
                        showAddEditDialog = true
                    },
                    onDelete = { deletingWallet = wallet },
                    onInspect = { onInspectWalletActivity(wallet.id) }
                )
            }
        }
    }

    if (showAddEditDialog) {
        AddEditWalletDialog(
            initial = editingWallet,
            currency = currency,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, type, bal, limit, colorHex ->
                onSaveWallet(editingWallet?.id, name, type, bal, limit, colorHex)
                showAddEditDialog = false
            }
        )
    }

    deletingWallet?.let { target ->
        val fallbacks = wallets.filter { it.id != target.id }
        val linkedCount = transactions.count { it.walletId == target.id || it.toWalletId == target.id }
        SafeDeleteWalletDialog(
            wallet = target,
            linkedCount = linkedCount,
            fallbackWallets = fallbacks,
            onDismiss = { deletingWallet = null },
            onConfirm = { fallbackId ->
                onDeleteWalletSafely(target.id, fallbackId)
                deletingWallet = null
            }
        )
    }
}

@Composable
private fun WalletDetailCard(
    wallet: Wallet,
    txCount: Int,
    currency: CurrencyCode,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onInspect: () -> Unit
) {
    val extended = LocalWalXColors.current
    val accent = Color(wallet.colorHex)
    val isCredit = wallet.type == WalletType.CREDIT_CARD
    val utilizationPct = if (isCredit) FinanceMath.safePercent(wallet.balance, wallet.creditLimit) else 0.0
    val utilColor = when {
        utilizationPct >= 75.0 -> extended.expense
        utilizationPct >= 40.0 -> extended.warning
        else -> extended.income
    }

    WalXCard(
        borderColor = accent.copy(alpha = 0.42f),
        contentPadding = 16.dp,
        modifier = Modifier.testTag("wallet_card_${wallet.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = walletTypeIcon(wallet.type),
                        contentDescription = wallet.type.displayName,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${wallet.type.displayName} • $txCount transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit wallet", tint = extended.textMuted)
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete wallet", tint = extended.expense)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = if (isCredit) "Current Card Balance Owed" else "Available Balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = extended.textMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = FinanceMath.formatCurrency(wallet.balance, currency),
                    style = TabularAmountLarge,
                    color = if (isCredit && wallet.balance > 0.0) extended.expense else MaterialTheme.colorScheme.onSurface
                )
            }
            OutlinedButton(
                onClick = onInspect,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Activity")
            }
        }

        if (isCredit && wallet.creditLimit > 0.0) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Credit Limit: ${FinanceMath.formatCurrency(wallet.creditLimit, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted
                )
                StatusPill(
                    text = "${utilizationPct}% used",
                    containerColor = utilColor.copy(alpha = 0.16f),
                    contentColor = utilColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(extended.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((utilizationPct / 100.0).toFloat().coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(utilColor)
                )
            }
        }
    }
}

@Composable
private fun AddEditWalletDialog(
    initial: Wallet?,
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onSave: (name: String, type: WalletType, balance: Double, creditLimit: Double, colorHex: Long) -> Unit
) {
    val extended = LocalWalXColors.current
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var type by remember(initial) { mutableStateOf(initial?.type ?: WalletType.BANK) }
    var balanceText by remember(initial) { mutableStateOf(initial?.balance?.toString() ?: "0") }
    var limitText by remember(initial) { mutableStateOf(initial?.creditLimit?.takeIf { it > 0 }?.toString() ?: "100000") }
    var selectedColor by remember(initial) { mutableLongStateOf(initial?.colorHex ?: walletColorChoices.first()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Wallet" else "Edit Wallet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Wallet Name") },
                    placeholder = { Text("e.g. HDFC Savings, Axis Ace") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Account Type", style = MaterialTheme.typography.labelLarge, color = extended.textMuted)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WalletType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it; error = null },
                    label = {
                        Text(
                            if (type == WalletType.CREDIT_CARD) "Current Card Balance Owed (${currency.symbol})"
                            else "Current Balance (${currency.symbol})"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (type == WalletType.CREDIT_CARD) {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it; error = null },
                        label = { Text("Credit Limit (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Accent Color", style = MaterialTheme.typography.labelLarge, color = extended.textMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    walletColorChoices.forEach { hex ->
                        val active = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = extended.expense)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.trim().isEmpty()) {
                    error = "Enter a wallet name."
                    return@Button
                }
                val bal = balanceText.trim().toDoubleOrNull()
                if (bal == null) {
                    error = "Enter a valid numeric balance."
                    return@Button
                }
                val limit = if (type == WalletType.CREDIT_CARD) {
                    limitText.trim().toDoubleOrNull() ?: 0.0
                } else 0.0
                onSave(name.trim(), type, FinanceMath.roundCurrency(bal), FinanceMath.roundCurrency(limit), selectedColor)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SafeDeleteWalletDialog(
    wallet: Wallet,
    linkedCount: Int,
    fallbackWallets: List<Wallet>,
    onDismiss: () -> Unit,
    onConfirm: (fallbackWalletId: String) -> Unit
) {
    val extended = LocalWalXColors.current
    var selectedFallbackId by remember(fallbackWallets) { mutableStateOf(fallbackWallets.firstOrNull()?.id.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete '${wallet.name}'?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (linkedCount > 0) {
                        "This wallet is linked to $linkedCount transaction(s). Choose a fallback wallet to safely reassign them so no records are orphaned:"
                    } else {
                        "No transactions are linked to this wallet. Confirm a fallback wallet for referential safety:"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fallbackWallets.forEach { fb ->
                        FilterChip(
                            selected = selectedFallbackId == fb.id,
                            onClick = { selectedFallbackId = fb.id },
                            label = { Text(fb.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedFallbackId.isNotBlank()) onConfirm(selectedFallbackId) },
                colors = ButtonDefaults.buttonColors(containerColor = extended.expense)
            ) {
                Text("Reassign & Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
