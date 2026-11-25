package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Normalized identity snapshot for a Firebase-authenticated user session.
 */
data class AuthenticatedAccount(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null
)

/**
 * Reactive session stream events emitted by [AuthenticationProvider.observeAuthState].
 */
sealed interface AuthSessionEvent {
    data object Unauthenticated : AuthSessionEvent
    data class Authenticated(val account: AuthenticatedAccount) : AuthSessionEvent
}

/**
 * Outcome of an interactive or silent authentication attempt.
 */
sealed interface AuthActionResult {
    data class Success(
        val account: AuthenticatedAccount,
        val isNewUser: Boolean
    ) : AuthActionResult

    data object Cancelled : AuthActionResult
    data class Error(val message: String) : AuthActionResult
}

private const val AUTH_TAG = "Auth"

/**
 * Core Firebase Authentication provider for WalX.
 *
 * Manages user sessions, silent auto sign-in for returning users, interactive Google Sign-In /
 * Sign-Up via Jetpack Credential Manager, and leak-free session teardown on sign-out.
 *
 * Resolves the Web Client ID strictly from [R.string.default_web_client_id], which is
 * auto-generated at build time by the Google Services Gradle plugin from `app/google-services.json`.
 */
class AuthenticationProvider(
    private val firebaseAuth: FirebaseAuth = Firebase.auth
) {
    /**
     * Current signed-in Firebase account, or null if unauthenticated.
     */
    val currentAccount: AuthenticatedAccount?
        get() = firebaseAuth.currentUser?.toAuthenticatedAccount()

    /**
     * Returns the verified Firebase UID for the current session, or throws if unauthenticated.
     */
    fun requireUserId(): String =
        firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to access WalX data.")

    /**
     * Cold flow observing Firebase Auth session transitions across login, account switch,
     * token refresh, and sign-out.
     */
    fun observeAuthState(): Flow<AuthSessionEvent> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { state ->
            val user = state.currentUser
            trySend(
                if (user != null) {
                    AuthSessionEvent.Authenticated(user.toAuthenticatedAccount())
                } else {
                    AuthSessionEvent.Unauthenticated
                }
            )
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }.distinctUntilChanged()

    /**
     * Attempts silent returning-user sign-in using previously authorized Google credentials.
     *
     * Uses a single [GetGoogleIdOption] per [GetCredentialRequest] for reliable system sheet
     * handling. Returns [AuthActionResult.Cancelled] without surfacing an error when no prior
     * authorized credential exists on the device.
     */
    suspend fun attemptAutoSignIn(activityContext: Context): AuthActionResult {
        val existing = currentAccount
        if (existing != null) {
            return AuthActionResult.Success(account = existing, isNewUser = false)
        }

        val activity = activityContext.findActivity() ?: return AuthActionResult.Cancelled
        val webClientId = resolveWebClientId(activity) ?: return AuthActionResult.Cancelled

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return executeCredentialSignIn(
            activity = activity,
            request = request,
            suppressNoCredentialError = true
        )
    }

    /**
     * Launches the interactive Google Sign-In / Sign-Up flow via Jetpack Credential Manager.
     *
     * Uses a single [GetSignInWithGoogleOption] per [GetCredentialRequest] so new and returning
     * users can select or create their Google-backed WalX session reliably.
     */
    suspend fun signInWithGoogle(activityContext: Context): AuthActionResult {
        val activity = activityContext.findActivity()
            ?: return AuthActionResult.Error(
                "Unable to launch Google Sign-In from the current screen. Please reopen WalX and try again."
            )

        val webClientId = resolveWebClientId(activity)
            ?: return AuthActionResult.Error(
                "Google Sign-In configuration missing: default_web_client_id was not generated from google-services.json."
            )

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return executeCredentialSignIn(
            activity = activity,
            request = request,
            suppressNoCredentialError = false
        )
    }

    /**
     * Atomically clears Jetpack Credential Manager state and signs out of Firebase Auth.
     *
     * Runs inside [NonCancellable] so rapid UI navigation or scope cancellation never leaves a
     * stale credential or half-cleared session behind.
     */
    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Credential state clear skipped during sign-out", e)
            } finally {
                firebaseAuth.signOut()
            }
        }
    }

    private suspend fun executeCredentialSignIn(
        activity: Activity,
        request: GetCredentialRequest,
        suppressNoCredentialError: Boolean
    ): AuthActionResult {
        return try {
            val credentialManager = CredentialManager.create(activity)
            val response = credentialManager.getCredential(
                context = activity,
                request = request
            )
            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Log.w(AUTH_TAG, "Unexpected credential type returned: ${credential.type}")
                return AuthActionResult.Error("Unsupported sign-in credential received. Please try again.")
            }

            val googleIdCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdCredential.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user
                ?: return AuthActionResult.Error("Sign-in succeeded, but no user profile was returned.")

            val isNewUser = authResult.additionalUserInfo?.isNewUser == true
            AuthActionResult.Success(
                account = firebaseUser.toAuthenticatedAccount(),
                isNewUser = isNewUser
            )
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            AuthActionResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(AUTH_TAG, "No Google credential available on device: ${e.message}", e)
            if (suppressNoCredentialError) {
                AuthActionResult.Cancelled
            } else {
                AuthActionResult.Error(
                    "No Google account is available on this device. Please add a Google account in device settings."
                )
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(AUTH_TAG, "Failed to parse Google ID token", e)
            AuthActionResult.Error("Could not verify Google account token. Please try again.")
        } catch (e: FirebaseAuthException) {
            Log.e(AUTH_TAG, "Firebase Authentication error (${e.errorCode})", e)
            AuthActionResult.Error(mapFirebaseAuthError(e))
        } catch (e: GetCredentialException) {
            Log.e(AUTH_TAG, "Credential Manager error (${e.type}): ${e.errorMessage}", e)
            val detail = e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
            AuthActionResult.Error(
                detail ?: "Google Sign-In could not complete right now. Check your connection and try again."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Unexpected error during Google Sign-In", e)
            AuthActionResult.Error(e.localizedMessage ?: "Sign-in failed unexpectedly. Please try again.")
        }
    }

    private fun resolveWebClientId(context: Context): String? {
        return runCatching {
            context.getString(R.string.default_web_client_id).trim()
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun mapFirebaseAuthError(e: FirebaseAuthException): String = when (e.errorCode) {
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
            "An account already exists with a different sign-in method."
        "ERROR_USER_DISABLED" ->
            "This account has been disabled. Contact support for assistance."
        "ERROR_INVALID_CREDENTIAL" ->
            "Your Google sign-in token expired or is invalid. Please try again."
        "ERROR_NETWORK_REQUEST_FAILED" ->
            "Network error while contacting authentication servers. Check your internet connection."
        "ERROR_TOO_MANY_REQUESTS" ->
            "Too many sign-in attempts. Please wait a moment and try again."
        else ->
            "Authentication failed (${e.errorCode}). Please try again."
    }

    private fun FirebaseUser.toAuthenticatedAccount(): AuthenticatedAccount {
        val cleanEmail = email?.trim().orEmpty()
        val fallbackName = cleanEmail.substringBefore("@").ifBlank { "WalX Member" }
        val cleanName = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: fallbackName
        return AuthenticatedAccount(
            uid = uid,
            displayName = cleanName,
            email = cleanEmail,
            photoUrl = photoUrl?.toString()
        )
    }
}
