package de.binauralbeats.app.audio

import android.media.AudioFormat
import android.media.AudioTrack
import de.binauralbeats.app.data.Phase
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Lets the user hear a wake ramp in the editor: the whole ramp squeezed into
 * a few seconds, then one strike of the bowl. Plays on the media stream - it
 * is a preview, not an alarm, and must follow the volume the user is holding.
 *
 * Rendered in one go into a static AudioTrack; there is nothing to stream and
 * no thread to keep in step.
 */
class WakePreview(private val sampleRate: Int = 44100) {

    private var track: AudioTrack? = null

    fun play(ramp: List<Phase>, volume: Float) {
        stop()
        val pcm = render(ramp, volume, sampleRate)
        val t = AudioTrack.Builder()
            .setAudioAttributes(PlaybackUsage.MEDIA.attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(pcm, 0, pcm.size)
        t.play()
        track = t
    }

    fun stop() {
        track?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        track = null
    }

    companion object {
        const val RAMP_SECONDS = 8.0
        const val CHIME_SECONDS = 5.0
        private const val CARRIER_HZ = 200.0

        /**
         * The preview as 16-bit mono PCM. Pure, so its shape is testable:
         * rises from silence, pulses at the ramp's band frequencies in order,
         * ends on a bowl strike and fades to zero.
         */
        fun render(ramp: List<Phase>, volume: Float, sampleRate: Int): ShortArray {
            val rampSamples = (RAMP_SECONDS * sampleRate).toInt()
            val total = rampSamples + (CHIME_SECONDS * sampleRate).toInt()
            val out = ShortArray(total)
            val minutes = ramp.sumOf { it.durationMinutes }.coerceAtLeast(1)
            val chime = ChimeVoice(sampleRate)
            var pulsePhase = 0.0

            for (i in 0 until total) {
                var sample = 0.0
                if (i < rampSamples && ramp.isNotEmpty()) {
                    val progress = i.toDouble() / rampSamples
                    val freq = bandAt(ramp, progress * minutes)
                    pulsePhase += freq / sampleRate
                    // Raised-sine gate: the ramp's isochronic pulse without the
                    // clicks of a hard on/off at this short a time scale.
                    val gate = 0.5 - 0.5 * kotlin.math.cos(2 * PI * pulsePhase)
                    val loud = progress.pow(2.5)
                    sample = sin(2 * PI * CARRIER_HZ * i / sampleRate) * gate * loud * 0.6
                }
                if (i == rampSamples) chime.strike(1f)
                sample += chime.nextSample()
                // Last half second fades out so the preview never ends on a cut.
                val tail = ((total - i).toDouble() / (0.5 * sampleRate)).coerceAtMost(1.0)
                out[i] = (sample * tail * volume * Short.MAX_VALUE * 0.8)
                    .toInt()
                    .coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            return out
        }

        /** Band frequency at [minute] into the ramp. */
        internal fun bandAt(ramp: List<Phase>, minute: Double): Double {
            var start = 0.0
            for (p in ramp) {
                start += p.durationMinutes
                if (minute < start) return p.frequency.toDouble()
            }
            return ramp.last().frequency.toDouble()
        }
    }
}
