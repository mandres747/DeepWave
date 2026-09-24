package de.binauralbeats.app.billing

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Play Billing for the premium build. Lives in the premium source set only -
 * the library is proprietary and would get the F-Droid build rejected.
 *
 * One-time, non-consumable products (premium, complete, rhythm_layer, wake_alarm): bought once,
 * owned forever, on every device of the same Play account. There is no server
 * to check against and no account in the app, so "does this user own it" is
 * answered by asking Play on every launch and caching the answer in memory
 * only. A user offline at launch keeps whatever the last successful query said
 * for that process; the next successful query corrects it.
 *
 * State is a set of owned product ids; what that set unlocks is decided in
 * Access, not here.
 */
object EntitlementsImpl : Entitlements {

    /** Every paid product this build sells. Queried and restored together. */
    private val products = Entitlements.ALL_PRODUCTS

    private val ownedState = MutableStateFlow<Set<String>>(emptySet())
    override val owned: StateFlow<Set<String>> = ownedState.asStateFlow()

    private val priceState = MutableStateFlow<Map<String, String>>(emptyMap())
    override val prices: StateFlow<Map<String, String>> = priceState.asStateFlow()

    private val details = mutableMapOf<String, ProductDetails>()

    private var client: BillingClient? = null
    private var pendingResult: ((PurchaseResult) -> Unit)? = null
    private var pendingProduct: String? = null

    private val purchasesUpdated = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
                val bought = pendingProduct?.let { it in ownedState.value } == true
                deliver(if (bought) PurchaseResult.OWNED else PurchaseResult.ERROR)
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                deliver(PurchaseResult.CANCELLED)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Can happen when a purchase completed elsewhere since the last
                // query. Ask Play again rather than guessing.
                refresh()
                deliver(PurchaseResult.OWNED)
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                deliver(PurchaseResult.UNAVAILABLE)
            else -> deliver(PurchaseResult.ERROR)
        }
    }

    override fun connect(context: Context) {
        if (client?.isReady == true) {
            refresh()
            return
        }
        val billing = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdated)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        client = billing
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    refresh()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Left alone on purpose: the next connect() reconnects. Retrying
                // in a loop here would spin while the device is offline.
            }
        })
    }

    private fun queryProducts() {
        val billing = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                products.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()
        // Billing 8 hands back a result object rather than a bare list; the
        // products it could not fetch are reported separately and ignored here.
        billing.queryProductDetailsAsync(params) { result, productDetailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            for (found in productDetailsResult.productDetailsList) {
                if (found.productId !in products) continue
                details[found.productId] = found
                found.oneTimePurchaseOfferDetails?.formattedPrice?.let { price ->
                    priceState.value = priceState.value + (found.productId to price)
                }
            }
        }
    }

    override fun refresh() {
        val billing = client ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billing.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            ownedState.value = purchases
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .flatMap { it.products }
                .filter { it in products }
                .toSet()
            purchases.forEach { handlePurchase(it) }
        }
    }

    override fun purchase(activity: Activity, productId: String, onResult: (PurchaseResult) -> Unit) {
        val billing = client
        val productDetails = details[productId]
        if (billing == null || !billing.isReady || productDetails == null) {
            onResult(PurchaseResult.UNAVAILABLE)
            return
        }
        pendingResult = onResult
        pendingProduct = productId
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()
        val launch = billing.launchBillingFlow(activity, params)
        if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
            deliver(PurchaseResult.ERROR)
        }
    }

    /**
     * Play keeps refunding an unacknowledged purchase after three days, so
     * acknowledging is not optional bookkeeping - skip it and paying users
     * silently lose what they bought.
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val ours = purchase.products.filter { it in products }
        if (ours.isEmpty()) return
        ownedState.value = ownedState.value + ours
        if (purchase.isAcknowledged) return
        val billing = client ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billing.acknowledgePurchase(params) { /* nothing to do either way */ }
    }

    private fun deliver(result: PurchaseResult) {
        pendingResult?.invoke(result)
        pendingResult = null
        pendingProduct = null
    }

    override fun release() {
        client?.endConnection()
        client = null
        details.clear()
    }
}
