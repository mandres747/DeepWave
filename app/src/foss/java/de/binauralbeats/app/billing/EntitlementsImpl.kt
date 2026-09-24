package de.binauralbeats.app.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * F-Droid build: no Play Billing, and no paid add-ons either - the rhythm
 * layer is not part of this build at all (FeatureFlags.rhythmLayerAvailable
 * is false), so nothing here is ever reached. It exists so the shared code
 * compiles without the proprietary billing library.
 */
object EntitlementsImpl : Entitlements {

    override val rhythmLayerOwned: StateFlow<Boolean> = MutableStateFlow(false)

    override val rhythmLayerPrice: StateFlow<String?> = MutableStateFlow(null)

    override fun connect(context: Context) = Unit

    override fun purchaseRhythmLayer(activity: Activity, onResult: (PurchaseResult) -> Unit) {
        onResult(PurchaseResult.UNAVAILABLE)
    }

    override val wakeAlarmOwned: StateFlow<Boolean> = MutableStateFlow(false)

    override val wakeAlarmPrice: StateFlow<String?> = MutableStateFlow(null)

    override fun purchaseWakeAlarm(activity: Activity, onResult: (PurchaseResult) -> Unit) {
        onResult(PurchaseResult.UNAVAILABLE)
    }

    override fun refresh() = Unit

    override fun release() = Unit
}
