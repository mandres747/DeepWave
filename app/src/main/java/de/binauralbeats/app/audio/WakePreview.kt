package de.binauralbeats.app.audio

import android.media.AudioFormat
import android.media.AudioTrack
import de.binauralbeats.app.alarm.WakeRamps
import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.data.ToneType
import kotlin.math.pow

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

        /**
         * The preview as 16-bit mono PCM. Pure, so its shape is testable:
         * rises from silence, pulses through the ramp's bands in order, then
         * the pulse steps back and the bowl strikes, and it fades to zero.
         * Built from the same pieces the alarm plays - ToneVoice, the ramp's
         * carrier, depth, glide and levels, ChimeVoice - so the preview is
         * not a nicer-sounding second implementation (the first one was).
         */
        fun render(ramp: List<Phase>, volume: Float, sampleRate: Int): ShortArray {
            val rampSamples = (RAMP_SECONDS * sampleRate).toInt()
            val total = rampSamples + (CHIME_SECONDS * sampleRate).toInt()
            val out = ShortArray(total)
            val rampSeconds = ramp.sumOf { it.durationMinutes } * 60.0
            val during = WakeRamps.levels(volume, 0f, hasAmbient = false, ringing = false)
            val atWake = WakeRamps.levels(volume, 0f, hasAmbient = false, ringing = true)
            val tone = ToneVoice(sampleRate)
            val stereo = FloatArray(2)
            val chime = ChimeVoice(sampleRate)
            val duckSamples = (0.5 * sampleRate).toInt()

            for (i in 0 until total) {
                val pulseLevel: Double
                val beat: Double
                if (i < rampSamples) {
                    val progress = i.toDouble() / rampSamples
                    beat = if (ramp.isEmpty()) 0.0 else WakeRamps.frequencyAt(ramp, progress * rampSeconds)
                    pulseLevel = during.pulse * progress.pow(2.5)
                } else {
                    beat = ramp.lastOrNull()?.frequency?.toDouble() ?: 0.0
                    val x = ((i - rampSamples).toDouble() / duckSamples).coerceAtMost(1.0)
                    pulseLevel = during.pulse + (atWake.pulse - during.pulse) * x
                }
                tone.next(WakeRamps.CARRIER_HZ.toDouble(), beat, ToneType.ISOCHRONIC, WakeRamps.PULSE_DEPTH.toDouble(), stereo)
                if (i == rampSamples) chime.strike(1f)
                val sample = stereo[0] * pulseLevel + chime.nextSample() * atWake.chime
                // Last half second fades out so the preview never ends on a cut.
                val tail = ((total - i).toDouble() / (0.5 * sampleRate)).coerceAtMost(1.0)
                out[i] = (sample * tail * Short.MAX_VALUE)
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
