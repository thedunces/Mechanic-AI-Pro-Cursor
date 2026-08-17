package com.mechanicai.pro.presentation.settings

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleCredentialClient: GoogleCredentialClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    isAnonymous = user?.isAnonymous ?: true,
                    email = user?.email,
                    isDeleted = user == null && _uiState.value.deleteInProgress
                )
            }
        }
        viewModelScope.launch {
            val user = authRepository.currentUser.first()
            _uiState.value = _uiState.value.copy(
                isAnonymous = user?.isAnonymous ?: true,
                email = user?.email
            )
        }
    }

    fun linkWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            runCatching { googleCredentialClient.requestIdToken(activity) }
                .fold(
                    onSuccess = { token ->
                        authRepository.linkWithGoogle(token).fold(
                            onSuccess = { user ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isAnonymous = user.isAnonymous,
                                    email = user.email,
                                    successMessage = "Google account linked. Your data and subscription can now be restored."
                                )
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "Failed to link Google account."
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Google Sign-In was canceled."
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
        if (!Patterns.EMAIL_ADDRESS.matcher(state.linkEmail).matches() ||
            state.linkPassword.length < 8
        ) {
            _uiState.value = state.copy(
                errorMessage = "Enter a valid email and a password of at least 8 characters."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null, successMessage = null)
            authRepository.linkWithEmail(state.linkEmail, state.linkPassword).fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAnonymous = user.isAnonymous,
                        email = user.email,
                        linkPassword = "",
                        successMessage = "Account linked. Your data and subscription can now be restored."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to link email account."
                    )
                }
            )
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                deleteInProgress = true,
                errorMessage = null,
                successMessage = null
            )
            authRepository.deleteAccount().fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isDeleted = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        deleteInProgress = false,
                        errorMessage = error.message ?: "Failed to delete account."
                    )
                }
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
        val deleteInProgress: Boolean = false,
        val isDeleted: Boolean = false
    )
}
