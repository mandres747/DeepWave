package de.binauralbeats.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import de.binauralbeats.app.data.RhythmPulse
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

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

        // Samples until the next pulse fires, carried in double precision so a
        // tempo like 90 BPM (666.67 ms) does not drift over a long session.
        var samplesToNextPulse = 0.0
        var pulseIndex = 0

        // Currently ringing click, if any.
        var clickAge = -1
        var clickFrequency = 0.0
        var clickDecay = 0.0
        var clickGain = 0f

        while (isPlaying) {
            val currentPattern = pattern
            if (currentPattern.isEmpty()) {
                java.util.Arrays.fill(buffer, 0)
                writeBuffer(buffer)
                continue
            }

            for (i in 0 until samplesPerBuffer) {
                if (samplesToNextPulse <= 0.0) {
                    val pulse = currentPattern[pulseIndex % currentPattern.size]
                    pulseIndex = (pulseIndex + 1) % currentPattern.size
                    samplesToNextPulse += pulse.intervalMillis / 1000.0 * sampleRate

                    // Accents sit a fifth above and ring slightly longer, which
                    // reads as emphasis without being louder enough to startle.
                    clickAge = 0
                    clickFrequency = if (pulse.accented) 1320.0 else 880.0
                    clickDecay = if (pulse.accented) 42.0 else 55.0
                    clickGain = if (pulse.accented) 1f else 0.72f
                }
                samplesToNextPulse -= 1.0

                var sample = 0f
                if (clickAge >= 0) {
                    val age = clickAge.toDouble() / sampleRate
                    val envelope = exp(-age * clickDecay).toFloat()
                    if (envelope < 0.0005f) {
                        clickAge = -1
                    } else {
                        val body = sin(2 * PI * clickFrequency * age).toFloat()
                        // A very short noise transient gives the click an edge
                        // to sit on; without it a pure sine reads as a beep.
                        val transient = if (clickAge < sampleRate / 400) {
                            (Random.nextFloat() * 2f - 1f) * 0.35f
                        } else {
                            0f
                        }
                        sample = (body + transient) * envelope * clickGain
                        clickAge++
                    }
                }

                val value = (sample * volume * fadeScale * Short.MAX_VALUE * 0.6f)
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
