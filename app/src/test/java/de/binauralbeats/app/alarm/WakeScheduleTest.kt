package de.binauralbeats.app.alarm

import de.binauralbeats.app.data.WakeAlarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * A wrong answer here is an alarm that rings on Saturday, twice, or not at
 * all - nothing a user forgives. 2026-09-23 is a Wednesday.
 */
class WakeScheduleTest {

    private val berlin = ZoneId.of("Europe/Berlin")

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, berlin)

    private fun alarm(h: Int, m: Int, days: Set<Int> = emptySet(), enabled: Boolean = true) =
        WakeAlarm(id = "a", hour = h, minute = m, days = days, enabled = enabled)

    private val weekdays = setOf(1, 2, 3, 4, 5)

    @Test
    fun `one-off later today rings today`() {
        assertEquals(
            at(2026, 9, 23, 6, 30),
            WakeSchedule.nextTrigger(alarm(6, 30), at(2026, 9, 23, 5, 0))
        )
    }

    @Test
    fun `one-off already past today rings tomorrow`() {
        assertEquals(
            at(2026, 9, 24, 6, 30),
            WakeSchedule.nextTrigger(alarm(6, 30), at(2026, 9, 23, 22, 15))
        )
    }

    @Test
    fun `the minute that just fired is not scheduled again`() {
        assertEquals(
            at(2026, 9, 24, 6, 30),
            WakeSchedule.nextTrigger(alarm(6, 30), at(2026, 9, 23, 6, 30))
        )
    }

    @Test
    fun `weekday alarm on Friday after ringing skips to Monday`() {
        assertEquals(
            at(2026, 9, 28, 6, 30),
            WakeSchedule.nextTrigger(alarm(6, 30, weekdays), at(2026, 9, 25, 7, 0))
        )
    }

    @Test
    fun `repeating alarm still ahead today rings today`() {
        assertEquals(
            at(2026, 9, 23, 6, 30),
            WakeSchedule.nextTrigger(alarm(6, 30, setOf(3)), at(2026, 9, 23, 6, 0))
        )
    }

    @Test
    fun `single weekday already past today rings a week later`() {
        assertEquals(
            at(2026, 9, 30, 6, 30),
            WakeSchedule.nextTrigger(alarm(6, 30, setOf(3)), at(2026, 9, 23, 7, 0))
        )
    }

    @Test
    fun `disabled alarm has no trigger`() {
        assertNull(WakeSchedule.nextTrigger(alarm(6, 30, enabled = false), at(2026, 9, 23, 5, 0)))
    }

    @Test
    fun `alarm whose days are all invalid has no trigger`() {
        assertNull(WakeSchedule.nextTrigger(alarm(6, 30, setOf(0, 9)), at(2026, 9, 23, 5, 0)))
    }

    @Test
    fun `wall-clock time skipped by spring-forward rings one hour later`() {
        // 29.03.2026: clocks jump from 02:00 to 03:00, 02:30 does not exist.
        // Ringing at 03:30 (the gap length later, java.time's rule) is closer
        // to what the user meant than skipping the day.
        val next = WakeSchedule.nextTrigger(alarm(2, 30), at(2026, 3, 28, 23, 0))!!
        assertEquals(29, next.dayOfMonth)
        assertEquals(3, next.hour)
        assertEquals(30, next.minute)
    }

    @Test
    fun `wall-clock time repeated by fall-back rings only the first time`() {
        // 25.10.2026: 02:00-03:00 happens twice. The first 02:30 is still in
        // summer time (+02:00).
        val next = WakeSchedule.nextTrigger(alarm(2, 30), at(2026, 10, 25, 1, 0))!!
        assertEquals(ZoneOffset.ofHours(2), next.offset)
        assertEquals(30, next.minute)
    }

    @Test
    fun `alarm that rang at the first fall-back 0230 does not ring at the second`() {
        val firstRing = at(2026, 10, 25, 2, 30)
        val next = WakeSchedule.nextTrigger(alarm(2, 30), firstRing)!!
        assertEquals(26, next.dayOfMonth)
    }

    @Test
    fun `plenty of time keeps the full ramp`() {
        val plan = WakeSchedule.plan(alarm(6, 30), at(2026, 9, 23, 22, 0))!!
        assertEquals(20, plan.rampMinutes)
        assertEquals(at(2026, 9, 24, 6, 10).toInstant(), plan.rampStart)
    }

    @Test
    fun `wake time closer than the ramp squeezes the ramp and starts now`() {
        // Set at 06:20 for 06:30 with a 20-minute ramp.
        val plan = WakeSchedule.plan(alarm(6, 30), at(2026, 9, 23, 6, 20))!!
        assertEquals(at(2026, 9, 23, 6, 30), plan.wakeAt)
        assertEquals(10, plan.rampMinutes)
        assertEquals(at(2026, 9, 23, 6, 20).toInstant(), plan.rampStart)
    }

    @Test
    fun `less than three minutes left means no ramp, only the wake sound`() {
        val plan = WakeSchedule.plan(alarm(6, 30), at(2026, 9, 23, 6, 28))!!
        assertEquals(0, plan.rampMinutes)
        assertEquals(plan.wakeAt.toInstant(), plan.rampStart)
    }

    @Test
    fun `a receiver firing milliseconds after the ramp start keeps the whole ramp`() {
        // Seen on the Galaxy A54: fired at +59 ms, left 2 min 59.94 s.
        val wake = at(2026, 9, 24, 18, 34).toInstant()
        val fired = at(2026, 9, 24, 18, 31).toInstant().plusMillis(59)
        assertEquals(3, WakeSchedule.effectiveRampMinutes(3, wake, fired))
        assertEquals(20, WakeSchedule.effectiveRampMinutes(20, wake, wake.minusSeconds(20 * 60).plusMillis(59)))
    }

    @Test
    fun `a receiver firing late shortens the ramp instead of overrunning`() {
        val wake = at(2026, 9, 24, 6, 30).toInstant()
        val lateStart = at(2026, 9, 24, 6, 13).toInstant()
        assertEquals(17, WakeSchedule.effectiveRampMinutes(20, wake, lateStart))
    }

    @Test
    fun `ramp start crosses midnight`() {
        val wake = at(2026, 9, 24, 0, 10)
        assertEquals(
            at(2026, 9, 23, 23, 50).toInstant(),
            WakeSchedule.rampStart(wake, 20)
        )
    }
}
