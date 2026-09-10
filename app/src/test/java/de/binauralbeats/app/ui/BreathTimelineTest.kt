package de.binauralbeats.app.ui

import de.binauralbeats.app.data.RhythmPattern
import de.binauralbeats.app.ui.components.BreathPhase
import de.binauralbeats.app.ui.components.BreathingPattern
import de.binauralbeats.app.ui.components.breathMomentAt
import de.binauralbeats.app.ui.components.phases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The breathing circle and the audible pulse have to describe the same breath.
 * They used to be two systems - separate pattern pickers, separate start
 * buttons, separate clocks - and drifted apart within a cycle. These tests pin
 * down the shared timeline they now both read.
 */
class BreathTimelineTest {

    @Test
    fun `a breath starts on the inhale`() {
        val moment = breathMomentAt(BreathingPattern.RELAXING, 0L)
        assertEquals(BreathPhase.INHALE, moment.phase)
        assertEquals(0f, moment.progress, 0.0001f)
        assertEquals(0, moment.cycle)
    }

    @Test
    fun `box breathing hands over at every fourth second`() {
        val box = BreathingPattern.BOX // 4-4-4-4
        assertEquals(BreathPhase.INHALE, breathMomentAt(box, 3_999L).phase)
        assertEquals(BreathPhase.HOLD, breathMomentAt(box, 4_000L).phase)
        assertEquals(BreathPhase.EXHALE, breathMomentAt(box, 8_000L).phase)
        assertEquals(BreathPhase.HOLD, breathMomentAt(box, 12_000L).phase)
        // Quarter of the way into the first hold.
        assertEquals(0.25f, breathMomentAt(box, 5_000L).progress, 0.0001f)
    }

    @Test
    fun `4-7-8 keeps its unequal phases`() {
        val p = BreathingPattern.RELAXING // 4-7-8, no second hold
        assertEquals(BreathPhase.INHALE, breathMomentAt(p, 0L).phase)
        assertEquals(BreathPhase.HOLD, breathMomentAt(p, 4_000L).phase)
        assertEquals(BreathPhase.EXHALE, breathMomentAt(p, 11_000L).phase)
        assertEquals(BreathPhase.EXHALE, breathMomentAt(p, 18_999L).phase)
        // 19 s later the whole thing starts over.
        val wrapped = breathMomentAt(p, 19_000L)
        assertEquals(BreathPhase.INHALE, wrapped.phase)
        assertEquals(0f, wrapped.progress, 0.0001f)
        assertEquals(1, wrapped.cycle)
    }

    @Test
    fun `a pattern without holds goes straight from in to out`() {
        // ENERGIZE is 2-0-2. A zero-length hold must not swallow a phase or
        // leave a gap the circle would sit still in.
        val p = BreathingPattern.ENERGIZE
        assertEquals(listOf(BreathPhase.INHALE, BreathPhase.EXHALE), p.phases().map { it.first })
        assertEquals(BreathPhase.INHALE, breathMomentAt(p, 1_999L).phase)

        val exhale = breathMomentAt(p, 2_500L)
        assertEquals(BreathPhase.EXHALE, exhale.phase)
        assertEquals(0.25f, exhale.progress, 0.0001f)
    }

    @Test
    fun `cycles are counted, not just phases`() {
        val p = BreathingPattern.CALM // 4-2-6, twelve seconds
        assertEquals(0, breathMomentAt(p, 11_999L).cycle)
        assertEquals(1, breathMomentAt(p, 12_000L).cycle)
        assertEquals(9, breathMomentAt(p, 12_000L * 9 + 500L).cycle)
    }

    @Test
    fun `time before the start is treated as the start`() {
        // The wall clock can hand back a negative gap for a frame after a
        // restart; that must not throw or show a phase from the end.
        val moment = breathMomentAt(BreathingPattern.BOX, -500L)
        assertEquals(BreathPhase.INHALE, moment.phase)
        assertEquals(0f, moment.progress, 0.0001f)
    }

    @Test
    fun `progress never reaches one`() {
        // A phase at progress 1.0 is the next phase at 0.0. Letting both exist
        // would make the circle stutter at every boundary.
        for (pattern in BreathingPattern.entries) {
            var millis = 0L
            while (millis < pattern.totalSeconds * 1000L * 2) {
                val progress = breathMomentAt(pattern, millis).progress
                assertTrue("$pattern at $millis: $progress", progress >= 0f && progress < 1f)
                millis += 97L
            }
        }
    }

    @Test
    fun `the circle and the click describe the same breath`() {
        // The regression that started all this: guide and engine each decided
        // for themselves which phases to drop. If these two ever disagree, the
        // pulse lands somewhere the circle is not.
        for (pattern in BreathingPattern.entries) {
            val drawn = pattern.phases().map { (_, seconds) -> seconds * 1000.0 }
            val played = RhythmPattern
                .breathing(pattern.inhale, pattern.hold1, pattern.exhale, pattern.hold2)
                .map { it.intervalMillis }
            assertEquals("$pattern", drawn, played)
        }
    }

    @Test
    fun `the first pulse of a breath is the accented one`() {
        // The inhale is what the listener should feel as the downbeat.
        val pulses = RhythmPattern.breathing(4, 7, 8, 0)
        assertEquals(true, pulses.first().accented)
        assertTrue(pulses.drop(1).none { it.accented })
    }
}
