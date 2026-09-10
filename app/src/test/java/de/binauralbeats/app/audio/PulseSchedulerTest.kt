package de.binauralbeats.app.audio

import de.binauralbeats.app.data.RhythmPattern
import de.binauralbeats.app.data.RhythmPulse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scheduler decides where every click lands, in the live engine and in the
 * exporter alike. Drift here is the kind of fault a listener feels long before
 * anyone can name it, so the timing is pinned down rather than eyeballed.
 */
class PulseSchedulerTest {

    private val sampleRate = 44100

    /** Sample offsets at which a pulse fires, over the given span. */
    private fun fireOffsets(pattern: List<RhythmPulse>, samples: Int): List<Int> {
        val scheduler = PulseScheduler(sampleRate)
        val offsets = mutableListOf<Int>()
        for (i in 0 until samples) {
            if (scheduler.advance(pattern) != null) offsets += i
        }
        return offsets
    }

    @Test
    fun `an empty pattern never fires`() {
        assertTrue(fireOffsets(emptyList(), sampleRate).isEmpty())
        assertNull(PulseScheduler(sampleRate).advance(emptyList()))
    }

    @Test
    fun `first pulse lands on the very first sample`() {
        val offsets = fireOffsets(RhythmPattern.metronome(120), 100)
        assertEquals(listOf(0), offsets)
    }

    @Test
    fun `120 BPM lands every half second`() {
        val offsets = fireOffsets(RhythmPattern.metronome(120), sampleRate * 2)
        assertEquals(listOf(0, 22050, 44100, 66150), offsets)
    }

    @Test
    fun `a tempo with a fractional interval does not drift`() {
        // 90 BPM is 666.667 ms, or 29400 samples exactly at 44.1 kHz. Counting
        // in whole milliseconds would put the 90th beat about a second early.
        val minute = sampleRate * 60
        val offsets = fireOffsets(RhythmPattern.metronome(90), minute)
        assertEquals(90, offsets.size)
        offsets.forEachIndexed { index, offset ->
            assertEquals("beat $index", index * 29400, offset)
        }
    }

    @Test
    fun `accents follow the pattern and repeat with it`() {
        val pattern = RhythmPattern.metronome(120, accentEvery = 4)
        val scheduler = PulseScheduler(sampleRate)
        val accents = mutableListOf<Boolean>()
        for (i in 0 until sampleRate * 4) {
            scheduler.advance(pattern)?.let { accents += it.accented }
        }
        assertEquals(8, accents.size)
        assertEquals(
            listOf(true, false, false, false, true, false, false, false),
            accents
        )
    }

    @Test
    fun `unequal intervals keep their own spacing`() {
        // 4-7-8 breathing: the gaps are what make it a breath rather than a beat.
        val pattern = RhythmPattern.breathing(inhaleSec = 4, hold1Sec = 7, exhaleSec = 8, hold2Sec = 0)
        val offsets = fireOffsets(pattern, sampleRate * 20)
        assertEquals(
            listOf(0, 4 * sampleRate, 11 * sampleRate, 19 * sampleRate),
            offsets
        )
    }

    @Test
    fun `reset starts the pattern over`() {
        val pattern = RhythmPattern.metronome(120)
        val scheduler = PulseScheduler(sampleRate)
        repeat(1000) { scheduler.advance(pattern) }
        scheduler.reset()
        assertEquals(true, scheduler.advance(pattern) != null)
    }
}

class ClickVoiceTest {

    private val sampleRate = 44100

    @Test
    fun `a voice that was never triggered is silent`() {
        val voice = ClickVoice(sampleRate)
        assertEquals(false, voice.isRinging)
        repeat(100) { assertEquals(0f, voice.nextSample(), 0f) }
    }

    @Test
    fun `a triggered click makes sound and then dies away`() {
        val voice = ClickVoice(sampleRate)
        voice.trigger(accented = false)
        assertTrue(voice.isRinging)

        var peak = 0f
        var samples = 0
        while (voice.isRinging && samples < sampleRate) {
            peak = maxOf(peak, kotlin.math.abs(voice.nextSample()))
            samples++
        }

        assertTrue("click should be audible", peak > 0.1f)
        assertTrue("click should not ring for a whole second", samples < sampleRate / 4)
        assertEquals(false, voice.isRinging)
    }

    @Test
    fun `an accent is louder than a plain beat`() {
        fun peakOf(accented: Boolean): Float {
            val voice = ClickVoice(sampleRate)
            voice.trigger(accented)
            var peak = 0f
            repeat(sampleRate / 4) { peak = maxOf(peak, kotlin.math.abs(voice.nextSample())) }
            return peak
        }
        assertTrue(peakOf(accented = true) > peakOf(accented = false))
    }
}
