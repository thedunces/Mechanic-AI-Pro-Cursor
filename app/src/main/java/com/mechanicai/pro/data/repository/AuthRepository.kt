package com.mechanicai.pro.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import com.mechanicai.pro.data.model.User
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {

    private val _currentUser = MutableStateFlow(auth.currentUser?.toUser())
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    val isLoggedIn: Flow<Boolean> = currentUser.map { it != null }

    fun hasPersistedUser(): Boolean = auth.currentUser != null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser?.toUser()
        }
    }

    suspend fun signInAnonymously(): Result<User> = runCatching {
        val result = auth.signInAnonymously().await()
        val user = result.user?.toUser()
        requireNotNull(user) { "Anonymous sign-in succeeded but user is null" }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        result.user?.toUser() ?: error("Sign-in succeeded but user is null")
    }.recoverCatching { error ->
        throw friendlyAuthError(error, "Could not sign in with that email and password.")
    }

    suspend fun createEmailAccount(email: String, password: String): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        result.user?.toUser() ?: error("Account creation succeeded but user is null")
    }.recoverCatching { error ->
        throw friendlyAuthError(error, "Could not create that email account.")
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        result.user?.toUser() ?: error("Google sign-in succeeded but user is null")
    }.recoverCatching { error ->
        throw friendlyAuthError(error, "Google sign-in failed.")
    }

    suspend fun linkWithGoogle(idToken: String): Result<User> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        linkWithCredential(credential)
    }.recoverCatching { error ->
        throw friendlyAuthError(error, "Could not link Google account.")
    }

    suspend fun linkWithEmail(email: String, password: String): Result<User> = runCatching {
        val credential = EmailAuthProvider.getCredential(email.trim(), password)
        linkWithCredential(credential)
    }.recoverCatching { error ->
        throw friendlyAuthError(error, "Could not link email account.")
    }

    private suspend fun linkWithCredential(credential: AuthCredential): User {
        val currentUser = auth.currentUser
        requireNotNull(currentUser) { "No current user to link" }
        val result = currentUser.linkWithCredential(credential).await()
        return result.user?.toUser()
            ?: throw IllegalStateException("Link succeeded but user is null")
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        functions.getHttpsCallable("deleteAccount").call().await()
        auth.signOut()
    }.recoverCatching { error ->
        throw friendlyAuthError(error, "Could not delete the account. Try signing in again and retry.")
    }

    private fun friendlyAuthError(error: Throwable, fallback: String): IllegalStateException {
        val code = (error as? FirebaseAuthException)?.errorCode
        val message = when (code) {
            "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
                "That account is already in use. Sign in with it instead of linking."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_NOT_FOUND" ->
                "Email or password is incorrect."
            "ERROR_WEAK_PASSWORD" ->
                "Choose a password with at least 8 characters."
            "ERROR_INVALID_EMAIL" ->
                "Enter a valid email address."
            "ERROR_REQUIRES_RECENT_LOGIN" ->
                "For security, sign in again and then retry this action."
            else -> error.message ?: fallback
        }
        return IllegalStateException(message, error)
    }

    private fun FirebaseUser.toUser(): User {
        return User(
            id = uid,
            isAnonymous = isAnonymous,
            displayName = displayName,
            email = email,
            createdAt = metadata?.creationTimestamp?.let { Date(it) },
            lastLoginAt = metadata?.lastSignInTimestamp?.let { Date(it) }
        )
    }
}
