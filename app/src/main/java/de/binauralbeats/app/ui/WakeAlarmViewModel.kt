package de.binauralbeats.app.ui

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.binauralbeats.app.FeatureFlagsImpl
import de.binauralbeats.app.alarm.AlarmScheduler
import de.binauralbeats.app.alarm.SleepTimerWake
import de.binauralbeats.app.alarm.WakeRamps
import de.binauralbeats.app.alarm.WakeSchedule
import de.binauralbeats.app.audio.WakePreview
import de.binauralbeats.app.billing.AccessState
import de.binauralbeats.app.billing.Entitlements
import de.binauralbeats.app.billing.EntitlementsImpl
import de.binauralbeats.app.billing.PurchaseResult
import de.binauralbeats.app.data.AmbientSound
import de.binauralbeats.app.data.WakeAlarm
import de.binauralbeats.app.data.WakeAlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID

/**
 * State for the wake-alarm sheet. Kept out of BinauralViewModel on purpose:
 * the alarm runs in its own service and shares nothing with a session, and
 * that class is large enough already.
 */
class WakeAlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<Application>()
    private val repo = WakeAlarmRepository(application)
    private val preview = WakePreview()

    val available: Boolean get() = FeatureFlagsImpl.wakeAlarmAvailable
    /** Bought on its own or with the bundle. */
    val owned: StateFlow<Boolean> = AccessState.access.map { it.wake }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccessState.access.value.wake)
    val price: StateFlow<String?> = EntitlementsImpl.prices.map { it[Entitlements.PRODUCT_WAKE_ALARM] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val alarms: StateFlow<List<WakeAlarm>> =
        repo.alarms.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lastWakeMinuteOfDay: StateFlow<Int> =
        repo.lastWakeMinuteOfDay.stateIn(viewModelScope, SharingStarted.Eagerly, WakeAlarmRepository.DEFAULT_WAKE_MINUTE)

    var showSheet by mutableStateOf(false)

    /** The alarm open in the editor dialog; null when the dialog is closed. */
    var editing by mutableStateOf<WakeAlarm?>(null)

    /** Set when an alarm was switched on without the exact-alarm permission. */
    var showExactAlarmExplanation by mutableStateOf(false)

    // Re-read on every resume: both permissions can change in system settings
    // while the app is in the background.
    var canScheduleExact by mutableStateOf(AlarmScheduler.canScheduleExact(application))
        private set
    var canFullScreen by mutableStateOf(checkFullScreen())
        private set

    fun onResume() {
        canScheduleExact = AlarmScheduler.canScheduleExact(app)
        canFullScreen = checkFullScreen()
    }

    private fun checkFullScreen(): Boolean =
        Build.VERSION.SDK_INT < 34 ||
            app.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    /** Defaults decided 23.09.: 07:00 Mon-Fri, fresh ramp, 20 min, stream at 40 %. */
    fun newAlarm(): WakeAlarm = WakeAlarm(
        id = UUID.randomUUID().toString(),
        hour = 7,
        minute = 0,
        days = (1..5).toSet(),
        rampKey = WakeRamps.DEFAULT_KEY,
        rampMinutes = 20,
        volume = 0.7f,
        ambient = AmbientSound.STREAM,
        ambientVolume = 0.4f,
        vibrate = false
    )

    /**
     * Saves and schedules. Returns how long until it rings, for the "rings in
     * 7 h 40 min" message, or null if it cannot ring yet. Without the
     * exact-alarm permission the alarm is still saved as switched on: once the
     * permission is granted, RescheduleReceiver picks it up (decided 23.09.:
     * explain, send to settings, the alarm becomes active afterwards).
     */
    fun save(alarm: WakeAlarm, onScheduled: (Duration?) -> Unit = {}) {
        preview.stop()
        viewModelScope.launch {
            repo.upsert(alarm)
            if (alarm.enabled) repo.rememberWakeTime(alarm.hour, alarm.minute)
            onScheduled(apply(alarm))
        }
        editing = null
    }

    fun setEnabled(alarm: WakeAlarm, enabled: Boolean, onScheduled: (Duration?) -> Unit = {}) =
        save(alarm.copy(enabled = enabled), onScheduled)

    fun delete(alarm: WakeAlarm) {
        preview.stop()
        AlarmScheduler.cancel(app, alarm.id)
        viewModelScope.launch { repo.delete(alarm.id) }
        editing = null
    }

    private fun apply(alarm: WakeAlarm): Duration? {
        if (!alarm.enabled) {
            AlarmScheduler.cancel(app, alarm.id)
            return null
        }
        if (!AlarmScheduler.canScheduleExact(app)) {
            showExactAlarmExplanation = true
            return null
        }
        val plan = AlarmScheduler.schedule(app, alarm) ?: return null
        return Duration.between(ZonedDateTime.now(), plan.wakeAt)
    }

    /** Wake time of the next enabled alarm, for the toolbar label. */
    fun nextWake(list: List<WakeAlarm>): ZonedDateTime? {
        if (!canScheduleExact) return null
        val now = ZonedDateTime.now()
        return list.mapNotNull { WakeSchedule.nextTrigger(it, now) }.minOrNull()
    }

    // --- Sleep timer line ---

    /** A regular alarm that already rings by tomorrow morning, with its time. */
    fun coveringAlarm(list: List<WakeAlarm>) = SleepTimerWake.covering(list, ZonedDateTime.now())

    /** The one-off alarm the sleep-timer line manages, if it is on. */
    fun sleepTimerAlarm(list: List<WakeAlarm>): WakeAlarm? =
        list.firstOrNull { it.id == SleepTimerWake.ALARM_ID && it.enabled }

    /**
     * Switches the sleep-timer one-off alarm. It takes the settings of the
     * user's most recently listed alarm, so ramp, volume and ambient match
     * what they chose elsewhere; switching off removes it again.
     */
    fun setSleepTimerWake(on: Boolean, hour: Int, minute: Int, onScheduled: (Duration?) -> Unit = {}) {
        if (!on) {
            alarms.value.firstOrNull { it.id == SleepTimerWake.ALARM_ID }?.let { delete(it) }
            return
        }
        val template = alarms.value.lastOrNull { it.id != SleepTimerWake.ALARM_ID } ?: newAlarm()
        save(
            template.copy(id = SleepTimerWake.ALARM_ID, hour = hour, minute = minute, days = emptySet(), enabled = true),
            onScheduled
        )
    }

    fun playPreview(alarm: WakeAlarm) =
        preview.play(WakeRamps.ramp(alarm.rampKey, alarm.rampMinutes), alarm.volume)

    fun stopPreview() = preview.stop()

    fun purchase(activity: Activity, onResult: (PurchaseResult) -> Unit) =
        EntitlementsImpl.purchase(activity, Entitlements.PRODUCT_WAKE_ALARM, onResult)

    fun restore() = EntitlementsImpl.refresh()

    fun exactAlarmSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${app.packageName}"))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
        }

    fun fullScreenSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= 34) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${app.packageName}"))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
        }

    override fun onCleared() {
        preview.stop()
        super.onCleared()
    }
}
