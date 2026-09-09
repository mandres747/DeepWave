package de.binauralbeats.app

object FeatureFlagsImpl : FeatureFlags {
    override val isPremium = false
    override val maxCustomPresets = 3
    override val wavExportEnabled = false
    override val statisticsEnabled = false
    override val premiumPresetsEnabled = false
    override val soundMixerEnabled = false
    override val storeUrl: String? = null

    // Paid add-on, and F-Droid has no way to charge for it - so it is not in
    // this build at all, like the WAV export and the mixer.
    override val rhythmLayerAvailable = false
}
