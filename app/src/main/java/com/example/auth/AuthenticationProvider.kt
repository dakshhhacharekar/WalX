package com.example.auth

import com.example.data.AuthResult
import com.example.data.WalXRepository
import com.example.domain.CurrencyCode
import com.example.domain.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive session state managed by [AuthenticationProvider].
 */
sealed interface AuthSessionEvent {
    data object Checking : AuthSessionEvent
    data object Unauthenticated : AuthSessionEvent
    data class Authenticated(val user: UserProfile) : AuthSessionEvent
}

/**
 * Core Email & Password Authentication provider for WalX.
 *
 * Uses salted PBKDF2-HMAC-SHA256 credential hashing, Recovery PIN password resets,
 * per-user isolated vaults, reactive loading state ([isLoading]), and clean session termination.
 */
class AuthenticationProvider(
    private val repository: WalXRepository
) {
    private val _isLoading = MutableStateFlow(false)

    /**
     * Reactive loading indicator emitted while login, sign-up, password reset, or sign-out
     * operations are executing so the UI shows visual feedback and blocks duplicate requests.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authState = MutableStateFlow<AuthSessionEvent>(AuthSessionEvent.Checking)

    /**
     * Reactive session stream driving authenticated vs unauthenticated UI routing.
     */
    val authState: StateFlow<AuthSessionEvent> = _authState.asStateFlow()

    val currentUser: UserProfile?
        get() = (_authState.value as? AuthSessionEvent.Authenticated)?.user

    fun requireUserId(): String =
        currentUser?.id ?: throw IllegalStateException("User must be signed in to access WalX data.")

    /**
     * Initializes the session on app launch, purging any legacy demo records and restoring a
     * remembered user session if present.
     */
    suspend fun restoreSession(): Boolean {
        _isLoading.value = true
        return try {
            repository.purgeLegacyDemoAccountIfPresent()
            val hasAccounts = repository.hasRegisteredAccounts()
            val remembered = repository.getRememberedUser()
            if (remembered != null) {
                _authState.value = AuthSessionEvent.Authenticated(remembered)
            } else {
                _authState.value = AuthSessionEvent.Unauthenticated
            }
            hasAccounts
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun signUp(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        resetPin: String,
        currency: CurrencyCode,
        startingBalance: Double = 0.0
    ): AuthResult {
        if (password != confirmPassword) {
            return AuthResult.Error("Passwords do not match.")
        }
        if (!_isLoading.compareAndSet(expect = false, update = true)) {
            return AuthResult.Error("Another authentication request is already in progress.")
        }
        return try {
            val result = repository.signUp(
                fullName = fullName,
                email = email,
                password = password,
                resetPin = resetPin,
                currency = currency,
                startingBalance = startingBalance
            )
            if (result is AuthResult.Success) {
                _authState.value = AuthSessionEvent.Authenticated(result.user)
            }
            result
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun logIn(email: String, password: String, rememberMe: Boolean): AuthResult {
        if (!_isLoading.compareAndSet(expect = false, update = true)) {
            return AuthResult.Error("Another authentication request is already in progress.")
        }
        return try {
            val result = repository.logIn(email = email, password = password, rememberMe = rememberMe)
            if (result is AuthResult.Success) {
                _authState.value = AuthSessionEvent.Authenticated(result.user)
            }
            result
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun resetPasswordWithPin(
        email: String,
        resetPin: String,
        newPassword: String,
        confirmNewPassword: String
    ): AuthResult {
        if (newPassword != confirmNewPassword) {
            return AuthResult.Error("New passwords do not match.")
        }
        if (!_isLoading.compareAndSet(expect = false, update = true)) {
            return AuthResult.Error("Another security request is already in progress.")
        }
        return try {
            repository.resetPasswordWithPin(
                email = email,
                resetPin = resetPin,
                newPassword = newPassword
            )
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String
    ): AuthResult {
        if (newPassword != confirmNewPassword) {
            return AuthResult.Error("New passwords do not match.")
        }
        if (!_isLoading.compareAndSet(expect = false, update = true)) {
            return AuthResult.Error("Another security request is already in progress.")
        }
        return try {
            repository.changePassword(
                userId = userId,
                currentPassword = currentPassword,
                newPassword = newPassword
            )
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun updateRecoveryPin(
        userId: String,
        currentPassword: String,
        newPin: String
    ): AuthResult {
        if (!_isLoading.compareAndSet(expect = false, update = true)) {
            return AuthResult.Error("Another security request is already in progress.")
        }
        return try {
            repository.updateRecoveryPin(
                userId = userId,
                currentPassword = currentPassword,
                newPin = newPin
            )
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Clears the remembered session marker and terminates the active authentication state cleanly.
     */
    suspend fun signOut() {
        _isLoading.value = true
        try {
            repository.clearLocalSessionMarker()
            _authState.value = AuthSessionEvent.Unauthenticated
        } finally {
            _isLoading.value = false
        }
    }
}
