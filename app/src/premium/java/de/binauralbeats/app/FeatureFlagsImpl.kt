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

    // Present in this build; whether the user may use it is decided by
    // Entitlements at runtime.
    override val rhythmLayerAvailable = true
}
