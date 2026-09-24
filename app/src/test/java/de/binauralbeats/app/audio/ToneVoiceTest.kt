package de.binauralbeats.app.audio

import de.binauralbeats.app.data.ToneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class ToneVoiceTest {

    private val rate = 44100

    /** Frequency of a signal from its upward zero crossings over [seconds]. */
    private fun measuredHz(samples: FloatArray, from: Int, seconds: Double): Double {
        val to = from + (seconds * rate).toInt()
        var crossings = 0
        for (i in from + 1 until to) if (samples[i - 1] <= 0f && samples[i] > 0f) crossings++
        return crossings / seconds
    }

    @Test
    fun `a moving beat is heard as itself, also late in a session`() {
        // The old sin(2π·f(t)·t) made a sweeping 10 Hz beat reach hundreds of
        // Hz after a few minutes. Measure the right channel's frequency (carrier
        // + beat) twenty minutes in.
        val voice = ToneVoice(rate)
        val out = FloatArray(2)
        val carrier = 200.0
        val start = 20 * 60 * rate
        val right = FloatArray(rate)
        for (n in 0 until start + rate) {
            val t = n.toDouble() / rate
            val beat = 10.0 + 2.5 * sin(2 * PI * 0.05 * t)
            voice.next(carrier, beat, ToneType.BINAURAL, 1.0, out)
            if (n >= start) right[n - start] = out[1]
        }
        val hz = measuredHz(right, 0, 1.0)
        assertTrue("right channel at $hz Hz", hz in 205.0..215.0)
    }

    @Test
    fun `a constant tone keeps its pitch`() {
        val voice = ToneVoice(rate)
        val out = FloatArray(2)
        val left = FloatArray(rate)
        for (n in 0 until rate) {
            voice.next(392.0, 10.0, ToneType.BINAURAL, 1.0, out)
            left[n] = out[0]
        }
        assertEquals(392.0, measuredHz(left, 0, 1.0), 1.0)
    }

    @Test
    fun `isochronic pulses have no clicks`() {
        // Largest step between neighbouring samples: a hard gate jumps by up to
        // the full amplitude, a smooth one stays near the sine's own slope.
        val voice = ToneVoice(rate)
        val out = FloatArray(2)
        var last = 0f
        var maxStep = 0f
        repeat(rate) {
            voice.next(392.0, 14.0, ToneType.ISOCHRONIC, 1.0, out)
            maxStep = maxOf(maxStep, abs(out[0] - last))
            last = out[0]
        }
        val sineSlope = (2 * PI * 392.0 / rate).toFloat()
        assertTrue("max step $maxStep vs sine slope $sineSlope", maxStep <= sineSlope * 1.2f)
    }

    @Test
    fun `gate is on, off and smooth in between`() {
        val edge = ToneVoice.edgeFraction(10.0, rate) // 0.1 of a cycle
        assertEquals(0.0, ToneVoice.isochronicGate(0.0, edge, 1.0), 1e-9)
        assertEquals(1.0, ToneVoice.isochronicGate(0.25, edge, 1.0), 1e-9)
        assertEquals(0.0, ToneVoice.isochronicGate(0.75, edge, 1.0), 1e-9)
        assertEquals(0.5, ToneVoice.isochronicGate(edge / 2, edge, 1.0), 1e-9)
    }

    @Test
    fun `half depth never drops below half`() {
        for (p in 0..100) {
            val g = ToneVoice.isochronicGate(p / 100.0, 0.1, 0.5)
            assertTrue(g in 0.5..1.0)
        }
        assertEquals(0.5, ToneVoice.isochronicGate(0.75, 0.1, 0.5), 1e-9)
    }
}
