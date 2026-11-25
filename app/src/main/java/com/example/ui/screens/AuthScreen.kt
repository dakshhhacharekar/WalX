package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.CurrencyCode
import com.example.ui.components.StatusPill
import com.example.ui.components.WalXBrandLockup
import com.example.ui.components.WalXCard
import com.example.ui.theme.LocalWalXColors

@Composable
fun AuthScreen(
    authError: String?,
    authBusy: Boolean,
    onClearError: () -> Unit,
    onAttemptAutoSignIn: () -> Unit,
    onContinueWithGoogle: (
        preferredDisplayName: String?,
        startingCurrency: CurrencyCode,
        startingBalance: Double
    ) -> Unit
) {
    val extended = LocalWalXColors.current
    val focusManager = LocalFocusManager.current

    var isSignUpTab by rememberSaveable { mutableStateOf(false) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var startingCurrency by remember { mutableStateOf(CurrencyCode.INR) }
    var startingBalanceText by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val displayError = localError ?: authError

    LaunchedEffect(Unit) {
        onAttemptAutoSignIn()
    }

    fun launchSignUpWithGoogle() {
        focusManager.clearFocus()
        val parsedBalance = if (startingBalanceText.isBlank()) {
            0.0
        } else {
            startingBalanceText.trim().replace(",", "").toDoubleOrNull()?.coerceAtLeast(0.0)
                ?: run {
                    localError = "Opening balance must be a valid positive number."
                    return
                }
        }
        localError = null
        onClearError()
        onContinueWithGoogle(
            displayName.trim().takeIf { it.length >= 2 },
            startingCurrency,
            parsedBalance
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        extended.canvas,
                        extended.accentContainer.copy(alpha = 0.28f),
                        extended.canvas
                    )
                )
            )
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WalXBrandLockup(compact = false)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Personal Finance Command Center",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                StatusPill("Multi-Wallet", extended.accentContainer, extended.accent)
                StatusPill("AI Copilot", extended.incomeContainer, extended.income)
                StatusPill("Isolated Vaults", extended.transferContainer, extended.transfer)
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Obsidian Firebase Auth Card
            WalXCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = extended.accent.copy(alpha = 0.30f),
                contentPadding = 18.dp
            ) {
                // Segmented Auth Tab Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(extended.cardElevated, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                ) {
                    AuthTabButton(
                        label = "Log In",
                        selected = !isSignUpTab,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_tab_login"),
                        onClick = {
                            isSignUpTab = false
                            localError = null
                            onClearError()
                        }
                    )
                    AuthTabButton(
                        label = "Create Account",
                        selected = isSignUpTab,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_tab_signup"),
                        onClick = {
                            isSignUpTab = true
                            localError = null
                            onClearError()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (displayError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = extended.expenseContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = extended.expense,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayError,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.expense,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (!isSignUpTab) {
                    // LOG IN WITH GOOGLE (FIREBASE AUTH)
                    Text(
                        text = "Welcome back to WalX",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sign in with your Google account to unlock your isolated multi-wallet ledger, budgets, and AI Copilot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AuthFeatureRow(
                        icon = Icons.Rounded.VerifiedUser,
                        title = "Verified Firebase Session",
                        subtitle = "One-tap Google Identity via Android Credential Manager.",
                        tint = extended.accent
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AuthFeatureRow(
                        icon = Icons.Rounded.Lock,
                        title = "Strict Per-User Sandboxing",
                        subtitle = "Wallets, transactions, budgets, and vaults are isolated by UID.",
                        tint = extended.income
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AuthFeatureRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "WalX AI Financial Copilot",
                        subtitle = "Natural-language quick-log and live spending analysis.",
                        tint = extended.transfer
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            localError = null
                            onClearError()
                            onContinueWithGoogle(null, CurrencyCode.INR, 0.0)
                        },
                        enabled = !authBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_signin_button")
                    ) {
                        if (authBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Signing in...",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Login,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // NEW ACCOUNT SETUP WITH GOOGLE (FIREBASE AUTH)
                    Text(
                        text = "Configure your new WalX vault",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize your starting preferences below, then link your Google account to initialize your private workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it; localError = null; onClearError() },
                        label = { Text("Display Name (Optional)") },
                        placeholder = { Text("Defaults to your Google profile name") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = startingBalanceText,
                        onValueChange = { startingBalanceText = it; localError = null; onClearError() },
                        label = { Text("Primary Bank Opening Balance (Optional)") },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { launchSignUpWithGoogle() }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_opening_balance_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Starting Currency",
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
                        CurrencyCode.entries.forEach { cur ->
                            FilterChip(
                                selected = startingCurrency == cur,
                                onClick = { startingCurrency = cur },
                                label = { Text(cur.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { launchSignUpWithGoogle() },
                        enabled = !authBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_signup_button")
                    ) {
                        if (authBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Creating account...",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Create Account with Google",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = extended.textMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Firebase Auth • Google Identity • Isolated Per-User Vaults",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color
) {
    val extended = LocalWalXColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(extended.cardElevated, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = tint.copy(alpha = 0.16f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = extended.textMuted
            )
        }
    }
}

@Composable
private fun AuthTabButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val extended = LocalWalXColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) extended.accent else Color.Transparent,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else extended.textMuted
            )
        }
    }
}
