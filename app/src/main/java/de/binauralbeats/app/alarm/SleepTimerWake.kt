package de.binauralbeats.app.alarm

import de.binauralbeats.app.data.WakeAlarm
import java.time.ZonedDateTime

/**
 * The "wake me tomorrow" line under the sleep timer. It is offered while the
 * timer is being set - when the timer runs out, the user is asleep and would
 * never see the question (the first draft of the concept had it there).
 *
 * Decided 2026-09-24: if an alarm already rings by tomorrow morning, the line
 * only shows it; otherwise it switches a one-off alarm on and off, kept under
 * its own id so it never touches the user's regular alarms.
 */
object SleepTimerWake {

    /** Id of the one-off alarm the sleep-timer line manages. */
    const val ALARM_ID = "sleep_timer"

    /** "Tomorrow morning" as seen from bedtime: anything ringing within this window. */
    const val WINDOW_HOURS = 18L

    /**
     * The soonest regular alarm that rings within [WINDOW_HOURS] of [now],
     * with its ring time, or null. The sleep-timer alarm itself does not
     * count - it is what the line switches, not something already covering.
     */
    fun covering(alarms: List<WakeAlarm>, now: ZonedDateTime): Pair<WakeAlarm, ZonedDateTime>? =
        alarms.asSequence()
            .filter { it.id != ALARM_ID }
            .mapNotNull { a -> WakeSchedule.nextTrigger(a, now)?.let { a to it } }
            .filter { (_, at) -> at.isBefore(now.plusHours(WINDOW_HOURS)) }
            .minByOrNull { (_, at) -> at }
}
