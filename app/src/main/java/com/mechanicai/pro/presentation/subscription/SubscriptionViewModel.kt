package com.mechanicai.pro.presentation.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.mechanicai.pro.data.billing.SubscriptionManager
import com.mechanicai.pro.data.billing.SubscriptionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptions: SubscriptionManager,
) : ViewModel() {
    val state: StateFlow<SubscriptionState> = subscriptions.state

    fun subscribe(activity: Activity) = subscriptions.launchPurchase(activity)

    fun restorePurchases() = subscriptions.restorePurchases()
}
