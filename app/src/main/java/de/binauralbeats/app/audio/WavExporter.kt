package de.binauralbeats.app.audio

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import de.binauralbeats.app.R
import de.binauralbeats.app.data.BackgroundNoise
import de.binauralbeats.app.data.ModulationType
import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.data.ToneType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class WavExporter {

    data class ExportResult(val success: Boolean, val filename: String, val error: String? = null)

    suspend fun export(
        context: Context,
        phases: List<Phase>,
        carrier: Float,
        volume: Float,
        noiseVolume: Float,
        transitionMs: Int,
        filename: String,
        onProgress: (Float) -> Unit = {}
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 44100
            val totalSamples = phases.sumOf { it.durationMinutes } * 60L * sampleRate
            val dataSize = totalSamples * 4 // 16-bit stereo = 4 bytes per sample frame
            val fadeSamples = (sampleRate * transitionMs / 1000.0).toInt()

            val buffer = ByteBuffer.allocate(dataSize.toInt()).order(ByteOrder.LITTLE_ENDIAN)

            var sampleCounter = 0L
            var pinkState = FloatArray(7)
            var lastBrown = 0f

            for (phaseIdx in phases.indices) {
                val phase = phases[phaseIdx]
                val phaseDurationSec = phase.durationMinutes * 60.0
                val phaseSamples = (phaseDurationSec * sampleRate).toLong()

                for (i in 0 until phaseSamples) {
                    val t = (sampleCounter + i).toDouble() / sampleRate
                    val phaseT = i.toDouble() / sampleRate

                    val beatFreq = applyModulation(phase.frequency, phase.modulation, phaseT, phaseDurationSec)

                    var leftSample: Float
                    var rightSample: Float

                    when (phase.toneType) {
                        ToneType.BINAURAL -> {
                            leftSample = sin(2 * PI * carrier * t).toFloat()
                            rightSample = sin(2 * PI * (carrier + beatFreq) * t).toFloat()
                        }
                        ToneType.ISOCHRONIC -> {
                            val tone = sin(2 * PI * carrier * t).toFloat()
                            val pulse = if (sin(2 * PI * beatFreq * t) > 0) 1f else 0f
                            leftSample = tone * pulse
                            rightSample = leftSample
                        }
                    }

                    when (phase.background) {
                        BackgroundNoise.PINK -> {
                            val noise = generatePinkNoise(pinkState) * noiseVolume
                            leftSample += noise
                            rightSample += noise
                        }
                        BackgroundNoise.BROWN -> {
                            lastBrown = generateBrownNoise(lastBrown)
                            val noise = lastBrown * noiseVolume
                            leftSample += noise
                            rightSample += noise
                        }
                        BackgroundNoise.NONE -> {}
                    }

                    val fadeEnvelope = if (fadeSamples > 0) when {
                        i < fadeSamples -> i.toFloat() / fadeSamples
                        phaseSamples - i < fadeSamples -> (phaseSamples - i).toFloat() / fadeSamples
                        else -> 1f
                    } else 1f

                    val globalFade = if (fadeSamples > 0) when {
                        phaseIdx == 0 && i < fadeSamples -> i.toFloat() / fadeSamples
                        phaseIdx == phases.lastIndex && phaseSamples - i < fadeSamples ->
                            (phaseSamples - i).toFloat() / fadeSamples
                        else -> 1f
                    } else 1f

                    val envelope = fadeEnvelope * globalFade * volume
                    leftSample = (leftSample * envelope).coerceIn(-1f, 1f)
                    rightSample = (rightSample * envelope).coerceIn(-1f, 1f)

                    buffer.putShort((leftSample * Short.MAX_VALUE).toInt().toShort())
                    buffer.putShort((rightSample * Short.MAX_VALUE).toInt().toShort())

                    if (i % (sampleRate * 10) == 0L) {
                        val progress = (sampleCounter + i).toFloat() / totalSamples
                        onProgress(progress)
                    }
                }
                sampleCounter += phaseSamples
            }

            onProgress(1f)

            val wavFilename = if (filename.endsWith(".wav")) filename else "$filename.wav"
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, wavFilename)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/BinauralBeats")
            }

            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext ExportResult(false, wavFilename, context.getString(R.string.export_file_error))

            context.contentResolver.openOutputStream(uri)?.use { output ->
                val dos = DataOutputStream(output)
                val pcmData = buffer.array()

                // WAV header
                dos.writeBytes("RIFF")
                dos.writeInt(Integer.reverseBytes(36 + pcmData.size))
                dos.writeBytes("WAVE")
                dos.writeBytes("fmt ")
                dos.writeInt(Integer.reverseBytes(16))
                dos.writeShort(java.lang.Short.reverseBytes(1).toInt()) // PCM
                dos.writeShort(java.lang.Short.reverseBytes(2).toInt()) // Stereo
                dos.writeInt(Integer.reverseBytes(sampleRate))
                dos.writeInt(Integer.reverseBytes(sampleRate * 4)) // byte rate
                dos.writeShort(java.lang.Short.reverseBytes(4).toInt()) // block align
                dos.writeShort(java.lang.Short.reverseBytes(16).toInt()) // bits per sample
                dos.writeBytes("data")
                dos.writeInt(Integer.reverseBytes(pcmData.size))
                dos.write(pcmData)
                dos.flush()
            }

            ExportResult(true, wavFilename)
        } catch (e: Exception) {
            ExportResult(false, filename, e.message)
        }
    }

    private fun applyModulation(
        baseFreq: Float, type: ModulationType, timeInPhase: Double, phaseDuration: Double
    ): Float = when (type) {
        ModulationType.STATIC -> baseFreq
        ModulationType.BREATHING -> baseFreq + (baseFreq * 0.15f * sin(2 * PI * 0.1 * timeInPhase)).toFloat()
        ModulationType.PULSE -> baseFreq * if (sin(2 * PI * 4 * timeInPhase) > 0) 1f else 0.3f
        ModulationType.DYNAMIC -> baseFreq * (1f - 0.3f * (timeInPhase / phaseDuration).toFloat())
        ModulationType.SWEEP -> baseFreq + (baseFreq * 0.25f * sin(2 * PI * 0.05 * timeInPhase)).toFloat()
    }

    private fun generatePinkNoise(state: FloatArray): Float {
        val white = Random.nextFloat() * 2f - 1f
        state[0] = 0.99886f * state[0] + white * 0.0555179f
        state[1] = 0.99332f * state[1] + white * 0.0750759f
        state[2] = 0.96900f * state[2] + white * 0.1538520f
        state[3] = 0.86650f * state[3] + white * 0.3104856f
        state[4] = 0.55000f * state[4] + white * 0.5329522f
        state[5] = -0.7616f * state[5] - white * 0.0168980f
        val pink = state[0] + state[1] + state[2] + state[3] + state[4] + state[5] + state[6] + white * 0.5362f
        state[6] = white * 0.115926f
        return pink * 0.11f
    }

    private fun generateBrownNoise(lastValue: Float): Float {
        val white = Random.nextFloat() * 2f - 1f
        return (lastValue + 0.02f * white).coerceIn(-1f, 1f)
    }
}
