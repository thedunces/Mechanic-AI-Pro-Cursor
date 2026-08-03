package com.mechanicai.pro.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.mechanicai.pro.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Firebase Authentication state.
 * Phase 1: anonymous auth + stubs for Google/email upgrade.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {

    private val _currentUser = MutableStateFlow(auth.currentUser?.toUser())
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    val isLoggedIn: Flow<Boolean> = currentUser.map { it != null }

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser?.toUser()
        }
    }

    /**
     * Signs in anonymously. Creates a new Firebase anonymous account if needed.
     */
    suspend fun signInAnonymously(): Result<User> = runCatching {
        val result = auth.signInAnonymously().await()
        val user = result.user?.toUser()
        requireNotNull(user) { "Anonymous sign-in succeeded but user is null" }
    }

    /**
     * Links the anonymous account to a Google credential.
     * Stub for Phase 5 production hardening.
     */
    suspend fun linkWithGoogle(idToken: String): Result<User> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        linkWithCredential(credential)
    }

    /**
     * Links the anonymous account to an email/password credential.
     * Stub for Phase 5 production hardening.
     */
    suspend fun linkWithEmail(email: String, password: String): Result<User> = runCatching {
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
        linkWithCredential(credential)
    }

    private suspend fun linkWithCredential(credential: AuthCredential): User {
        val currentUser = auth.currentUser
        requireNotNull(currentUser) { "No current user to link" }
        val result = currentUser.linkWithCredential(credential).await()
        return result.user?.toUser()
            ?: throw IllegalStateException("Link succeeded but user is null")
    }

    /**
     * Signs the current user out.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Deletes the current user account and all associated data.
     * Should be called with a re-authentication step in production.
     */
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        auth.currentUser?.delete()?.await()
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
