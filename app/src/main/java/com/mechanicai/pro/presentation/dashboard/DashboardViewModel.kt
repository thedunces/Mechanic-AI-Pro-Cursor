package com.mechanicai.pro.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mechanicai.pro.data.billing.SubscriptionManager
import com.mechanicai.pro.data.billing.SubscriptionState
import com.mechanicai.pro.data.model.User
import com.mechanicai.pro.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    subscriptionManager: SubscriptionManager,
) : ViewModel() {

    val user: StateFlow<User?> = authRepository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val subscription: StateFlow<SubscriptionState> = subscriptionManager.state
}
