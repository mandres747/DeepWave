package de.binauralbeats.app

interface FeatureFlags {
    val isPremium: Boolean
    val maxCustomPresets: Int
    val wavExportEnabled: Boolean
    val statisticsEnabled: Boolean
    val premiumPresetsEnabled: Boolean
}
