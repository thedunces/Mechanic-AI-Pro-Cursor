package com.mechanicai.pro.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.time.YearMonth
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

data class SubscriptionState(
    val tier: String = "free",
    val diagnosesUsed: Int = 0,
    val monthlyLimit: Int = 3,
    val price: String? = null,
    val isLoading: Boolean = true,
    val purchaseInProgress: Boolean = false,
    val errorMessage: String? = null,
    val requiresAccountUpgrade: Boolean = true,
) {
    val remaining: Int get() = (monthlyLimit - diagnosesUsed).coerceAtLeast(0)
    val isPro: Boolean get() = tier == "pro"
}

@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) : PurchasesUpdatedListener {

    companion object {
        const val PRO_PRODUCT_ID = "mechanic_ai_pro_monthly"
        private const val PRO_BASE_PLAN_ID = "monthly"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()
    private var productDetails: ProductDetails? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        connect()
        observeServerState()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    restorePurchases()
                } else {
                    setError(result.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                setError(result.debugMessage)
                return@queryProductDetailsAsync
            }
            productDetails = detailsResult.productDetailsList.firstOrNull()
            val offer = productDetails?.subscriptionOfferDetails
                ?.firstOrNull { it.basePlanId == PRO_BASE_PLAN_ID }
                ?: productDetails?.subscriptionOfferDetails?.firstOrNull()
            val price = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
            _state.value = _state.value.copy(price = price, isLoading = false)
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails
        val offer = details?.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == PRO_BASE_PLAN_ID }
            ?: details?.subscriptionOfferDetails?.firstOrNull()
        val uid = auth.currentUser?.uid
        if (auth.currentUser?.isAnonymous != false) {
            setError("Link an email or Google account before subscribing so purchases can be restored.")
            return
        }
        if (details == null || offer == null || uid == null) {
            setError("The Pro subscription is not available yet.")
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(sha256(uid))
            .build()
        _state.value = _state.value.copy(purchaseInProgress = true, errorMessage = null)
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(purchaseInProgress = false)
            setError(result.debugMessage)
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.filter { PRO_PRODUCT_ID in it.products }.forEach(::processPurchase)
            } else {
                setError(result.debugMessage)
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases.orEmpty().forEach(::processPurchase)
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            setError(result.debugMessage)
        } else {
            _state.value = _state.value.copy(purchaseInProgress = false)
        }
    }

    private fun processPurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED ||
            PRO_PRODUCT_ID !in purchase.products
        ) return

        scope.launch {
            runCatching {
                functions.getHttpsCallable("verifySubscription")
                    .call(
                        mapOf(
                            "purchaseToken" to purchase.purchaseToken,
                            "productId" to PRO_PRODUCT_ID,
                        ),
                    )
                    .await()
                if (!purchase.isAcknowledged) {
                    billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build(),
                    ) { result ->
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            setError("Purchase verified, but acknowledgement is pending.")
                        }
                    }
                }
            }.onSuccess {
                _state.value = _state.value.copy(purchaseInProgress = false, errorMessage = null)
            }.onFailure { error ->
                _state.value = _state.value.copy(purchaseInProgress = false)
                setError(error.message ?: "Purchase verification failed.")
            }
        }
    }

    private fun observeServerState() {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid ?: return@addAuthStateListener
            _state.value = _state.value.copy(
                requiresAccountUpgrade = firebaseAuth.currentUser?.isAnonymous != false,
            )
            firestore.document("users/$uid/entitlements/current")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        setError(error.message ?: "Could not load subscription.")
                        return@addSnapshotListener
                    }
                    _state.value = _state.value.copy(
                        tier = snapshot?.getString("tier") ?: "free",
                        monthlyLimit = snapshot?.getLong("monthlyLimit")?.toInt() ?: 3,
                        isLoading = false,
                    )
                }

            val period = YearMonth.now(ZoneOffset.UTC).toString()
            firestore.document("users/$uid/usage/$period")
                .addSnapshotListener { snapshot, _ ->
                    _state.value = _state.value.copy(
                        diagnosesUsed = snapshot?.getLong("diagnosesUsed")?.toInt() ?: 0,
                    )
                }
        }
    }

    private fun setError(message: String) {
        _state.value = _state.value.copy(
            isLoading = false,
            purchaseInProgress = false,
            errorMessage = message,
        )
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
