package de.binauralbeats.app.audio

import android.media.AudioFormat
import android.media.AudioTrack

/**
 * Strikes a singing bowl every [intervalMs] on its own AudioTrack, a little
 * stronger each time, until stopped. The wake alarm's sound from the wake time
 * on; the ramp keeps playing underneath.
 */
class ChimeEngine(
    private val sampleRate: Int = 44100,
    private val intervalMs: Long = 20_000L
) {
    private var audioTrack: AudioTrack? = null
    private var engineThread: Thread? = null

    @Volatile
    var isPlaying = false
        private set

    @Volatile
    var volume = 0.8f

    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(sampleRate / 2)

    fun start(usage: PlaybackUsage = PlaybackUsage.ALARM) {
        stop()
        isPlaying = true
        val track = AudioTrack.Builder()
            .setAudioAttributes(usage.attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack = track
        track.play()
        // The thread gets its own track reference: after a stop/start the old
        // thread must not write into the new track (the race fixed in 1.3.1
        // for BinauralGenerator).
        engineThread = Thread { generate(track) }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        isPlaying = false
        engineThread?.interrupt()
        engineThread = null
        audioTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        audioTrack = null
    }

    private fun generate(track: AudioTrack) {
        val buffer = ShortArray(bufferSize / 2)
        val voice = ChimeVoice(sampleRate)
        val samplesPerStrike = intervalMs * sampleRate / 1000L
        var sinceStrike = samplesPerStrike // strike on the first sample
        var strikes = 0

        while (isPlaying && !Thread.currentThread().isInterrupted) {
            for (i in buffer.indices) {
                if (sinceStrike >= samplesPerStrike) {
                    voice.strike(strengthFor(strikes++))
                    sinceStrike = 0
                }
                sinceStrike++
                buffer[i] = (voice.nextSample() * volume * Short.MAX_VALUE * 0.9f)
                    .toInt()
                    .coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            try {
                if (track.write(buffer, 0, buffer.size) < 0) return
            } catch (_: Exception) {
                return
            }
        }
    }

    companion object {
        /** First strike at half strength, full from the fifth on. */
        fun strengthFor(strike: Int): Float = (0.5f + strike * 0.125f).coerceAtMost(1f)
    }
}
