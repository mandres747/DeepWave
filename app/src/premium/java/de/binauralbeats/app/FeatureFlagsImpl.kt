package de.binauralbeats.app

object FeatureFlagsImpl : FeatureFlags {
    override val isPremium = true
    override val maxCustomPresets = Int.MAX_VALUE
    override val wavExportEnabled = true
    override val statisticsEnabled = true
    override val premiumPresetsEnabled = true
}
