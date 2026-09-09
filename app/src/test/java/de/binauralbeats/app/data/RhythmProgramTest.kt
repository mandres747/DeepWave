package de.binauralbeats.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmPatternTest {

    @Test
    fun `metronome without accent is a single pulse`() {
        val pattern = RhythmPattern.metronome(bpm = 120, accentEvery = 0)
        assertEquals(1, pattern.size)
        assertEquals(500.0, pattern[0].intervalMillis, 0.0001)
        assertEquals(false, pattern[0].accented)
    }

    @Test
    fun `metronome accents the first pulse of every bar`() {
        val pattern = RhythmPattern.metronome(bpm = 120, accentEvery = 4)
        assertEquals(4, pattern.size)
        assertEquals(listOf(true, false, false, false), pattern.map { it.accented })
        pattern.forEach { assertEquals(500.0, it.intervalMillis, 0.0001) }
    }

    @Test
    fun `tempo that does not divide evenly keeps its fractional interval`() {
        // 90 BPM is 666.67 ms. Rounding to whole milliseconds would drift by
        // roughly a second per quarter hour.
        val pattern = RhythmPattern.metronome(bpm = 90)
        assertEquals(666.6667, pattern[0].intervalMillis, 0.001)
    }

    @Test
    fun `tempo is clamped to the supported range`() {
        assertEquals(
            60_000.0 / RhythmPattern.MAX_BPM,
            RhythmPattern.metronome(bpm = 900)[0].intervalMillis,
            0.0001
        )
        assertEquals(
            60_000.0 / RhythmPattern.MIN_BPM,
            RhythmPattern.metronome(bpm = 1)[0].intervalMillis,
            0.0001
        )
    }

    @Test
    fun `breathing pattern drops empty phases`() {
        // 4-7-8 has no second hold; a zero-length phase would fire a pulse at
        // the same instant as the next inhale.
        val pattern = RhythmPattern.breathing(inhaleSec = 4, hold1Sec = 7, exhaleSec = 8, hold2Sec = 0)
        assertEquals(3, pattern.size)
        assertEquals(listOf(4000.0, 7000.0, 8000.0), pattern.map { it.intervalMillis })
        assertEquals(listOf(true, false, false), pattern.map { it.accented })
    }

    @Test
    fun `box breathing keeps all four phases`() {
        val pattern = RhythmPattern.breathing(4, 4, 4, 4)
        assertEquals(4, pattern.size)
        assertTrue(pattern.all { it.intervalMillis == 4000.0 })
    }
}

class RhythmTimelineTest {

    private val program = listOf(
        RhythmStep(bpm = 110, durationMinutes = 2),
        RhythmStep(bpm = 120, durationMinutes = 3),
        RhythmStep(bpm = 100, durationMinutes = 1)
    )

    @Test
    fun `first step covers the start`() {
        assertEquals(110, stepAt(program, 0)?.bpm)
        assertEquals(110, stepAt(program, 119)?.bpm)
    }

    @Test
    fun `boundary belongs to the following step`() {
        assertEquals(120, stepAt(program, 120)?.bpm)
        assertEquals(120, stepAt(program, 299)?.bpm)
        assertEquals(100, stepAt(program, 300)?.bpm)
    }

    @Test
    fun `program that has run out returns null`() {
        assertNull(stepAt(program, 360))
        assertNull(stepAt(program, 10_000))
    }

    @Test
    fun `empty program has nothing to play`() {
        assertNull(stepAt(emptyList(), 0))
    }

    @Test
    fun `zero length steps are skipped instead of swallowing time`() {
        val withGap = listOf(
            RhythmStep(bpm = 90, durationMinutes = 0),
            RhythmStep(bpm = 130, durationMinutes = 1)
        )
        assertEquals(130, stepAt(withGap, 0)?.bpm)
        assertEquals(130, stepAt(withGap, 59)?.bpm)
        assertNull(stepAt(withGap, 60))
    }
}
