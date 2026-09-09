package de.binauralbeats.app.data

/**
 * Records which version a user first ran, so a later release can tell a
 * long-standing user apart from a fresh install.
 *
 * Why this has to be written now and not when it is first needed: Play Billing
 * does not report *when* someone bought the app, and PackageManager's
 * firstInstallTime resets on reinstalls and device changes. Once a paid add-on
 * exists (see docs/RHYTHMUS_LAYER_KONZEPT.md), the only reliable way to hand
 * existing buyers that add-on for free is a marker that was already on their
 * device beforehand.
 *
 * The subtle part is the upgrade path. A user who installed an early version
 * and only much later updates straight to the add-on release reaches that
 * release with no marker at all. Writing the current version code for them
 * would misfile a long-standing user as brand new - so an absent marker plus
 * any pre-existing settings is recorded as [PRE_MARKER] instead.
 */
object FirstRunMarker {

    /**
     * Stand-in version code for installs that predate the marker itself:
     * lower than any real version code, so every "did this user predate
     * version X" comparison answers yes.
     */
    const val PRE_MARKER = 0

    /**
     * The value to persist, or null when the marker is already recorded and
     * must not be touched again.
     *
     * @param stored what is on the device, null when nothing was recorded yet
     * @param hasEarlierData whether the app already holds settings from a
     *   previous run, which means an earlier version ran here before
     * @param currentVersionCode the version code running right now
     */
    fun resolve(
        stored: Int?,
        hasEarlierData: Boolean,
        currentVersionCode: Int
    ): Int? = when {
        stored != null -> null
        hasEarlierData -> PRE_MARKER
        else -> currentVersionCode
    }
}
