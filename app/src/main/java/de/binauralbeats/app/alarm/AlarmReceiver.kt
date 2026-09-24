package de.binauralbeats.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.binauralbeats.app.data.WakeAlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager at the ramp start (STAGE_RAMP) and at the wake time
 * (STAGE_WAKE). Starts WakeAlarmService right away - the permission to start a
 * foreground service from the background only lasts a few seconds after an
 * exact alarm - and then, off the main thread, schedules the next occurrence.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val stage = intent.action ?: return
        if (stage != STAGE_RAMP && stage != STAGE_WAKE) return
        val requestId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val alarmId = AlarmScheduler.baseId(requestId)
        val wakeAt = intent.getLongExtra(AlarmScheduler.EXTRA_WAKE_AT, 0L)
        WakeLog.event(
            context,
            "fired ${stage.substringAfterLast('.')} id=$requestId, " +
                "${(System.currentTimeMillis() - wakeAt) / 1000}s relative to wake time"
        )

        context.startForegroundService(
            Intent(context, WakeAlarmService::class.java)
                .setAction(if (stage == STAGE_RAMP) WakeAlarmService.ACTION_RAMP else WakeAlarmService.ACTION_WAKE)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                .putExtra(AlarmScheduler.EXTRA_WAKE_AT, intent.getLongExtra(AlarmScheduler.EXTRA_WAKE_AT, 0L))
                .putExtra(AlarmScheduler.EXTRA_RAMP_MINUTES, intent.getIntExtra(AlarmScheduler.EXTRA_RAMP_MINUTES, 0))
        )

        // The next occurrence is scheduled once, at the wake stage: doing it at
        // the ramp stage would move the pending wake alarm of *this* morning.
        if (stage != STAGE_WAKE || requestId != alarmId) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = WakeAlarmRepository(context)
                val alarm = repo.get(alarmId) ?: return@launch
                if (alarm.isOneOff) {
                    repo.upsert(alarm.copy(enabled = false))
                } else {
                    AlarmScheduler.schedule(context, alarm)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val STAGE_RAMP = "de.binauralbeats.app.alarm.RAMP"
        const val STAGE_WAKE = "de.binauralbeats.app.alarm.WAKE"
    }
}
