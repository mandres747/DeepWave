package de.binauralbeats.app

object FeatureFlagsImpl : FeatureFlags {
    override val isPremium = true
    override val maxCustomPresets = Int.MAX_VALUE
    override val wavExportEnabled = true
    override val statisticsEnabled = true
    override val premiumPresetsEnabled = true
    override val soundMixerEnabled = true
    override val storeUrl: String? =
        "https://play.google.com/store/apps/details?id=de.binauralbeats.app"

    // Stays false until the in-app purchase is wired up; flipping this without
    // an entitlement check would hand the paid add-on to everyone for free.
    override val rhythmLayerEnabled = false
}
