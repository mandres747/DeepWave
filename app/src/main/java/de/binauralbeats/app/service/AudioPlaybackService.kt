package de.binauralbeats.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import de.binauralbeats.app.MainActivity
import de.binauralbeats.app.R
import de.binauralbeats.app.audio.AmbientEngine
import de.binauralbeats.app.audio.RhythmEngine
import de.binauralbeats.app.audio.SpokenCues
import de.binauralbeats.app.audio.BinauralGenerator
import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.data.RhythmStep
import de.binauralbeats.app.data.stepAt

class AudioPlaybackService : Service() {

    inner class LocalBinder : Binder() {
        val service: AudioPlaybackService get() = this@AudioPlaybackService
    }

    private val binder = LocalBinder()
    val generator = BinauralGenerator()
    val ambient = AmbientEngine()
    val rhythm = RhythmEngine()
    private val cues by lazy { SpokenCues(this) }

    // Lets a ViewModel recreated after process death (Activity/service rebind while
    // the foreground service keeps playing) restore what is actually running.
    data class PlaybackParams(
        val phases: List<Phase>,
        val carrier: Float,
        val volume: Float,
        val noiseVolume: Float,
        val transitionMs: Int
    )

    var lastPlaybackParams: PlaybackParams? = null
        private set

    // Sleep timer lives here so it survives screen-off while the foreground
    // service keeps playing.
    private val handler = Handler(Looper.getMainLooper())
    private var sleepTimerEndAt = 0L
    var onSleepTimerTick: ((remainingSeconds: Int) -> Unit)? = null
    var onSleepTimerFinished: (() -> Unit)? = null

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun startPlayback(
        phases: List<Phase>,
        carrier: Float,
        volume: Float,
        noiseVolume: Float = 0.15f,
        transitionMs: Int = 500
    ) {
        lastPlaybackParams = PlaybackParams(phases, carrier, volume, noiseVolume, transitionMs)
        ensureForeground(getString(R.string.notif_title), getString(R.string.notif_playing))
        generator.start(phases, carrier, volume, noiseVolume, transitionMs)
    }

    fun stopPlayback() {
        generator.stop()
        maybeExitForeground()
    }

    fun startAmbient() {
        ensureForeground(getString(R.string.notif_title), getString(R.string.notif_ambient))
        ambient.start()
    }

    fun stopAmbient() {
        ambient.stop()
        maybeExitForeground()
    }

    fun startRhythm() {
        ensureForeground(getString(R.string.notif_title), getString(R.string.notif_rhythm))
        rhythm.start()
    }

    fun stopRhythm() {
        handler.removeCallbacks(rhythmProgramTick)
        rhythmProgram = emptyList()
        rhythm.stop()
        maybeExitForeground()
    }

    // --- Rhythm program ---
    //
    // Lives here rather than in the ViewModel so a running program survives
    // screen-off and Activity recreation, the same reason the sleep timer is
    // here. The engine itself stays dumb: it is handed a new pulse pattern at
    // each step boundary and never learns what a program is.

    private var rhythmProgram: List<RhythmStep> = emptyList()
    private var rhythmProgramStartedAt = 0L
    private var rhythmCurrentStep: RhythmStep? = null

    var onRhythmStepChanged: ((step: RhythmStep?, remainingSeconds: Int) -> Unit)? = null

    /** Set from the ViewModel; announcements are off unless the user asked. */
    var spokenCuesEnabled = false

    fun startRhythmProgram(steps: List<RhythmStep>) {
        handler.removeCallbacks(rhythmProgramTick)
        if (spokenCuesEnabled) cues.start()
        rhythmProgram = steps
        rhythmProgramStartedAt = SystemClock.elapsedRealtime()
        rhythmCurrentStep = null
        ensureForeground(getString(R.string.notif_title), getString(R.string.notif_rhythm))
        handler.post(rhythmProgramTick)
    }

    private val rhythmProgramTick = object : Runnable {
        override fun run() {
            val elapsedSec =
                ((SystemClock.elapsedRealtime() - rhythmProgramStartedAt) / 1000L).toInt()
            val step = stepAt(rhythmProgram, elapsedSec)

            if (step == null) {
                rhythmCurrentStep = null
                if (spokenCuesEnabled) cues.say(getString(R.string.cue_program_done))
                onRhythmStepChanged?.invoke(null, 0)
                stopRhythm()
                return
            }

            if (step !== rhythmCurrentStep) {
                rhythmCurrentStep = step
                rhythm.pattern = step.pulses
                if (!rhythm.isPlaying) rhythm.start()
                if (spokenCuesEnabled) cues.say(getString(R.string.cue_tempo, step.bpm))
            }

            val totalSec = rhythmProgram.sumOf { it.durationMinutes * 60 }
            onRhythmStepChanged?.invoke(step, (totalSec - elapsedSec).coerceAtLeast(0))
            handler.postDelayed(this, 1000L)
        }
    }

    // --- Sleep timer ---

    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        sleepTimerEndAt = SystemClock.elapsedRealtime() + minutes * 60_000L
        handler.post(timerTick)
    }

    fun cancelSleepTimer() {
        handler.removeCallbacks(timerTick)
        handler.removeCallbacks(fadeStep)
        sleepTimerEndAt = 0L
        generator.fadeScale = 1f
        ambient.fadeScale = 1f
        rhythm.fadeScale = 1f
    }

    private val timerTick = object : Runnable {
        override fun run() {
            val remainingMs = sleepTimerEndAt - SystemClock.elapsedRealtime()
            if (remainingMs <= 0L) {
                onSleepTimerTick?.invoke(0)
                fadeStartedAt = SystemClock.elapsedRealtime()
                handler.post(fadeStep)
            } else {
                onSleepTimerTick?.invoke((remainingMs / 1000L).toInt() + 1)
                handler.postDelayed(this, 1000L)
            }
        }
    }

    private var fadeStartedAt = 0L

    private val fadeStep = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.elapsedRealtime() - fadeStartedAt
            val scale = (1f - elapsed.toFloat() / FADE_DURATION_MS).coerceIn(0f, 1f)
            generator.fadeScale = scale
            ambient.fadeScale = scale
            rhythm.fadeScale = scale
            if (scale > 0f) {
                handler.postDelayed(this, 250L)
            } else {
                generator.stop()
                ambient.stop()
                rhythm.stop()
                sleepTimerEndAt = 0L
                maybeExitForeground()
                onSleepTimerFinished?.invoke()
            }
        }
    }

    // --- Foreground lifecycle (shared by both engines) ---

    private fun ensureForeground(title: String, text: String) {
        startForegroundService(Intent(this, AudioPlaybackService::class.java))
        val notification = buildNotification(title, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun maybeExitForeground() {
        if (!generator.isPlaying && !ambient.isPlaying && !rhythm.isPlaying) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    fun updateNotification(presetName: String, phaseInfo: String) {
        val notification = buildNotification(presetName, phaseInfo)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(rhythmProgramTick)
        cancelSleepTimer()
        generator.stop()
        ambient.stop()
        rhythm.stop()
        cues.shutdown()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "binaural_playback"
        const val NOTIFICATION_ID = 1
        const val FADE_DURATION_MS = 15_000L
    }
}
