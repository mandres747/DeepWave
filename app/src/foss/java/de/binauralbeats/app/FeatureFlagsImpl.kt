package de.binauralbeats.app

object FeatureFlagsImpl : FeatureFlags {
    override val isPremium = false
    override val maxCustomPresets = 3
    override val wavExportEnabled = false
    override val statisticsEnabled = false
    override val premiumPresetsEnabled = false
}
