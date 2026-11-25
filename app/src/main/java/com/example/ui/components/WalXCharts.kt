package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CashflowPoint
import com.example.domain.CashflowRange
import com.example.domain.CategorySpendSlice
import com.example.domain.CurrencyCode
import com.example.domain.DashboardMetrics
import com.example.domain.FinanceMath
import com.example.ui.theme.LocalWalXColors
import com.example.ui.theme.TabularAmountMedium
import com.example.ui.theme.TabularAmountSmall
import kotlin.math.max

@Composable
fun MonthlyBudgetCard(
    metrics: DashboardMetrics,
    currency: CurrencyCode,
    modifier: Modifier = Modifier
) {
    val extended = LocalWalXColors.current
    val pct = metrics.budgetUsedPercent.coerceAtLeast(0.0)
    val barColor = when {
        pct >= 90.0 -> extended.expense
        pct >= 70.0 -> extended.warning
        else -> extended.income
    }
    val animatedProgress by animateFloatAsState(
        targetValue = (pct / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "budget_bar_anim"
    )

    WalXCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Monthly Budget Pacing",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${metrics.daysLeftInMonth} days left this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted
                )
            }
            StatusPill(
                text = "${pct}% used",
                containerColor = barColor.copy(alpha = 0.16f),
                contentColor = barColor
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .clip(RoundedCornerShape(50))
                .background(extended.border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(11.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BudgetStatColumn(
                label = "Spent",
                value = FinanceMath.formatCurrency(metrics.monthExpense, currency),
                valueColor = MaterialTheme.colorScheme.onSurface
            )
            BudgetStatColumn(
                label = if (metrics.budgetRemaining >= 0) "Remaining" else "Over Budget",
                value = FinanceMath.formatCurrency(kotlin.math.abs(metrics.budgetRemaining), currency),
                valueColor = if (metrics.budgetRemaining >= 0) extended.income else extended.expense,
                alignEnd = false
            )
            BudgetStatColumn(
                label = "Safe / Day",
                value = FinanceMath.formatCurrency(metrics.dailySafeSpend, currency),
                valueColor = extended.accent,
                alignEnd = true
            )
        }
    }
}

@Composable
private fun BudgetStatColumn(
    label: String,
    value: String,
    valueColor: Color,
    alignEnd: Boolean = false
) {
    val extended = LocalWalXColors.current
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = extended.textMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = TabularAmountSmall,
            color = valueColor
        )
    }
}

@Composable
fun CashflowTrendCard(
    points: List<CashflowPoint>,
    selectedRange: CashflowRange,
    currency: CurrencyCode,
    onSelectRange: (CashflowRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalWalXColors.current
    val hasData = points.any { it.income > 0.0 || it.expense > 0.0 }
    var selectedIndex by remember(points, selectedRange) {
        mutableIntStateOf(points.indexOfLast { it.income > 0.0 || it.expense > 0.0 }.coerceAtLeast(0))
    }

    WalXCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cashflow Trend",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    LegendDot("Income", extended.income)
                    LegendDot("Expense", extended.expense)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = extended.cardElevated
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    CashflowRange.entries.forEach { range ->
                        val active = range == selectedRange
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (active) extended.accent else Color.Transparent,
                            modifier = Modifier
                                .testTag("cashflow_toggle_${range.name.lowercase()}")
                                .clickable { onSelectRange(range) }
                        ) {
                            Text(
                                text = if (range == CashflowRange.LAST_14_DAYS) "14D" else "6M",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else extended.textMuted,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!hasData || points.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.BarChart,
                title = "No cashflow activity yet",
                subtitle = "Log income or expenses to unlock your interactive trend chart."
            )
            return@WalXCard
        }

        // Interactive Tooltip Banner for tapped bar
        val activePoint = points.getOrNull(selectedIndex.coerceIn(0, points.lastIndex))
        if (activePoint != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = extended.cardElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedRange == CashflowRange.LAST_14_DAYS) "Day ${activePoint.label}" else activePoint.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "+${FinanceMath.formatCurrency(activePoint.income, currency, compact = true)}",
                            style = TabularAmountSmall,
                            color = extended.income
                        )
                        Text(
                            text = "-${FinanceMath.formatCurrency(activePoint.expense, currency, compact = true)}",
                            style = TabularAmountSmall,
                            color = extended.expense
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val maxVal = max(1.0, points.maxOf { max(it.income, it.expense) })
        val gridColor = extended.border
        val incColor = extended.income
        val expColor = extended.expense
        val highlightColor = extended.accent.copy(alpha = 0.14f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty() && size.width > 0) {
                                val slotW = size.width.toFloat() / points.size
                                val tapped = (offset.x / slotW).toInt().coerceIn(0, points.lastIndex)
                                selectedIndex = tapped
                            }
                        }
                    }
            ) {
                val chartH = size.height - 12f
                val slotW = size.width / points.size.toFloat()

                // Horizontal reference grid lines
                for (g in 0..3) {
                    val y = chartH * (g / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                points.forEachIndexed { idx, pt ->
                    val slotLeft = idx * slotW
                    if (idx == selectedIndex) {
                        drawRoundRect(
                            color = highlightColor,
                            topLeft = Offset(slotLeft + 2f, 0f),
                            size = Size((slotW - 4f).coerceAtLeast(4f), chartH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                    val barW = (slotW * 0.30f).coerceIn(5f, 24f)
                    val gap = (slotW * 0.08f).coerceAtLeast(2f)
                    val centerX = slotLeft + slotW / 2f

                    val incH = ((pt.income / maxVal) * (chartH * 0.90f)).toFloat()
                    val expH = ((pt.expense / maxVal) * (chartH * 0.90f)).toFloat()

                    if (incH > 1f) {
                        drawRoundRect(
                            color = incColor,
                            topLeft = Offset(centerX - barW - gap / 2f, chartH - incH),
                            size = Size(barW, incH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                    if (expH > 1f) {
                        drawRoundRect(
                            color = expColor,
                            topLeft = Offset(centerX + gap / 2f, chartH - expH),
                            size = Size(barW, expH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
            }
        }

        // Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = if (points.size > 8) 2 else 1
            points.forEachIndexed { idx, pt ->
                if (idx % step == 0 || idx == points.lastIndex) {
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (idx == selectedIndex) extended.accent else extended.textMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseBreakdownCard(
    slices: List<CategorySpendSlice>,
    totalExpense: Double,
    currency: CurrencyCode,
    modifier: Modifier = Modifier
) {
    val extended = LocalWalXColors.current
    var selectedSliceIdx by remember(slices) { mutableIntStateOf(-1) }

    WalXCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Expense Breakdown",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Current month category distribution",
            style = MaterialTheme.typography.bodySmall,
            color = extended.textMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (slices.isEmpty() || totalExpense <= 0.0) {
            EmptyStateCard(
                icon = Icons.Rounded.DonutLarge,
                title = "No expenses this month",
                subtitle = "Your category spending donut will appear once you log an expense."
            )
            return@WalXCard
        }

        val activeSlice = slices.getOrNull(selectedSliceIdx)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(174.dp)) {
                val strokeWidth = 24.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                var startAngle = -90f

                slices.forEachIndexed { idx, slice ->
                    val sweep = ((slice.amount / totalExpense) * 360.0).toFloat().coerceAtLeast(2f)
                    val isSelected = selectedSliceIdx == idx
                    drawArc(
                        color = Color(slice.colorHex),
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2.2f).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = activeSlice?.categoryName ?: "Total Spent",
                    style = MaterialTheme.typography.labelMedium,
                    color = extended.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = FinanceMath.formatCurrency(
                        activeSlice?.amount ?: totalExpense,
                        currency,
                        compact = true
                    ),
                    style = TabularAmountMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (activeSlice != null) {
                    Text(
                        text = "${activeSlice.percentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(activeSlice.colorHex)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Ranked legend (tap to highlight)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slices.take(6).forEachIndexed { idx, slice ->
                val isSelected = selectedSliceIdx == idx
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) extended.cardElevated else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedSliceIdx = if (selectedSliceIdx == idx) -1 else idx
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(slice.colorHex))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = slice.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = FinanceMath.formatCurrency(slice.amount, currency),
                            style = TabularAmountSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${slice.percentage}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.textMuted,
                            modifier = Modifier.width(42.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    val extended = LocalWalXColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = extended.textMuted
        )
    }
}
