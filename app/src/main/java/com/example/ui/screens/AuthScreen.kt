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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.CurrencyCode
import com.example.domain.PasswordHasher
import com.example.ui.components.StatusPill
import com.example.ui.components.WalXBrandLockup
import com.example.ui.components.WalXCard
import com.example.ui.theme.LocalWalXColors

@Composable
fun AuthScreen(
    hasRegisteredAccounts: Boolean,
    authError: String?,
    authBusy: Boolean,
    securityError: String?,
    onClearError: () -> Unit,
    onClearSecurityError: () -> Unit,
    onLogIn: (email: String, password: String, rememberMe: Boolean) -> Unit,
    onSignUp: (
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        resetPin: String,
        currency: CurrencyCode,
        startingBalance: Double
    ) -> Unit,
    onResetPassword: (
        email: String,
        pin: String,
        newPass: String,
        confirmNewPass: String,
        onSuccess: () -> Unit
    ) -> Unit
) {
    val extended = LocalWalXColors.current
    val focusManager = LocalFocusManager.current

    // Default to Create Account when no local account exists yet; otherwise Log In
    var isSignUpTab by rememberSaveable(hasRegisteredAccounts) {
        mutableStateOf(!hasRegisteredAccounts)
    }

    var emailInput by rememberSaveable { mutableStateOf("") }

    // Login state
    var loginPassword by remember { mutableStateOf("") }
    var loginShowPass by remember { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(true) }

    // Sign-up state
    var fullName by rememberSaveable { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var resetPin by remember { mutableStateOf("") }
    var signUpShowPass by remember { mutableStateOf(false) }
    var startingCurrency by remember { mutableStateOf(CurrencyCode.INR) }
    var startingBalanceText by rememberSaveable { mutableStateOf("") }

    var localError by remember { mutableStateOf<String?>(null) }
    val displayError = localError ?: authError

    var showResetDialog by remember { mutableStateOf(false) }

    fun submitLogin() {
        focusManager.clearFocus()
        val cleanEmail = emailInput.trim()
        if (cleanEmail.isEmpty()) {
            localError = "Please enter your email address."
            return
        }
        if (loginPassword.isEmpty()) {
            localError = "Please enter your password."
            return
        }
        localError = null
        onLogIn(cleanEmail, loginPassword, rememberMe)
    }

    fun submitSignUp() {
        focusManager.clearFocus()
        val cleanName = fullName.trim()
        val cleanEmail = emailInput.trim()
        val cleanPin = resetPin.trim()
        if (cleanName.length < 2) {
            localError = "Please enter your full name (at least 2 characters)."
            return
        }
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            localError = "Please enter a valid email address."
            return
        }
        if (signUpPassword.length < 6) {
            localError = "Password must be at least 6 characters."
            return
        }
        if (signUpPassword != confirmPassword) {
            localError = "Passwords do not match."
            return
        }
        if (cleanPin.length < 4) {
            localError = "Recovery PIN must be at least 4 characters (used for Forgot Password)."
            return
        }
        val startingBal = if (startingBalanceText.isBlank()) {
            0.0
        } else {
            startingBalanceText.trim().replace(",", "").toDoubleOrNull()?.coerceAtLeast(0.0)
                ?: run {
                    localError = "Opening balance must be a valid number."
                    return
                }
        }
        localError = null
        onSignUp(
            cleanName,
            cleanEmail,
            signUpPassword,
            confirmPassword,
            cleanPin,
            startingCurrency,
            startingBal
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        extended.canvas,
                        extended.accentContainer.copy(alpha = 0.25f),
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

            Spacer(modifier = Modifier.height(20.dp))

            WalXCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = extended.accent.copy(alpha = 0.28f),
                contentPadding = 18.dp
            ) {
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
                            .padding(bottom = 12.dp)
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
                    if (!hasRegisteredAccounts) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = extended.accentContainer.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "New to WalX? Switch to Create Account to set up your personal finance vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            localError = null
                            onClearError()
                        },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = {
                            loginPassword = it
                            localError = null
                            onClearError()
                        },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { loginShowPass = !loginShowPass }) {
                                Icon(
                                    if (loginShowPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (loginShowPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitLogin() }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it }
                            )
                            Text("Remember me", style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(
                            onClick = {
                                onClearSecurityError()
                                showResetDialog = true
                            },
                            modifier = Modifier.testTag("forgot_password_button")
                        ) {
                            Text("Forgot Password?", color = extended.accent)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { submitLogin() },
                        enabled = !authBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (authBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Log In to WalX", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; localError = null; onClearError() },
                        label = { Text("Full Name") },
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
                        value = emailInput,
                        onValueChange = { emailInput = it; localError = null; onClearError() },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_email_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = signUpPassword,
                        onValueChange = { signUpPassword = it; localError = null; onClearError() },
                        label = { Text("Password (min 6 characters)") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { signUpShowPass = !signUpShowPass }) {
                                Icon(
                                    if (signUpShowPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (signUpShowPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_password_input")
                    )

                    if (signUpPassword.isNotEmpty()) {
                        PasswordStrengthMeter(password = signUpPassword)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val passwordsMatch = confirmPassword.isNotEmpty() && signUpPassword == confirmPassword
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; localError = null; onClearError() },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = if (passwordsMatch) {
                            {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Passwords match",
                                    tint = extended.income
                                )
                            }
                        } else null,
                        isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                        visualTransformation = if (signUpShowPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_confirm_password_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resetPin,
                        onValueChange = { resetPin = it; localError = null; onClearError() },
                        label = { Text("Recovery PIN (4+ chars for Forgot Password)") },
                        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_pin_input")
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
                            onDone = { submitSignUp() }
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

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { submitSignUp() },
                        enabled = !authBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("signup_submit_button")
                    ) {
                        if (authBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create WalX Account", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = extended.textMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Salted PBKDF2 Auth • Recovery PIN • Isolated Vaults",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showResetDialog) {
        ForgotPasswordDialog(
            initialEmail = emailInput,
            lockEmail = false,
            busy = authBusy,
            error = securityError,
            onClearError = onClearSecurityError,
            onDismiss = {
                showResetDialog = false
                onClearSecurityError()
            },
            onReset = { email, pin, newPass, confirmNewPass ->
                onResetPassword(email, pin, newPass, confirmNewPass) {
                    emailInput = email.trim()
                    loginPassword = ""
                    showResetDialog = false
                }
            }
        )
    }
}

@Composable
internal fun PasswordStrengthMeter(password: String) {
    val extended = LocalWalXColors.current
    val strength = PasswordHasher.evaluatePasswordStrength(password)
    val strengthLabel = when (strength) {
        1 -> "Weak"
        2 -> "Fair"
        3 -> "Good"
        else -> "Strong"
    }
    val strengthColor = when (strength) {
        1 -> extended.expense
        2 -> extended.warning
        3 -> extended.transfer
        else -> extended.income
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (bar in 1..4) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (bar <= strength) strengthColor else extended.border)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = strengthLabel,
            style = MaterialTheme.typography.labelSmall,
            color = strengthColor
        )
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

@Composable
internal fun ForgotPasswordDialog(
    initialEmail: String,
    lockEmail: Boolean,
    busy: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onReset: (email: String, pin: String, newPass: String, confirmNewPass: String) -> Unit
) {
    val extended = LocalWalXColors.current
    var email by remember { mutableStateOf(initialEmail) }
    var pin by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmNewPass by remember { mutableStateOf("") }
    var showPasswords by remember { mutableStateOf(false) }
    var localErr by remember { mutableStateOf<String?>(null) }
    val shownError = localErr ?: error

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forgot Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter your account email and the Recovery PIN you set during registration to create a new password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted
                )
                if (shownError != null) {
                    Text(
                        text = shownError,
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.expense,
                        fontWeight = FontWeight.Medium
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; localErr = null; onClearError() },
                    label = { Text("Account Email") },
                    enabled = !lockEmail,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_email_input")
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; localErr = null; onClearError() },
                    label = { Text("Recovery PIN") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_pin_input")
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it; localErr = null; onClearError() },
                    label = { Text("New Password (min 6 chars)") },
                    trailingIcon = {
                        IconButton(onClick = { showPasswords = !showPasswords }) {
                            Icon(
                                if (showPasswords) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_new_password_input")
                )
                if (newPass.isNotEmpty()) {
                    PasswordStrengthMeter(password = newPass)
                }
                OutlinedTextField(
                    value = confirmNewPass,
                    onValueChange = { confirmNewPass = it; localErr = null; onClearError() },
                    label = { Text("Confirm New Password") },
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_confirm_password_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPass.length < 6) {
                        localErr = "New password must be at least 6 characters."
                        return@Button
                    }
                    if (newPass != confirmNewPass) {
                        localErr = "New passwords do not match."
                        return@Button
                    }
                    onReset(email, pin, newPass, confirmNewPass)
                },
                enabled = !busy,
                modifier = Modifier.testTag("forgot_submit_button")
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Reset Password")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
