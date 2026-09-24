package de.binauralbeats.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import de.binauralbeats.app.MainActivity
import de.binauralbeats.app.data.WakeAlarm
import java.time.ZonedDateTime

/**
 * Hands the next occurrence of each alarm to AlarmManager. Only ever the next
 * one: AlarmReceiver schedules the following occurrence after each ring, and
 * RescheduleReceiver rebuilds everything after a reboot or a clock change,
 * because Android forgets all alarms on reboot.
 *
 * Both exact alarm calls used here (setAlarmClock, setExactAndAllowWhileIdle)
 * fire on time in Doze and let the receiver start a foreground service from
 * the background. They need SCHEDULE_EXACT_ALARM, which Android 14 denies by default; USE_EXACT_ALARM is
 * deliberately not used (Play reserves it for apps whose core function is an
 * alarm clock). See docs/KLANGWECKER_KONZEPT.md, section 3.
 */
object AlarmScheduler {

    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_WAKE_AT = "wake_at"
    const val EXTRA_RAMP_MINUTES = "ramp_minutes"

    /** Snoozes use their own request code so they never replace the regular alarm. */
    private const val SNOOZE_SUFFIX = "#snooze"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /**
     * Schedules the next occurrence of [alarm], or cancels it when there is
     * none. Returns the plan so the caller can show "rings in 7 h 40 min".
     * Without the exact-alarm permission nothing is scheduled - an alarm that
     * may ring minutes late is worse than one the UI says is not set.
     */
    fun schedule(context: Context, alarm: WakeAlarm, now: ZonedDateTime = ZonedDateTime.now()): WakeSchedule.Plan? {
        cancel(context, alarm.id)
        if (!canScheduleExact(context)) {
            WakeLog.event(context, "not scheduled id=${alarm.id}: no exact-alarm permission")
            return null
        }
        val plan = WakeSchedule.plan(alarm, now) ?: return null
        WakeLog.event(context, "scheduled id=${alarm.id}: ramp ${plan.rampStart} (${plan.rampMinutes} min), wake ${plan.wakeAt}")
        setAlarms(context, alarm.id, plan.wakeAt.toInstant().toEpochMilli(), plan.rampStart.toEpochMilli(), plan.rampMinutes)
        return plan
    }

    fun scheduleSnooze(context: Context, alarmId: String, wakeAtMillis: Long) {
        if (!canScheduleExact(context)) return
        setAlarms(context, alarmId + SNOOZE_SUFFIX, wakeAtMillis, wakeAtMillis, 0)
    }

    fun cancel(context: Context, alarmId: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        for (id in listOf(alarmId, alarmId + SNOOZE_SUFFIX)) {
            for (stage in listOf(AlarmReceiver.STAGE_RAMP, AlarmReceiver.STAGE_WAKE)) {
                am.cancel(firePendingIntent(context, id, stage, 0L, 0))
            }
        }
    }

    /** Strips the snooze marker so a snoozed ring still finds its alarm. */
    fun baseId(requestId: String): String = requestId.removeSuffix(SNOOZE_SUFFIX)

    /**
     * Two alarms per occurrence. AlarmClockInfo has a single time, and it is
     * both what fires and what the status bar shows - so the setAlarmClock
     * alarm sits on the wake time the user set, and the ramp start gets a
     * separate exact alarm. Side effect worth having: if the ramp alarm is ever
     * lost, the wake alarm still rings on time, just without the ramp.
     */
    private fun setAlarms(context: Context, requestId: String, wakeAtMillis: Long, rampStartMillis: Long, rampMinutes: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        val show = PendingIntent.getActivity(
            context, requestId.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(wakeAtMillis, show),
            firePendingIntent(context, requestId, AlarmReceiver.STAGE_WAKE, wakeAtMillis, rampMinutes)
        )
        if (rampMinutes > 0 && rampStartMillis < wakeAtMillis) {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                rampStartMillis,
                firePendingIntent(context, requestId, AlarmReceiver.STAGE_RAMP, wakeAtMillis, rampMinutes)
            )
        }
    }

    private fun firePendingIntent(context: Context, requestId: String, stage: String, wakeAtMillis: Long, rampMinutes: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(stage)
            // The data URI makes each alarm's PendingIntent distinct even if two
            // ids ever share a hash code.
            .setData(android.net.Uri.parse("deepwave-alarm://$requestId"))
            .putExtra(EXTRA_ALARM_ID, requestId)
            .putExtra(EXTRA_WAKE_AT, wakeAtMillis)
            .putExtra(EXTRA_RAMP_MINUTES, rampMinutes)
        return PendingIntent.getBroadcast(
            context, (requestId + stage).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
