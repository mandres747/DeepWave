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

    /**
     * Whether this build contains the rhythm layer at all. Off in the FOSS
     * build for the same reason the WAV export and the mixer are off there:
     * it is a paid feature and F-Droid has no way to charge for it. Whether a
     * premium user has actually bought it is a separate, runtime question -
     * see Entitlements. See docs/RHYTHMUS_LAYER_KONZEPT.md.
     */
    val rhythmLayerAvailable: Boolean
}
