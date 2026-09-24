package de.binauralbeats.app.alarm

import de.binauralbeats.app.data.WakeAlarm
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * When an alarm rings next. Pure java.time, no Android, so the calendar edge
 * cases (weekdays, midnight, daylight-saving switches) are pinned down in
 * WakeScheduleTest instead of being found at 6:30 in the morning.
 *
 * Android only ever gets the *next* occurrence. After it fires, AlarmReceiver
 * asks again with the current time and schedules the one after - a repeating
 * AlarmManager alarm would drift across DST and is inexact anyway.
 */
object WakeSchedule {

    /** Snooze length, decided 23.09.: the classic 9 minutes, no new ramp after it. */
    const val SNOOZE_MINUTES = 9

    /** Unanswered alarms stop this long after the wake time. */
    const val AUTO_STOP_MINUTES = 15

    /** How far ahead we look for a matching weekday; one week always suffices. */
    private const val DAYS_AHEAD = 7L

    /**
     * The next moment at or after [now] when [alarm] rings (the wake time, not
     * the ramp start), or null if the alarm is off or has no valid day.
     *
     * Rules:
     * - a time that is exactly [now] or earlier today does not count - the
     *   alarm that just fired must not be scheduled again for the same minute;
     * - repeating alarms ([WakeAlarm.days] = ISO 1..7) take the first matching
     *   day from today on;
     * - one-off alarms ring at the next occurrence of hour:minute;
     * - on the spring-forward day the wall-clock time may not exist (02:30 on
     *   the last Sunday of March in Europe/Berlin).
     */
    fun nextTrigger(alarm: WakeAlarm, now: ZonedDateTime): ZonedDateTime? {
        if (!alarm.enabled) return null
        val time = LocalTime.of(alarm.hour, alarm.minute)
        val today = now.toLocalDate()
        // Comparing ZonedDateTimes compares instants, which is what keeps the
        // repeated 02:30 in October from counting as "later" a second time.
        // ZonedDateTime.of resolves a gap by moving forward by its length and
        // an overlap to the earlier offset - both decided that way (23.09.).
        return (0..DAYS_AHEAD)
            .asSequence()
            .map { ZonedDateTime.of(today.plusDays(it), time, now.zone) }
            .firstOrNull { candidate ->
                candidate.isAfter(now) &&
                    (alarm.isOneOff || candidate.dayOfWeek.value in alarm.days)
            }
    }

    /**
     * When the ramp has to start so that it ends exactly at [wakeAt]. This is
     * the time handed to AlarmManager; [wakeAt] itself goes into
     * AlarmClockInfo so the status bar shows the time the user set.
     */
    fun rampStart(wakeAt: ZonedDateTime, rampMinutes: Int): Instant =
        wakeAt.toInstant().minusSeconds(rampMinutes * 60L)

    fun nextTrigger(alarm: WakeAlarm, now: Instant, zone: ZoneId): ZonedDateTime? =
        nextTrigger(alarm, now.atZone(zone))

    /** A ramp shorter than this has no room for its phases; only the wake sound plays. */
    const val MIN_RAMP_MINUTES = 3

    /**
     * How long the ramp can actually be when there are only [wakeAt] - [now]
     * left. Decided 23.09.: a wake time closer than the ramp is long does not
     * move to tomorrow - the ramp starts at once, squeezed into the time left,
     * and below [MIN_RAMP_MINUTES] there is no ramp at all. Also used by the
     * service, because the receiver may fire a little late.
     */
    fun effectiveRampMinutes(requested: Int, wakeAt: Instant, now: Instant): Int {
        // Rounded, not truncated: the alarm fires a few milliseconds after the
        // ramp start, and 2 min 59.94 s truncated to 2 dropped a 3-minute ramp
        // entirely and shortened every other ramp by a minute (A54, 24.09.).
        val left = ((wakeAt.toEpochMilli() - now.toEpochMilli() + 30_000L) / 60_000L).toInt()
        val minutes = minOf(requested, left)
        return if (minutes >= MIN_RAMP_MINUTES) minutes else 0
    }

    /** What AlarmManager is told: fire at [rampStart], show [wakeAt]. */
    data class Plan(val wakeAt: ZonedDateTime, val rampStart: Instant, val rampMinutes: Int)

    fun plan(alarm: WakeAlarm, now: ZonedDateTime): Plan? {
        val wakeAt = nextTrigger(alarm, now) ?: return null
        val minutes = effectiveRampMinutes(alarm.rampMinutes, wakeAt.toInstant(), now.toInstant())
        return Plan(wakeAt, rampStart(wakeAt, minutes), minutes)
    }
}
