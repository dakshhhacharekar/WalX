package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.Category
import com.example.domain.CurrencyCode
import com.example.domain.FinanceMath
import com.example.domain.SafeMathEvaluator
import com.example.domain.TransactionType
import com.example.domain.WalXDateUtils
import com.example.domain.Wallet
import com.example.ui.theme.LocalWalXColors
import com.example.ui.theme.TabularAmountSmall
import com.example.ui.viewmodel.TransactionDraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionModalSheet(
    draft: TransactionDraft,
    wallets: List<Wallet>,
    categories: List<Category>,
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    val extended = LocalWalXColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var txType by remember(draft) { mutableStateOf(draft.type) }
    var amountExpr by remember(draft) { mutableStateOf(draft.amountExpr) }
    var title by remember(draft) { mutableStateOf(draft.title) }
    var selectedCategoryId by remember(draft) { mutableStateOf(draft.categoryId) }
    var selectedWalletId by remember(draft) { mutableStateOf(draft.walletId.ifBlank { wallets.firstOrNull()?.id.orEmpty() }) }
    var selectedToWalletId by remember(draft) { mutableStateOf(draft.toWalletId) }
    var dateIso by remember(draft) { mutableStateOf(draft.date) }
    var notes by remember(draft) { mutableStateOf(draft.notes) }
    var tagsText by remember(draft) { mutableStateOf(draft.tagsText) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val relevantCategories = remember(categories, txType) {
        categories.filter { it.type == txType }
    }

    LaunchedEffect(txType, relevantCategories) {
        if (txType != TransactionType.TRANSFER) {
            if (relevantCategories.none { it.id == selectedCategoryId }) {
                selectedCategoryId = relevantCategories.firstOrNull()?.id
            }
        } else {
            if (selectedToWalletId == null || selectedToWalletId == selectedWalletId) {
                selectedToWalletId = wallets.firstOrNull { it.id != selectedWalletId }?.id
            }
        }
    }

    val evalResult = remember(amountExpr) { SafeMathEvaluator.evaluate(amountExpr) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = extended.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (draft.editingId == null) "Log Transaction" else "Edit Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Segmented Switcher: Expense | Income | Transfer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extended.cardElevated, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransactionType.entries.forEach { mode ->
                    val active = txType == mode
                    val activeColor = when (mode) {
                        TransactionType.EXPENSE -> extended.expense
                        TransactionType.INCOME -> extended.income
                        TransactionType.TRANSFER -> extended.transfer
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) activeColor else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_mode_${mode.name.lowercase()}")
                            .clickable {
                                txType = mode
                                validationError = null
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) Color.White else extended.textMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Safe Inline Math Amount Field
            OutlinedTextField(
                value = amountExpr,
                onValueChange = {
                    amountExpr = it
                    validationError = null
                },
                label = { Text("Amount or math expression (${currency.symbol})") },
                placeholder = { Text("e.g. 120 + 45.50 or (250*2)") },
                leadingIcon = { Icon(Icons.Rounded.Calculate, contentDescription = "Calculator") },
                trailingIcon = {
                    evalResult.getOrNull()?.takeIf { it > 0.0 }?.let { computed ->
                        StatusPill(
                            text = "= ${FinanceMath.formatCurrency(computed, currency)}",
                            containerColor = extended.accentContainer,
                            contentColor = extended.accent,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            // Quick math operator bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("+", "-", "*", "/", "(", ")").forEach { op ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = extended.cardElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { amountExpr += op }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = op,
                                style = TabularAmountSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    validationError = null
                },
                label = { Text("Title / Merchant") },
                placeholder = { Text(if (txType == TransactionType.TRANSFER) "e.g. Card Bill Payment" else "e.g. Mainland China Dinner") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Wallet Selector
            Text(
                text = if (txType == TransactionType.TRANSFER) "From Wallet" else "Wallet",
                style = MaterialTheme.typography.labelLarge,
                color = extended.textMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wallets.forEach { w ->
                    FilterChip(
                        selected = selectedWalletId == w.id,
                        onClick = {
                            selectedWalletId = w.id
                            if (txType == TransactionType.TRANSFER && selectedToWalletId == w.id) {
                                selectedToWalletId = wallets.firstOrNull { it.id != w.id }?.id
                            }
                            validationError = null
                        },
                        label = { Text(w.name) },
                        leadingIcon = {
                            Icon(
                                walletTypeIcon(w.type),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            // Destination Wallet for Transfer (disables selecting same wallet)
            if (txType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "To Wallet",
                    style = MaterialTheme.typography.labelLarge,
                    color = extended.textMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (wallets.size < 2) {
                    Text(
                        text = "Add at least 2 wallets in the Wallets tab to make a transfer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.expense
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        wallets.forEach { w ->
                            val isSame = w.id == selectedWalletId
                            FilterChip(
                                selected = selectedToWalletId == w.id,
                                enabled = !isSame,
                                onClick = {
                                    selectedToWalletId = w.id
                                    validationError = null
                                },
                                label = { Text(w.name) },
                                leadingIcon = {
                                    Icon(
                                        walletTypeIcon(w.type),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                // Category Selector
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = extended.textMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    relevantCategories.forEach { cat ->
                        val catColor = Color(cat.colorHex)
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.22f),
                                selectedLabelColor = catColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date field + Quick Today/Yesterday buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = dateIso,
                    onValueChange = { dateIso = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = dateIso == WalXDateUtils.todayIso(),
                    onClick = { dateIso = WalXDateUtils.todayIso() },
                    label = { Text("Today") }
                )
                FilterChip(
                    selected = dateIso == WalXDateUtils.daysAgoIso(1),
                    onClick = { dateIso = WalXDateUtils.daysAgoIso(1) },
                    label = { Text("Yest") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text("Tags (comma separated)") },
                placeholder = { Text("dinner, friends, work") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.expense,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val computed = SafeMathEvaluator.evaluate(amountExpr).getOrElse { err ->
                        validationError = err.message ?: "Invalid amount expression"
                        return@Button
                    }
                    if (computed <= 0.0) {
                        validationError = "Amount must be greater than zero."
                        return@Button
                    }
                    if (title.trim().isEmpty()) {
                        validationError = "Please enter a title or merchant."
                        return@Button
                    }
                    if (selectedWalletId.isBlank()) {
                        validationError = "Please select a wallet."
                        return@Button
                    }
                    if (txType == TransactionType.TRANSFER) {
                        if (selectedToWalletId.isNullOrBlank() || selectedToWalletId == selectedWalletId) {
                            validationError = "Select two different wallets for a transfer."
                            return@Button
                        }
                    }
                    if (!WalXDateUtils.isValidIsoDate(dateIso.trim())) {
                        validationError = "Use valid YYYY-MM-DD date format."
                        return@Button
                    }
                    onSave(
                        draft.editingId,
                        txType,
                        computed,
                        title.trim(),
                        selectedCategoryId,
                        selectedWalletId,
                        selectedToWalletId,
                        dateIso.trim(),
                        notes.trim(),
                        tagsText.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("tx_save_button")
            ) {
                Text(
                    text = if (draft.editingId == null) "Save Transaction" else "Update Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        dateIso = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
