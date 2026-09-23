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
