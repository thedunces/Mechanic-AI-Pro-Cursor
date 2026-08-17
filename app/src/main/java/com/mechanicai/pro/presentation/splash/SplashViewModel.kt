package com.mechanicai.pro.presentation.splash

import android.app.Activity
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mechanicai.pro.data.auth.GoogleCredentialClient
import com.mechanicai.pro.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleCredentialClient: GoogleCredentialClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        if (authRepository.hasPersistedUser()) {
            _uiState.value = SplashUiState(isLoading = false, isAuthenticated = true)
        }
    }

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value.trim())
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInAnonymously()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAuthenticated = result.isSuccess,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { googleCredentialClient.requestIdToken(activity) }
                .mapCatching { token ->
                    authRepository.signInWithGoogle(token).getOrThrow()
                }
                .fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Google sign-in failed."
                        )
                    }
                )
        }
    }

    fun signInWithEmail() {
        submitEmailAuth { email, password ->
            authRepository.signInWithEmail(email, password)
        }
    }

    fun createEmailAccount() {
        submitEmailAuth { email, password ->
            authRepository.createEmailAccount(email, password)
        }
    }

    private fun submitEmailAuth(action: suspend (String, String) -> Result<*>) {
        val state = _uiState.value
        if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches() || state.password.length < 8) {
            _uiState.value = state.copy(
                errorMessage = "Enter a valid email and a password of at least 8 characters."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val result = action(state.email, state.password)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAuthenticated = result.isSuccess,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    data class SplashUiState(
        val isLoading: Boolean = false,
        val isAuthenticated: Boolean = false,
        val errorMessage: String? = null,
        val email: String = "",
        val password: String = ""
    )
}
