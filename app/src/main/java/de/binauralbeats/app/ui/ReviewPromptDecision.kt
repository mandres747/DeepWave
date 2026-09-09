package de.binauralbeats.app.ui

/**
 * When to ask the user for a Play Store review. Pulled out as a pure function
 * so the timing rule is unit-testable and lives in one place instead of being
 * spread across the ViewModel.
 *
 * The rule follows the closed-test feedback (2026-08-29, "Rate Your App
 * Button"): ask only after the user has actually finished sessions, never on
 * first launch, and never twice - a declined prompt stays declined.
 */
object ReviewPromptDecision {

    /**
     * Completed sessions required before the prompt appears. Three is the point
     * where someone has clearly adopted the app rather than tried it once, so
     * the rating reflects real use.
     */
    const val SESSIONS_BEFORE_PROMPT = 3

    fun shouldPrompt(
        completedSessions: Int,
        alreadyHandled: Boolean,
        storeAvailable: Boolean
    ): Boolean = storeAvailable &&
        !alreadyHandled &&
        completedSessions >= SESSIONS_BEFORE_PROMPT
}
