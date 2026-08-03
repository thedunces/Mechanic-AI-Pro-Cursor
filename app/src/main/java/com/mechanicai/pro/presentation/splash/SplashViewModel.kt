package com.mechanicai.pro.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mechanicai.pro.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authRepository.signInAnonymously()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAuthenticated = result.isSuccess,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    data class SplashUiState(
        val isLoading: Boolean = true,
        val isAuthenticated: Boolean = false,
        val errorMessage: String? = null
    )
}
