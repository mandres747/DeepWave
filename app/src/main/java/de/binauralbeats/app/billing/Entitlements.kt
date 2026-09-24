package de.binauralbeats.app.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/** What came back from a purchase attempt, so the UI can say the right thing. */
enum class PurchaseResult {
    /** The add-on is now owned - either just bought or already on the account. */
    OWNED,

    /** The user backed out. Not an error, and must not be reported as one. */
    CANCELLED,

    /** Billing is not usable here at all (no Play Store, unsupported device). */
    UNAVAILABLE,

    ERROR
}

/**
 * Runtime ownership of paid add-ons, kept apart from FeatureFlags on purpose:
 * FeatureFlags answers "does this build contain the feature", Entitlements
 * answers "may this user use it". The FOSS build answers no to the first
 * question and therefore never asks the second.
 *
 * The premium implementation talks to Play Billing; the FOSS one is a stub
 * with no dependency on it, which is what keeps the proprietary library out
 * of the F-Droid build. See docs/RHYTHMUS_LAYER_KONZEPT.md.
 */
interface Entitlements {

    /** True once the rhythm add-on is owned on this Play account. */
    val rhythmLayerOwned: StateFlow<Boolean>

    /** Localised price as Play reports it, or null while it is unknown. */
    val rhythmLayerPrice: StateFlow<String?>

    /** Opens the billing connection and picks up purchases made elsewhere. */
    fun connect(context: Context)

    fun purchaseRhythmLayer(activity: Activity, onResult: (PurchaseResult) -> Unit)

    /** True once the wake-alarm add-on is owned. See docs/KLANGWECKER_KONZEPT.md. */
    val wakeAlarmOwned: StateFlow<Boolean>

    val wakeAlarmPrice: StateFlow<String?>

    fun purchaseWakeAlarm(activity: Activity, onResult: (PurchaseResult) -> Unit)

    /**
     * Re-reads what the account owns. This is also what "restore purchases"
     * does - Play already knows about the purchase on every device, there is
     * nothing to restore from our side.
     */
    fun refresh()

    fun release()

    companion object {
        /** In-app product id; must match the product created in Play Console. */
        const val PRODUCT_RHYTHM_LAYER = "rhythm_layer"

        /** Created and activated in Play Console on 2026-09-23. */
        const val PRODUCT_WAKE_ALARM = "wake_alarm"
    }
}
