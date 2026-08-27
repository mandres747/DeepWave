package de.binauralbeats.app.ui

import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.service.AudioPlaybackService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests for the F-Droid review bug (2026-08-27): a ViewModel
 * recreated while the foreground service kept playing showed "Start" instead
 * of reflecting the still-running session. See computeRestoredPlaybackState.
 */
class PlaybackStateRestorationTest {

    private val samplePhases = listOf(Phase(frequency = 6f, durationMinutes = 20))
    private val sampleParams = AudioPlaybackService.PlaybackParams(
        phases = samplePhases,
        carrier = 210f,
        volume = 0.6f,
        noiseVolume = 0.2f,
        transitionMs = 750
    )

    @Test
    fun `service not playing means nothing to restore`() {
        val restored = computeRestoredPlaybackState(
            generatorIsPlaying = false,
            generatorIsPaused = false,
            generatorCurrentPhaseIndex = 2,
            lastPlaybackParams = sampleParams
        )
        assertNull(restored)
    }

    @Test
    fun `service playing after recreation restores full session state`() {
        val restored = computeRestoredPlaybackState(
            generatorIsPlaying = true,
            generatorIsPaused = false,
            generatorCurrentPhaseIndex = 1,
            lastPlaybackParams = sampleParams
        )
        requireNotNull(restored)
        assertEquals(samplePhases, restored.phases)
        assertEquals(210f, restored.carrier)
        assertEquals(0.6f, restored.volume)
        assertEquals(0.2f, restored.noiseVolume)
        assertEquals(750, restored.transitionMs)
        assertEquals(1, restored.currentPhaseIndex)
        assertEquals(false, restored.isPaused)
    }

    @Test
    fun `paused session restores isPaused true`() {
        val restored = computeRestoredPlaybackState(
            generatorIsPlaying = true,
            generatorIsPaused = true,
            generatorCurrentPhaseIndex = 0,
            lastPlaybackParams = sampleParams
        )
        requireNotNull(restored)
        assertEquals(true, restored.isPaused)
    }

    @Test
    fun `playing without recorded params still restores play state, not phases`() {
        // Defensive case: isPlaying became true without a matching PlaybackParams
        // record (shouldn't happen in practice - see AudioPlaybackService.startPlayback
        // - but must not crash or silently fake session data).
        val restored = computeRestoredPlaybackState(
            generatorIsPlaying = true,
            generatorIsPaused = false,
            generatorCurrentPhaseIndex = 0,
            lastPlaybackParams = null
        )
        requireNotNull(restored)
        assertNull(restored.phases)
        assertNull(restored.carrier)
    }
}
