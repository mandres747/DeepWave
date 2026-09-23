package de.binauralbeats.app.audio

import android.media.AudioAttributes

/**
 * Which system stream an engine plays on. Everything used to be hard-wired to
 * media; the wake alarm needs the alarm stream instead, so it follows the
 * alarm volume and gets through Do Not Disturb - on the media stream an alarm
 * stays silent whenever the media volume was turned down the night before.
 * See docs/KLANGWECKER_KONZEPT.md, section 3.
 */
enum class PlaybackUsage(val attributes: AudioAttributes) {
    MEDIA(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    ),
    ALARM(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    )
}
