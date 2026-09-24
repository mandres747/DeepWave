package de.binauralbeats.app.alarm

import de.binauralbeats.app.data.WakeAlarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SleepTimerWakeTest {

    private val berlin = ZoneId.of("Europe/Berlin")
    private fun at(d: Int, h: Int, m: Int) = ZonedDateTime.of(2026, 9, d, h, m, 0, 0, berlin)
    private val weekdays = (1..5).toSet()

    // 2026-09-23 is a Wednesday, 09-25 a Friday, 09-26 a Saturday.

    @Test
    fun `a weekday alarm covers a Wednesday night`() {
        val a = WakeAlarm("w", 7, 0, weekdays)
        val (found, time) = SleepTimerWake.covering(listOf(a), at(23, 22, 30))!!
        assertEquals("w", found.id)
        assertEquals(at(24, 7, 0), time)
    }

    @Test
    fun `a weekday alarm does not cover Friday night`() {
        // Next ring is Monday - far outside "tomorrow morning".
        assertNull(SleepTimerWake.covering(listOf(WakeAlarm("w", 7, 0, weekdays)), at(25, 23, 0)))
    }

    @Test
    fun `disabled alarms do not count`() {
        assertNull(SleepTimerWake.covering(listOf(WakeAlarm("w", 7, 0, weekdays, enabled = false)), at(23, 22, 30)))
    }

    @Test
    fun `the sleep-timer alarm itself is not covering`() {
        val own = WakeAlarm(SleepTimerWake.ALARM_ID, 8, 0)
        assertNull(SleepTimerWake.covering(listOf(own), at(25, 23, 0)))
    }

    @Test
    fun `the soonest of several alarms is shown`() {
        val late = WakeAlarm("late", 8, 30, (1..7).toSet())
        val early = WakeAlarm("early", 6, 15, weekdays)
        assertEquals("early", SleepTimerWake.covering(listOf(late, early), at(23, 22, 0))!!.first.id)
    }
}
