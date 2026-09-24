package de.binauralbeats.app.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * F-Droid build: no Play Billing, and nothing to buy - Premium, the rhythm
 * layer and the wake alarm are not part of this build at all (their
 * FeatureFlags are false), so nothing here is ever reached. It exists so the
 * shared code compiles without the proprietary billing library.
 */
object EntitlementsImpl : Entitlements {

    override val owned: StateFlow<Set<String>> = MutableStateFlow(emptySet())

    override val prices: StateFlow<Map<String, String>> = MutableStateFlow(emptyMap())

    override fun connect(context: Context) = Unit

    override fun purchase(activity: Activity, productId: String, onResult: (PurchaseResult) -> Unit) {
        onResult(PurchaseResult.UNAVAILABLE)
    }

    override fun refresh() = Unit

    override fun release() = Unit
}
