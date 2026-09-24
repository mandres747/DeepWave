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
 * can be tried on the emulator.
 *
 *   adb -s emulator-5554 shell am broadcast -a de.binauralbeats.app.DEBUG_ALARM \
 *       -n de.binauralbeats.app/.alarm.DebugAlarmReceiver --ei minutes 5 --ei ramp 4
 */
class DebugAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val minutes = intent.getIntExtra("minutes", 5)
        val ramp = intent.getIntExtra("ramp", 4)
        val ambient = intent.getStringExtra("ambient")?.let { runCatching { AmbientSound.valueOf(it) }.getOrNull() }
        val at = ZonedDateTime.now().plusMinutes(minutes.toLong())
        val alarm = WakeAlarm(
            id = "debug", hour = at.hour, minute = at.minute,
            rampMinutes = ramp, ambient = ambient
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
