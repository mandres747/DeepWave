package de.binauralbeats.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.binauralbeats.app.data.AmbientSound
import de.binauralbeats.app.data.WakeAlarm
import de.binauralbeats.app.data.WakeAlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * Debug builds only (src/premiumDebug): sets a one-off alarm a few minutes
 * ahead without any UI, so the whole AlarmManager -> receiver -> service path
 * can be tried on an emulator or a phone (debug builds use the package id
 * de.binauralbeats.app.debug, so they sit next to the Play version).
 *
 *   adb -s <device> shell am broadcast -a de.binauralbeats.app.DEBUG_ALARM \
 *       -n de.binauralbeats.app.debug/de.binauralbeats.app.alarm.DebugAlarmReceiver \
 *       --ei minutes 5 --ei ramp 4 [--es ambient STREAM] [--ez vibrate true]
 *   or a fixed time: --ei hour 6 --ei minute 30 --ei ramp 20
 */
class DebugAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val minutes = intent.getIntExtra("minutes", 5)
        val ramp = intent.getIntExtra("ramp", 4)
        val ambient = intent.getStringExtra("ambient")?.let { runCatching { AmbientSound.valueOf(it) }.getOrNull() }
        val at = ZonedDateTime.now().plusMinutes(minutes.toLong())
        // --ei hour/--ei minute set a fixed time instead, for overnight tests.
        val hour = intent.getIntExtra("hour", at.hour)
        val minuteOfHour = intent.getIntExtra("minute", if (intent.hasExtra("hour")) 0 else at.minute)
        val alarm = WakeAlarm(
            id = "debug", hour = hour, minute = minuteOfHour,
            rampMinutes = ramp, ambient = ambient,
            vibrate = intent.getBooleanExtra("vibrate", false)
        )
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WakeAlarmRepository(context).upsert(alarm)
                val plan = AlarmScheduler.schedule(context, alarm)
                Log.i(TAG, "debug alarm: $plan (exact allowed: ${AlarmScheduler.canScheduleExact(context)})")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "WakeAlarm"
    }
}
