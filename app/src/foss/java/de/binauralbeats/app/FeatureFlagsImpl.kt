package de.binauralbeats.app

object FeatureFlagsImpl : FeatureFlags {
    override val isPremium = false
    override val maxCustomPresets = 3
    override val wavExportEnabled = false
    override val statisticsEnabled = false
    override val premiumPresetsEnabled = false
    override val soundMixerEnabled = false
    override val storeUrl: String? = null

    // F-Droid has no billing, so the layer is simply part of the build.
    override val rhythmLayerEnabled = true
}
