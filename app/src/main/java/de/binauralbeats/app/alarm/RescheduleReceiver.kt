package de.binauralbeats.app.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.binauralbeats.app.data.WakeAlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Rebuilds every AlarmManager entry from the stored alarms. Needed after a
 * reboot (Android drops all alarms), after an app update, after the clock or
 * time zone changes (the stored wall-clock time now maps to another instant),
 * and when the exact-alarm permission is granted - alarms switched on while it
 * was missing were saved but never scheduled.
 *
 * Not LOCKED_BOOT_COMPLETED: the alarms live in credential-encrypted DataStore,
 * readable only after the first unlock. A phone that reboots at night and is
 * not unlocked again stays silent - a known v1 limit, see the concept.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        WakeLog.event(context, "reschedule after ${intent.action?.substringAfterLast('.')}")
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )

        suspend fun rescheduleAll(context: Context) {
            for (alarm in WakeAlarmRepository(context).all()) {
                if (alarm.enabled) AlarmScheduler.schedule(context, alarm)
                else AlarmScheduler.cancel(context, alarm.id)
            }
        }
    }
}
