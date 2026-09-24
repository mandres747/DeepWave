package de.binauralbeats.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ChimeVoiceTest {

    private val rate = 44100

    @Test
    fun `silent until struck`() {
        val v = ChimeVoice(rate)
        assertFalse(v.isRinging)
        assertEquals(0f, v.nextSample(), 0f)
    }

    @Test
    fun `a strike stays within full scale`() {
        val v = ChimeVoice(rate)
        v.strike(1f)
        repeat(rate * 2) { assertTrue(abs(v.nextSample()) <= 1f) }
    }

    @Test
    fun `starts without a click`() {
        // The very first samples must be tiny, not a jump to full level.
        val v = ChimeVoice(rate)
        v.strike(1f)
        assertTrue(abs(v.nextSample()) < 0.01f)
        assertTrue(abs(v.nextSample()) < 0.02f)
    }

    @Test
    fun `rings for seconds and then dies away on its own`() {
        val v = ChimeVoice(rate)
        v.strike(1f)
        repeat(rate * 5) { v.nextSample() }
        assertTrue("a bowl should still hum after 5 s", v.isRinging)
        repeat(rate * 40) { v.nextSample() }
        assertFalse("and be silent after 45 s", v.isRinging)
    }

    @Test
    fun `strikes grow from half to full strength`() {
        assertEquals(0.5f, ChimeEngine.strengthFor(0), 0f)
        assertEquals(1f, ChimeEngine.strengthFor(4), 0f)
        assertEquals(1f, ChimeEngine.strengthFor(20), 0f)
    }
}
