package com.mechanicai.pro.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mechanicai.pro.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.currentUser.first()
            _uiState.value = _uiState.value.copy(
                isAnonymous = user?.isAnonymous ?: true,
                email = user?.email
            )
        }
    }

    /**
     * Stub for linking an anonymous account to Google Sign-In.
     * In production, launch the Google Sign-In flow and pass the idToken to [AuthRepository.linkWithGoogle].
     */
    fun linkWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.linkWithGoogle(idToken)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isAnonymous = user.isAnonymous,
                        email = user.email,
                        successMessage = "Account linked successfully"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to link account"
                    )
                }
            )
        }
    }

    fun updateLinkEmail(value: String) {
        _uiState.value = _uiState.value.copy(linkEmail = value.trim())
    }

    fun updateLinkPassword(value: String) {
        _uiState.value = _uiState.value.copy(linkPassword = value)
    }

    fun linkWithEmail() {
        val state = _uiState.value
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.linkEmail).matches() ||
            state.linkPassword.length < 8
        ) {
            _uiState.value = state.copy(
                errorMessage = "Enter a valid email and a password of at least 8 characters.",
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            authRepository.linkWithEmail(state.linkEmail, state.linkPassword).fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAnonymous = user.isAnonymous,
                        email = user.email,
                        linkPassword = "",
                        successMessage = "Account linked. Your data and subscription can now be restored.",
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to link email account.",
                    )
                },
            )
        }
    }

    data class SettingsUiState(
        val isAnonymous: Boolean = true,
        val email: String? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val linkEmail: String = "",
        val linkPassword: String = "",
    )
}
