package de.binauralbeats.app

interface FeatureFlags {
    val isPremium: Boolean
    val maxCustomPresets: Int
    val wavExportEnabled: Boolean
    val statisticsEnabled: Boolean
    val premiumPresetsEnabled: Boolean
    val soundMixerEnabled: Boolean

    /**
     * Store page this build can be rated on, or null when the build has no
     * store listing to link to. The FOSS flavour ships under a different
     * application id (`.foss`) and is not on Play, so a Play link there would
     * point at a page that does not exist.
     */
    val storeUrl: String?
}
