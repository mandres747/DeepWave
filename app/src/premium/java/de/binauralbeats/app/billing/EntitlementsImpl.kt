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
 * One-time, non-consumable product: bought once, owned forever, on every
 * device of the same Play account. There is no server to check against and no
 * account in the app, so "does this user own it" is answered by asking Play
 * on every launch and caching the answer in memory only. A user offline at
 * launch keeps whatever the last successful query said for that process; the
 * next successful query corrects it.
 */
object EntitlementsImpl : Entitlements {

    private val owned = MutableStateFlow(false)
    override val rhythmLayerOwned: StateFlow<Boolean> = owned.asStateFlow()

    private val price = MutableStateFlow<String?>(null)
    override val rhythmLayerPrice: StateFlow<String?> = price.asStateFlow()

    private var client: BillingClient? = null
    private var productDetails: ProductDetails? = null
    private var pendingResult: ((PurchaseResult) -> Unit)? = null

    private val purchasesUpdated = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
                deliver(if (owned.value) PurchaseResult.OWNED else PurchaseResult.ERROR)
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
                    queryProduct()
                    refresh()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Left alone on purpose: the next connect() reconnects. Retrying
                // in a loop here would spin while the device is offline.
            }
        })
    }

    private fun queryProduct() {
        val billing = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(Entitlements.PRODUCT_RHYTHM_LAYER)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        // Billing 8 hands back a result object rather than a bare list; the
        // products it could not fetch are reported separately and ignored here.
        billing.queryProductDetailsAsync(params) { result, productDetailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val found = productDetailsResult.productDetailsList
                .firstOrNull { it.productId == Entitlements.PRODUCT_RHYTHM_LAYER }
            productDetails = found
            price.value = found?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    override fun refresh() {
        val billing = client ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billing.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val hasIt = purchases.any {
                it.products.contains(Entitlements.PRODUCT_RHYTHM_LAYER) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            owned.value = hasIt
            purchases.forEach { handlePurchase(it) }
        }
    }

    override fun purchaseRhythmLayer(activity: Activity, onResult: (PurchaseResult) -> Unit) {
        val billing = client
        val details = productDetails
        if (billing == null || !billing.isReady || details == null) {
            onResult(PurchaseResult.UNAVAILABLE)
            return
        }
        pendingResult = onResult
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
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
        if (!purchase.products.contains(Entitlements.PRODUCT_RHYTHM_LAYER)) return
        owned.value = true
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
    }

    override fun release() {
        client?.endConnection()
        client = null
        productDetails = null
    }
}
