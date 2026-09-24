package de.binauralbeats.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import de.binauralbeats.app.MainActivity
import de.binauralbeats.app.R
import de.binauralbeats.app.audio.AmbientEngine
import de.binauralbeats.app.audio.BinauralGenerator
import de.binauralbeats.app.audio.ChimeEngine
import de.binauralbeats.app.audio.PlaybackUsage
import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.data.WakeAlarm
import de.binauralbeats.app.data.WakeAlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Date
import android.text.format.DateFormat

/**
 * Plays a wake alarm: the ramp before the wake time, then the alarm itself
 * until it is dismissed, snoozed or runs out.
 *
 * A service of its own rather than more state in AudioPlaybackService: that
 * one belongs to the ViewModel, whose UI reads its generator - an alarm there
 * would show up in the morning as a session nobody started. Its own engine
 * instances also play on the alarm stream (PlaybackUsage.ALARM), so the alarm
 * follows the alarm volume and gets through Do Not Disturb.
 */
class WakeAlarmService : Service() {

    private val generator = BinauralGenerator()
    private val ambient = AmbientEngine()
    private val chime = ChimeEngine()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    private var alarmId: String? = null
    private var wakeAtMillis = 0L

    // Fade-in: from silence at fadeStartedAt to full at fadeStartedAt + fadeTotalMs.
    private var fadeStartedAt = 0L
    private var fadeTotalMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RAMP -> onRamp(intent)
            ACTION_WAKE -> onWake(intent)
            ACTION_SNOOZE -> snooze()
            ACTION_DISMISS -> finish()
            else -> finish()
        }
        // An alarm that was killed must not replay by itself hours later.
        return START_NOT_STICKY
    }

    // --- Stages ---

    private fun onRamp(intent: Intent) {
        wakeAtMillis = intent.getLongExtra(AlarmScheduler.EXTRA_WAKE_AT, 0L)
        alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        // startForeground within the few seconds Android allows, before any I/O.
        goForeground(rampNotification(), keepAwakeUntil = wakeAtMillis + AFTER_WAKE_MS, id = NOTIFICATION_ID_RAMP)
        val requested = intent.getIntExtra(AlarmScheduler.EXTRA_RAMP_MINUTES, 0)
        val id = alarmId ?: return finish()

        scope.launch {
            val alarm = loadAlarm(id) ?: return@launch finish()
            val now = Instant.now()
            val minutes = WakeSchedule.effectiveRampMinutes(requested, Instant.ofEpochMilli(wakeAtMillis), now)
            // Too late for a ramp (receiver ran very late): the wake stage will
            // still fire on time and play the alarm on its own.
            if (minutes == 0) return@launch
            startSound(alarm, WakeRamps.ramp(alarm.rampKey, minutes), fadeMs = wakeAtMillis - now.toEpochMilli(), ringing = false)
        }
    }

    private fun onWake(intent: Intent) {
        wakeAtMillis = intent.getLongExtra(AlarmScheduler.EXTRA_WAKE_AT, System.currentTimeMillis())
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return finish()
        alarmId = id
        // A new notification id, not an update of the ramp's: Android does not
        // pop up a heads-up for an update, so replacing the quiet ramp notice
        // under the same id left the alarm without a banner (emulator, 24.09.).
        goForeground(alarmNotification(), keepAwakeUntil = wakeAtMillis + AFTER_WAKE_MS, id = NOTIFICATION_ID_ALARM)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID_RAMP)
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, AFTER_WAKE_MS)
        _ringing.value = true

        // Settings first, sound second: the bowl used to strike once at the
        // engine's default level before the alarm's own volume had loaded.
        scope.launch {
            val alarm = loadAlarm(id) ?: WakeAlarm(id = id, hour = 0, minute = 0)
            val atWake = WakeRamps.levels(alarm.volume, alarm.ambientVolume, alarm.ambient != null, ringing = true)
            if (generator.isPlaying) {
                // The ramp ran and is at full ramp level: take the pulse and the
                // ambient back to their wake levels so the bowl stands clear.
                val ramp = WakeRamps.levels(alarm.volume, alarm.ambientVolume, alarm.ambient != null, ringing = false)
                fadeTotalMs = 0L
                duckTo(
                    pulse = if (ramp.pulse > 0f) atWake.pulse / ramp.pulse else 0f,
                    ambient = if (ramp.ambient > 0f) atWake.ambient / ramp.ambient else 0f
                )
            } else {
                // No ramp ran (snooze, short notice, or the ramp alarm was lost):
                // the last band at wake level, faded in over a minute.
                startSound(alarm, emptyList(), fadeMs = NO_RAMP_FADE_MS, ringing = true)
            }
            chime.volume = atWake.chime
            chime.start()
            if (alarm.vibrate) startVibration()
        }
    }

    // --- Stepping the ramp back at the wake time ---

    private var duckStartedAt = 0L
    private var duckFromPulse = 1f
    private var duckFromAmbient = 1f
    private var duckToPulse = 1f
    private var duckToAmbient = 1f

    private fun duckTo(pulse: Float, ambient: Float) {
        duckFromPulse = generator.fadeScale
        duckFromAmbient = this.ambient.fadeScale
        duckToPulse = pulse
        duckToAmbient = ambient
        duckStartedAt = SystemClock.elapsedRealtime()
        handler.removeCallbacks(duckTick)
        handler.post(duckTick)
    }

    private val duckTick = object : Runnable {
        override fun run() {
            val x = ((SystemClock.elapsedRealtime() - duckStartedAt).toFloat() / DUCK_MS).coerceIn(0f, 1f)
            generator.fadeScale = duckFromPulse + (duckToPulse - duckFromPulse) * x
            ambient.fadeScale = duckFromAmbient + (duckToAmbient - duckFromAmbient) * x
            if (x < 1f) handler.postDelayed(this, FADE_TICK_MS)
        }
    }

    private fun snooze() {
        val id = alarmId
        stopSound()
        if (id != null) {
            AlarmScheduler.scheduleSnooze(
                this, id, System.currentTimeMillis() + WakeSchedule.SNOOZE_MINUTES * 60_000L
            )
        }
        finish()
    }

    private val autoStop = Runnable { finish() }

    // --- Sound ---

    private fun startSound(alarm: WakeAlarm, ramp: List<Phase>, fadeMs: Long, ringing: Boolean) {
        val levels = WakeRamps.levels(alarm.volume, alarm.ambientVolume, alarm.ambient != null, ringing)
        // After the ramp the last band keeps playing until the alarm is
        // stopped; the phase is simply long enough to outlast the auto-stop.
        val finalBand = (ramp.lastOrNull() ?: WakeRamps.ramp(alarm.rampKey, WakeSchedule.MIN_RAMP_MINUTES).last())
            .copy(durationMinutes = (AFTER_WAKE_MS / 60_000L).toInt() + 1)
        val phases = ramp + finalBand
        generator.start(
            phases,
            carrier = WakeRamps.CARRIER_HZ,
            vol = levels.pulse,
            noiseVol = 0f,
            // No dips at the phase seams: the bands glide into each other.
            transitionMs = 0,
            usage = PlaybackUsage.ALARM,
            initialFade = 0f,
            pulseDepth = WakeRamps.PULSE_DEPTH,
            beatFrequencyAt = { seconds -> WakeRamps.frequencyAt(phases, seconds) }
        )
        alarm.ambient?.let { sound ->
            ambient.setVolume(sound, levels.ambient)
            ambient.start(usage = PlaybackUsage.ALARM, initialFade = 0f)
        }
        fadeStartedAt = SystemClock.elapsedRealtime()
        fadeTotalMs = fadeMs.coerceAtLeast(1L)
        handler.removeCallbacks(fadeTick)
        handler.post(fadeTick)
    }

    private val fadeTick = object : Runnable {
        override fun run() {
            if (fadeTotalMs <= 0L) return
            val elapsed = SystemClock.elapsedRealtime() - fadeStartedAt
            val scale = WakeRamps.volumeAt(elapsed, fadeTotalMs, 1f)
            generator.fadeScale = scale
            ambient.fadeScale = scale
            if (elapsed < fadeTotalMs) handler.postDelayed(this, FADE_TICK_MS)
        }
    }

    private fun stopSound() {
        handler.removeCallbacks(fadeTick)
        handler.removeCallbacks(duckTick)
        handler.removeCallbacks(autoStop)
        generator.stop()
        ambient.stop()
        chime.stop()
        vibrator()?.cancel()
    }

    // --- Vibration (per alarm, off by default) ---

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

    private fun startVibration() {
        val v = vibrator()?.takeIf { it.hasVibrator() } ?: return
        // Two soft pulses, a long pause, repeat - a nudge rather than a buzz.
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 400, 300, 400, 2400),
            intArrayOf(0, 120, 0, 120, 0),
            0
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, PlaybackUsage.ALARM.attributes)
        }
    }

    private fun finish() {
        stopSound()
        _ringing.value = false
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun loadAlarm(id: String): WakeAlarm? =
        withContext(Dispatchers.IO) { WakeAlarmRepository(this@WakeAlarmService).get(id) }

    // --- Foreground, notifications ---

    private fun goForeground(notification: Notification, keepAwakeUntil: Long, id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(id, notification)
        }
        // The fade and auto-stop run on Handler timers, which stall while the
        // CPU sleeps; hold it awake until the alarm is certainly over.
        val timeout = (keepAwakeUntil - System.currentTimeMillis()).coerceIn(60_000L, MAX_WAKE_LOCK_MS)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DeepWave:WakeAlarm")
            .apply { acquire(timeout) }
    }

    private fun wakeTimeText(): String =
        // Follows the system 12/24-hour setting, which DateFormat.getTimeInstance ignores.
        DateFormat.getTimeFormat(this).format(Date(wakeAtMillis))

    private fun rampNotification(): Notification =
        Notification.Builder(this, CHANNEL_RAMP)
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentTitle(getString(R.string.wake_channel_alarm))
            .setContentText(getString(R.string.wake_notif_ramp, wakeTimeText()))
            .setContentIntent(openApp())
            .addAction(action(ACTION_DISMISS, R.string.wake_action_dismiss))
            .setOngoing(true)
            .build()

    private fun alarmNotification(): Notification =
        Notification.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentTitle(getString(R.string.wake_notif_title))
            .setContentText(getString(R.string.wake_notif_text, wakeTimeText()))
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(wakeScreen())
            // Shows WakeActivity over the lock screen. If the user revoked the
            // full-screen permission (Android 14+), Android falls back to a
            // heads-up notification, and the two actions below still work.
            .setFullScreenIntent(wakeScreen(), true)
            .addAction(action(ACTION_SNOOZE, R.string.wake_action_snooze, WakeSchedule.SNOOZE_MINUTES))
            .addAction(action(ACTION_DISMISS, R.string.wake_action_dismiss))
            .setOngoing(true)
            .build()

    private fun wakeScreen(): PendingIntent = PendingIntent.getActivity(
        this, 1,
        Intent(this, WakeActivity::class.java)
            .putExtra(AlarmScheduler.EXTRA_WAKE_AT, wakeAtMillis)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun action(what: String, label: Int, vararg args: Any): Notification.Action {
        val pi = PendingIntent.getService(
            this, what.hashCode(),
            Intent(this, WakeAlarmService::class.java).setAction(what),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Action.Builder(null, getString(label, *args), pi).build()
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        // The ramp must not make a sound of its own or pop up - it plays while
        // the user is still meant to be asleep.
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RAMP, getString(R.string.wake_channel_ramp), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.wake_channel_ramp_desc); setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, getString(R.string.wake_channel_alarm), NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    description = getString(R.string.wake_channel_alarm_desc)
                    // The service plays the alarm itself; a channel sound would clash.
                    setSound(null, null)
                    setShowBadge(false)
                }
        )
    }

    override fun onDestroy() {
        stopSound()
        _ringing.value = false
        wakeLock?.let { if (it.isHeld) it.release() }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        // Same process as WakeActivity, so a flow is enough to let the screen
        // close itself when the alarm ends by snooze, stop or auto-stop.
        private val _ringing = MutableStateFlow(false)
        val ringing: StateFlow<Boolean> = _ringing.asStateFlow()

        const val ACTION_RAMP = "de.binauralbeats.app.alarm.action.RAMP"
        const val ACTION_WAKE = "de.binauralbeats.app.alarm.action.WAKE"
        const val ACTION_SNOOZE = "de.binauralbeats.app.alarm.action.SNOOZE"
        const val ACTION_DISMISS = "de.binauralbeats.app.alarm.action.DISMISS"

        const val CHANNEL_RAMP = "wake_ramp"
        const val CHANNEL_ALARM = "wake_alarm"
        // 1 is AudioPlaybackService's.
        private const val NOTIFICATION_ID_RAMP = 2
        private const val NOTIFICATION_ID_ALARM = 3

        private const val AFTER_WAKE_MS = WakeSchedule.AUTO_STOP_MINUTES * 60_000L
        private const val NO_RAMP_FADE_MS = 60_000L
        private const val FADE_TICK_MS = 250L
        private const val DUCK_MS = 10_000L
        private const val MAX_WAKE_LOCK_MS = 60 * 60_000L
    }
}
