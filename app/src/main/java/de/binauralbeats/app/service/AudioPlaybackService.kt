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
import android.os.IBinder
import de.binauralbeats.app.MainActivity
import de.binauralbeats.app.R
import de.binauralbeats.app.audio.BinauralGenerator
import de.binauralbeats.app.data.Phase

class AudioPlaybackService : Service() {

    inner class LocalBinder : Binder() {
        val service: AudioPlaybackService get() = this@AudioPlaybackService
    }

    private val binder = LocalBinder()
    val generator = BinauralGenerator()

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
        startForegroundService(Intent(this, AudioPlaybackService::class.java))

        val notification = buildNotification(getString(R.string.notif_title), getString(R.string.notif_playing))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        generator.start(phases, carrier, volume, noiseVolume, transitionMs)
    }

    fun stopPlayback() {
        generator.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        generator.stop()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "binaural_playback"
        const val NOTIFICATION_ID = 1
    }
}
