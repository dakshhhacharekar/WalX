package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class WalXExtendedColors(
    val canvas: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val accent: Color,
    val accentContainer: Color,
    val textMuted: Color
)

val LocalWalXColors = staticCompositionLocalOf {
    WalXExtendedColors(
        canvas = ObsidianCanvas,
        card = ObsidianCard,
        cardElevated = ObsidianCardElevated,
        border = ObsidianBorder,
        income = WalXEmerald,
        incomeContainer = WalXEmeraldContainerDark,
        expense = WalXRose,
        expenseContainer = WalXRoseContainerDark,
        transfer = WalXSky,
        transferContainer = WalXSkyContainerDark,
        warning = WalXAmber,
        warningContainer = WalXAmberContainerDark,
        accent = WalXIndigo,
        accentContainer = WalXIndigoContainerDark,
        textMuted = ObsidianTextSecondary
    )
}

private val WalXDarkColorScheme = darkColorScheme(
    primary = WalXIndigo,
    onPrimary = Color.White,
    primaryContainer = WalXIndigoContainerDark,
    onPrimaryContainer = Color(0xFFC7D2FE),
    secondary = WalXEmerald,
    onSecondary = Color(0xFF032117),
    secondaryContainer = WalXEmeraldContainerDark,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = WalXSky,
    onTertiary = Color(0xFF042233),
    tertiaryContainer = WalXSkyContainerDark,
    onTertiaryContainer = Color(0xFFBAE6FD),
    error = WalXRose,
    onError = Color.White,
    errorContainer = WalXRoseContainerDark,
    onErrorContainer = Color(0xFFFECDD3),
    background = ObsidianCanvas,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianCard,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianCardElevated,
    onSurfaceVariant = ObsidianTextSecondary,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF283044)
)

private val WalXLightColorScheme = lightColorScheme(
    primary = WalXIndigoDark,
    onPrimary = Color.White,
    primaryContainer = WalXIndigoContainerLight,
    onPrimaryContainer = Color(0xFF312E81),
    secondary = WalXEmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = WalXEmeraldContainerLight,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = WalXSkyDark,
    onTertiary = Color.White,
    tertiaryContainer = WalXSkyContainerLight,
    onTertiaryContainer = Color(0xFF0C4A6E),
    error = WalXRoseDark,
    onError = Color.White,
    errorContainer = WalXRoseContainerLight,
    onErrorContainer = Color(0xFF881337),
    background = DaylightCanvas,
    onBackground = DaylightTextPrimary,
    surface = DaylightCard,
    onSurface = DaylightTextPrimary,
    surfaceVariant = DaylightCardElevated,
    onSurfaceVariant = DaylightTextSecondary,
    outline = DaylightBorder,
    outlineVariant = Color(0xFFCBD5E1)
)

private val WalXDarkExtended = WalXExtendedColors(
    canvas = ObsidianCanvas,
    card = ObsidianCard,
    cardElevated = ObsidianCardElevated,
    border = ObsidianBorder,
    income = WalXEmerald,
    incomeContainer = WalXEmeraldContainerDark,
    expense = WalXRose,
    expenseContainer = WalXRoseContainerDark,
    transfer = WalXSky,
    transferContainer = WalXSkyContainerDark,
    warning = WalXAmber,
    warningContainer = WalXAmberContainerDark,
    accent = WalXIndigo,
    accentContainer = WalXIndigoContainerDark,
    textMuted = ObsidianTextSecondary
)

private val WalXLightExtended = WalXExtendedColors(
    canvas = DaylightCanvas,
    card = DaylightCard,
    cardElevated = DaylightCardElevated,
    border = DaylightBorder,
    income = WalXEmeraldDark,
    incomeContainer = WalXEmeraldContainerLight,
    expense = WalXRoseDark,
    expenseContainer = WalXRoseContainerLight,
    transfer = WalXSkyDark,
    transferContainer = WalXSkyContainerLight,
    warning = WalXAmberDark,
    warningContainer = WalXAmberContainerLight,
    accent = WalXIndigoDark,
    accentContainer = WalXIndigoContainerLight,
    textMuted = DaylightTextSecondary
)

val WalXShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WalXTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WalXDarkColorScheme else WalXLightColorScheme
    val extended = if (darkTheme) WalXDarkExtended else WalXLightExtended

    CompositionLocalProvider(LocalWalXColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WalXTypography,
            shapes = WalXShapes,
            content = content
        )
    }
}
