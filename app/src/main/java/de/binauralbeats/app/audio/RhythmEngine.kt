package de.binauralbeats.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import de.binauralbeats.app.data.RhythmPulse

/**
 * Audible tempo track: a soft wood-block pulse on its own AudioTrack, so it
 * can run over a binaural session, over the ambient mixer, or entirely on its
 * own. Synthesized rather than sampled, mirroring BinauralGenerator and
 * AmbientEngine - the app ships no audio assets.
 *
 * The engine has no notion of "metronome" or "breathing cue". It plays a list
 * of pulses and cycles it forever; equal intervals sound like a metronome,
 * unequal ones like a breath. See RhythmPattern for how those lists are built.
 */
class RhythmEngine(
    private val sampleRate: Int = 44100
) {
    private var audioTrack: AudioTrack? = null
    private var engineThread: Thread? = null

    @Volatile
    var isPlaying = false
        private set

    /** Replaced wholesale while playing; the next pulse picks up the change. */
    @Volatile
    var pattern: List<RhythmPulse> = emptyList()

    @Volatile
    var volume = 0.6f

    /** Sleep-timer fade, multiplied into every sample. Float writes are atomic. */
    @Volatile
    var fadeScale = 1f

    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(sampleRate * 2)

    fun start() {
        stop()
        if (pattern.isEmpty()) return
        isPlaying = true
        fadeScale = 1f

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        engineThread = Thread { generateAudio() }.apply {
            priority = Thread.MAX_PRIORITY
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

    private fun generateAudio() {
        val samplesPerBuffer = bufferSize / 4
        val buffer = ShortArray(samplesPerBuffer * 2)

        val scheduler = PulseScheduler(sampleRate)
        val voice = ClickVoice(sampleRate)

        while (isPlaying) {
            val currentPattern = pattern
            if (currentPattern.isEmpty()) {
                java.util.Arrays.fill(buffer, 0)
                if (!writeBuffer(buffer)) return
                continue
            }

            for (i in 0 until samplesPerBuffer) {
                scheduler.advance(currentPattern)?.let { voice.trigger(it.accented) }

                val value = (voice.nextSample() * volume * fadeScale * Short.MAX_VALUE * 0.6f)
                    .toInt()
                    .coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
                buffer[i * 2] = value
                buffer[i * 2 + 1] = value
            }

            if (!writeBuffer(buffer)) return
        }
    }

    /** Returns false when the track is gone and the thread should end. */
    private fun writeBuffer(buffer: ShortArray): Boolean {
        val track = audioTrack ?: return false
        return try {
            track.write(buffer, 0, buffer.size)
            true
        } catch (_: Exception) {
            false
        }
    }
}
