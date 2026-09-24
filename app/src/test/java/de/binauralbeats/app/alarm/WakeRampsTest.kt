package de.binauralbeats.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeRampsTest {

    @Test
    fun `every ramp stretches to exactly the requested length`() {
        for ((key, _) in WakeRamps.all) {
            for (minutes in WakeRamps.DURATIONS + listOf(7, 13, 45)) {
                val ramp = WakeRamps.ramp(key, minutes)
                assertEquals("$key @ $minutes", minutes, ramp.sumOf { it.durationMinutes })
                assertTrue("$key @ $minutes has a zero phase", ramp.all { it.durationMinutes >= 1 })
            }
        }
    }

    @Test
    fun `stretching keeps the frequency order`() {
        val ramp = WakeRamps.ramp("fresh", 10)
        assertEquals(listOf(6f, 10f, 14f), ramp.map { it.frequency })
    }

    @Test
    fun `too short a target still leaves one minute per phase`() {
        val ramp = WakeRamps.ramp("gentle", 1)
        assertEquals(listOf(1, 1, 1), ramp.map { it.durationMinutes })
    }

    @Test
    fun `unknown ramp key falls back to the default`() {
        assertEquals(WakeRamps.ramp(WakeRamps.DEFAULT_KEY, 20), WakeRamps.ramp("gone", 20))
    }

    @Test
    fun `bands glide into each other without jumps`() {
        val ramp = WakeRamps.ramp("fresh", 20) // 6 Hz 5', 10 Hz 7', 14 Hz 8'
        val total = ramp.sumOf { it.durationMinutes } * 60.0
        assertEquals(6.0, WakeRamps.frequencyAt(ramp, 0.0), 0.0)
        assertEquals(14.0, WakeRamps.frequencyAt(ramp, total), 0.0)
        assertEquals(14.0, WakeRamps.frequencyAt(ramp, total + 600), 0.0)
        var last = WakeRamps.frequencyAt(ramp, 0.0)
        var s = 0.0
        while (s <= total) {
            val f = WakeRamps.frequencyAt(ramp, s)
            assertTrue("never falls on a rising ramp", f >= last - 1e-9)
            // One second of glide never moves more than a fraction of a hertz.
            assertTrue("no jump at $s s: $last -> $f", f - last < 0.1)
            last = f
            s += 1.0
        }
    }

    @Test
    fun `layers never add up to more than the headroom`() {
        for (v in listOf(0.1f, 0.5f, 0.7f, 1f)) for (a in listOf(0f, 0.4f, 1f)) for (ringing in listOf(false, true)) {
            val l = WakeRamps.levels(v, a, hasAmbient = a > 0f, ringing = ringing)
            assertTrue("v=$v a=$a ringing=$ringing: $l", l.pulse + l.ambient + l.chime <= WakeRamps.HEADROOM + 1e-6f)
        }
    }

    @Test
    fun `at the wake time the pulse steps back behind the bowl`() {
        val ramp = WakeRamps.levels(0.7f, 0.4f, hasAmbient = true, ringing = false)
        val wake = WakeRamps.levels(0.7f, 0.4f, hasAmbient = true, ringing = true)
        assertTrue(wake.pulse < ramp.pulse * 0.5f)
        assertTrue(wake.chime > wake.pulse * 2f)
        assertEquals(0f, ramp.chime, 0f)
    }

    @Test
    fun `volume rises from silence to the set level and never falls`() {
        val total = 20 * 60_000L
        assertEquals(0f, WakeRamps.volumeAt(0, total, 0.8f), 0f)
        assertEquals(0.8f, WakeRamps.volumeAt(total, total, 0.8f), 1e-6f)
        assertEquals(0.8f, WakeRamps.volumeAt(total * 2, total, 0.8f), 1e-6f)

        var last = -1f
        for (ms in 0L..total step 30_000L) {
            val v = WakeRamps.volumeAt(ms, total, 0.8f)
            assertTrue(v >= last)
            last = v
        }
        // Halfway through, the curve is still well below half volume.
        assertTrue(WakeRamps.volumeAt(total / 2, total, 0.8f) < 0.2f)
    }
}
