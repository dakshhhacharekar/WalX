package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.Category
import com.example.domain.CurrencyCode
import com.example.domain.FinanceMath
import com.example.domain.FinanceTransaction
import com.example.domain.TransactionType
import com.example.domain.WalXDateUtils
import com.example.domain.Wallet
import com.example.ui.theme.LocalWalXColors
import com.example.ui.theme.TabularAmountSmall

@Composable
fun TransactionRowItem(
    transaction: FinanceTransaction,
    category: Category?,
    wallet: Wallet?,
    toWallet: Wallet?,
    currency: CurrencyCode,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalWalXColors.current
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> extended.income
        TransactionType.EXPENSE -> extended.expense
        TransactionType.TRANSFER -> extended.transfer
    }
    val badgeColor = when (transaction.type) {
        TransactionType.TRANSFER -> extended.transfer
        else -> category?.let { Color(it.colorHex) } ?: extended.accent
    }
    val signPrefix = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }

    WalXCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tx_card_${transaction.id}"),
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.type == TransactionType.TRANSFER) {
                        Icons.Rounded.SwapHoriz
                    } else {
                        categoryIcon(category?.iconKey ?: "category")
                    },
                    contentDescription = category?.name ?: transaction.type.displayName,
                    tint = badgeColor,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val metaText = if (transaction.type == TransactionType.TRANSFER) {
                    val src = wallet?.name ?: "Wallet"
                    val dst = toWallet?.name ?: "Vault"
                    "$src → $dst • ${WalXDateUtils.formatFriendlyDate(transaction.date)}"
                } else {
                    val catName = category?.name ?: "Uncategorized"
                    val wName = wallet?.name ?: "Wallet"
                    "$catName • $wName • ${WalXDateUtils.formatFriendlyDate(transaction.date)}"
                }
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        transaction.tags.take(3).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = extended.accent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$signPrefix${FinanceMath.formatCurrency(transaction.amount, currency)}",
                    style = TabularAmountSmall,
                    color = amountColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("tx_edit_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit transaction",
                            tint = extended.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("tx_delete_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete transaction",
                            tint = extended.expense.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
