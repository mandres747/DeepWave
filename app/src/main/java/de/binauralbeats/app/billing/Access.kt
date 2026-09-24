package de.binauralbeats.app.billing

import de.binauralbeats.app.FeatureFlagsImpl
import de.binauralbeats.app.data.FirstRunMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * What this user may use right now. FeatureFlags says what the build
 * contains; Entitlements says what the Play account owns; this puts the two
 * together with the one rule Play cannot know: people who bought DeepWave
 * while it was a paid app keep Premium. See docs/FREEMIUM_KONZEPT.md.
 */
data class Access(
    val premium: Boolean,
    val rhythm: Boolean,
    val wake: Boolean,
    /** The bundle is only offered while none of its parts is owned. */
    val offerBundle: Boolean
) {
    companion object {
        /** First version code of the free download; anything earlier was bought. */
        const val FREEMIUM_VERSION_CODE = 10

        /** Own presets without Premium - the same limit the F-Droid build has. */
        const val FREE_CUSTOM_PRESETS = 3

        val NONE = Access(premium = false, rhythm = false, wake = false, offerBundle = false)

        /**
         * @param firstSeenVersionCode the marker from FirstRunMarker, null while
         *   it has not been read yet - treated as "not a previous buyer" until then
         * @param premiumInBuild false in the FOSS build, which has none of it
         */
        fun resolve(
            owned: Set<String>,
            firstSeenVersionCode: Int?,
            premiumInBuild: Boolean,
            rhythmInBuild: Boolean,
            wakeInBuild: Boolean
        ): Access {
            val complete = Entitlements.PRODUCT_COMPLETE in owned
            val boughtPaidApp = firstSeenVersionCode != null &&
                firstSeenVersionCode in FirstRunMarker.PRE_MARKER until FREEMIUM_VERSION_CODE
            return Access(
                premium = premiumInBuild && (complete || Entitlements.PRODUCT_PREMIUM in owned || boughtPaidApp),
                rhythm = rhythmInBuild && (complete || Entitlements.PRODUCT_RHYTHM_LAYER in owned),
                wake = wakeInBuild && (complete || Entitlements.PRODUCT_WAKE_ALARM in owned),
                offerBundle = premiumInBuild && !boughtPaidApp && owned.none { it in Entitlements.ALL_PRODUCTS }
            )
        }
    }
}

/**
 * The live Access for the app. The first-seen marker is handed in by
 * BinauralViewModel once it has been written, which happens before anything
 * else touches the settings store.
 */
object AccessState {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val firstSeenVersionCode = MutableStateFlow<Int?>(null)

    val access: StateFlow<Access> = combine(EntitlementsImpl.owned, firstSeenVersionCode) { owned, firstSeen ->
        Access.resolve(
            owned = owned,
            firstSeenVersionCode = firstSeen,
            premiumInBuild = FeatureFlagsImpl.isPremium,
            rhythmInBuild = FeatureFlagsImpl.rhythmLayerAvailable,
            wakeInBuild = FeatureFlagsImpl.wakeAlarmAvailable
        )
    }.stateIn(scope, SharingStarted.Eagerly, Access.NONE)
}
