package de.binauralbeats.app.alarm

import de.binauralbeats.app.alarm.WakeAlarmLabels.Days
import de.binauralbeats.app.audio.WakePreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import kotlin.math.abs

class WakeAlarmLabelsTest {

    @Test
    fun `day sets get their common names`() {
        assertEquals(Days.Once, WakeAlarmLabels.days(emptySet()))
        assertEquals(Days.Daily, WakeAlarmLabels.days((1..7).toSet()))
        assertEquals(Days.Weekdays, WakeAlarmLabels.days(setOf(5, 3, 1, 2, 4)))
        assertEquals(Days.Weekend, WakeAlarmLabels.days(setOf(7, 6)))
        assertEquals(Days.Some(listOf(1, 3, 5)), WakeAlarmLabels.days(setOf(5, 1, 3)))
    }

    @Test
    fun `invalid day numbers are ignored`() {
        assertEquals(Days.Weekend, WakeAlarmLabels.days(setOf(0, 6, 7, 9)))
        assertEquals(Days.Once, WakeAlarmLabels.days(setOf(0, 8)))
    }

    @Test
    fun `time until rounds up to whole minutes`() {
        assertEquals(0L to 1L, WakeAlarmLabels.until(Duration.ofSeconds(30)))
        assertEquals(7L to 40L, WakeAlarmLabels.until(Duration.ofMinutes(460)))
        assertEquals(7L to 41L, WakeAlarmLabels.until(Duration.ofMinutes(460).plusSeconds(1)))
        assertEquals(24L to 0L, WakeAlarmLabels.until(Duration.ofHours(24)))
    }

    @Test
    fun `preview rises, walks the bands in order and fades out`() {
        val rate = 8000
        val ramp = WakeRamps.ramp("fresh", 20)
        val pcm = WakePreview.render(ramp, 1f, rate)

        fun peak(fromSec: Double, toSec: Double) =
            (fromSec * rate).toInt().until((toSec * rate).toInt()).maxOf { abs(pcm[it].toInt()) }

        assertTrue("quiet start", peak(0.0, 1.0) < peak(6.0, 7.0))
        assertTrue("bowl rings after the ramp", peak(8.0, 9.0) > 0)
        assertEquals("ends in silence", 0, abs(pcm.last().toInt()))

        assertEquals(6.0, WakePreview.bandAt(ramp, 0.0), 0.0)
        assertEquals(10.0, WakePreview.bandAt(ramp, 7.0), 0.0)
        assertEquals(14.0, WakePreview.bandAt(ramp, 19.9), 0.0)
    }
}
