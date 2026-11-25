package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.WalletType
import com.example.ui.theme.LocalWalXColors
import com.example.ui.theme.WalXEmerald
import com.example.ui.theme.WalXIndigo
import com.example.ui.theme.WalXSky

/**
 * Custom geometric WalX SVG-style mark drawn natively via Compose Canvas.
 */
@Composable
fun WalXLogoMark(
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalWalXColors.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF15192B), Color(0xFF0D111D))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height
            val strokeW = w * 0.14f

            // Geometric W wing (Indigo -> Sky gradient)
            val wPath = Path().apply {
                moveTo(w * 0.08f, h * 0.22f)
                lineTo(w * 0.26f, h * 0.78f)
                lineTo(w * 0.44f, h * 0.36f)
                lineTo(w * 0.60f, h * 0.78f)
            }
            drawPath(
                path = wPath,
                brush = Brush.linearGradient(listOf(WalXIndigo, WalXSky)),
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Geometric X slash (Emerald accent)
            drawLine(
                brush = Brush.linearGradient(listOf(WalXEmerald, WalXSky)),
                start = Offset(w * 0.60f, h * 0.78f),
                end = Offset(w * 0.92f, h * 0.22f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = WalXEmerald,
                start = Offset(w * 0.60f, h * 0.24f),
                end = Offset(w * 0.92f, h * 0.76f),
                strokeWidth = strokeW * 0.9f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun WalXBrandLockup(
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extended = LocalWalXColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WalXLogoMark(size = if (compact) 34.dp else 44.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "WalX",
                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "by Nor Gaxmer Builds",
                style = MaterialTheme.typography.labelMedium,
                color = extended.textMuted
            )
        }
    }
}

@Composable
fun WalXCard(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalWalXColors.current.card,
    borderColor: Color = LocalWalXColors.current.border,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val extended = LocalWalXColors.current
    WalXCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(extended.accentContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = extended.accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textMuted,
                textAlign = TextAlign.Center
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(14.dp))
                action()
            }
        }
    }
}

fun walletTypeIcon(type: WalletType): ImageVector = when (type) {
    WalletType.BANK -> Icons.Rounded.AccountBalance
    WalletType.CASH -> Icons.Rounded.AccountBalanceWallet
    WalletType.CREDIT_CARD -> Icons.Rounded.CreditCard
    WalletType.DIGITAL_WALLET -> Icons.Rounded.PhoneAndroid
}

fun categoryIcon(iconKey: String): ImageVector = when (iconKey) {
    "restaurant" -> Icons.Rounded.Restaurant
    "shopping_cart" -> Icons.Rounded.ShoppingCart
    "directions_car" -> Icons.Rounded.DirectionsCar
    "home" -> Icons.Rounded.Home
    "bolt" -> Icons.Rounded.Bolt
    "shopping_bag" -> Icons.Rounded.ShoppingBag
    "movie" -> Icons.Rounded.Movie
    "favorite" -> Icons.Rounded.Favorite
    "subscriptions" -> Icons.Rounded.Subscriptions
    "payments" -> Icons.Rounded.Payments
    "work" -> Icons.Rounded.Work
    "trending_up" -> Icons.Rounded.TrendingUp
    else -> Icons.Rounded.Category
}
