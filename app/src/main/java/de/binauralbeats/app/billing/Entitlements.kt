package de.binauralbeats.app.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/** What came back from a purchase attempt, so the UI can say the right thing. */
enum class PurchaseResult {
    /** The product is now owned - either just bought or already on the account. */
    OWNED,

    /** The user backed out. Not an error, and must not be reported as one. */
    CANCELLED,

    /** Billing is not usable here at all (no Play Store, unsupported device). */
    UNAVAILABLE,

    ERROR
}

/**
 * Which paid products the Play account owns, kept apart from FeatureFlags on
 * purpose: FeatureFlags answers "does this build contain the feature",
 * Entitlements answers "has this account bought it", and Access combines
 * the two with the rule for people who bought the former paid app.
 *
 * The premium implementation talks to Play Billing; the FOSS one is a stub
 * with no dependency on it, which is what keeps the proprietary library out
 * of the F-Droid build. See docs/FREEMIUM_KONZEPT.md.
 */
interface Entitlements {

    /** Product ids this Play account owns. */
    val owned: StateFlow<Set<String>>

    /** Localised price per product id, as Play reports it; missing while unknown. */
    val prices: StateFlow<Map<String, String>>

    /** Opens the billing connection and picks up purchases made elsewhere. */
    fun connect(context: Context)

    fun purchase(activity: Activity, productId: String, onResult: (PurchaseResult) -> Unit)

    /**
     * Re-reads what the account owns. This is also what "restore purchases"
     * does - Play already knows about the purchase on every device, there is
     * nothing to restore from our side.
     */
    fun refresh()

    fun release()

    companion object {
        // In-app product ids; each must match a product in Play Console.
        const val PRODUCT_RHYTHM_LAYER = "rhythm_layer"

        /** Created and activated in Play Console on 2026-09-23. */
        const val PRODUCT_WAKE_ALARM = "wake_alarm"

        /** Premium unlock of the free download, 3.99 EUR. */
        const val PRODUCT_PREMIUM = "premium"

        /** Premium plus both add-ons, 5.99 EUR. */
        const val PRODUCT_COMPLETE = "complete"

        val ALL_PRODUCTS = listOf(PRODUCT_PREMIUM, PRODUCT_COMPLETE, PRODUCT_RHYTHM_LAYER, PRODUCT_WAKE_ALARM)
    }
}
