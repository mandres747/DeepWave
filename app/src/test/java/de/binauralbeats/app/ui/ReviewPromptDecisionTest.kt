package de.binauralbeats.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rating prompt is the one thing in the app that interrupts the user
 * unasked, so its timing rule is pinned down here: never early, never twice,
 * never in a build that has no store page.
 */
class ReviewPromptDecisionTest {

    private val threshold = ReviewPromptDecision.SESSIONS_BEFORE_PROMPT

    @Test
    fun `no prompt before the session threshold is reached`() {
        for (sessions in 0 until threshold) {
            assertFalse(
                "prompted after only $sessions session(s)",
                ReviewPromptDecision.shouldPrompt(
                    completedSessions = sessions,
                    alreadyHandled = false,
                    storeAvailable = true
                )
            )
        }
    }

    @Test
    fun `prompts once the threshold session is completed`() {
        assertTrue(
            ReviewPromptDecision.shouldPrompt(
                completedSessions = threshold,
                alreadyHandled = false,
                storeAvailable = true
            )
        )
    }

    @Test
    fun `never prompts again after the user rated or declined`() {
        assertFalse(
            ReviewPromptDecision.shouldPrompt(
                completedSessions = threshold + 20,
                alreadyHandled = true,
                storeAvailable = true
            )
        )
    }

    @Test
    fun `builds without a store listing never prompt`() {
        // The FOSS flavour has no Play listing (different application id), so
        // there is nothing to send the user to.
        assertFalse(
            ReviewPromptDecision.shouldPrompt(
                completedSessions = threshold,
                alreadyHandled = false,
                storeAvailable = false
            )
        )
    }
}
