package de.binauralbeats.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import de.binauralbeats.app.data.AmbientSound
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural ambient sound mixer. All layers are synthesized from filtered
 * noise in real time — no audio assets, mirroring BinauralGenerator's design.
 * Runs on its own AudioTrack so it can play independently of a binaural
 * session or layered on top of one.
 */
class AmbientEngine(
    private val sampleRate: Int = 44100
) {
    private var audioTrack: AudioTrack? = null
    private var engineThread: Thread? = null

    @Volatile
    var isPlaying = false
        private set

    // Sleep-timer fade; multiplied into every sample. Float writes are atomic.
    @Volatile
    var fadeScale = 1f

    private val volumes = FloatArray(AmbientSound.entries.size)

    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(sampleRate * 2)

    fun setVolume(sound: AmbientSound, volume: Float) {
        volumes[sound.ordinal] = volume.coerceIn(0f, 1f)
    }

    fun getVolume(sound: AmbientSound): Float = volumes[sound.ordinal]

    fun hasActiveLayer(): Boolean = volumes.any { it > 0.001f }

    fun start() {
        stop()
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
        fadeScale = 1f
    }

    private fun generateAudio() {
        val samplesPerBuffer = bufferSize / 4
        val buffer = ShortArray(samplesPerBuffer * 2)
        var sampleCounter = 0L

        // Shared pink noise source (ocean, wind, pink layer)
        val pinkState = FloatArray(7)

        // Per-layer filter and envelope state
        val rainLp = FloatArray(2)
        val rainDropEnv = FloatArray(2)
        var windLp = 0f
        var fireBrown = 0f
        var fireCrackleEnv = 0f
        val streamLp = FloatArray(2)
        var brownLast = 0f

        while (isPlaying) {
            for (i in 0 until samplesPerBuffer) {
                val t = (sampleCounter + i).toDouble() / sampleRate
                var left = 0f
                var right = 0f

                val needPink = volumes[AmbientSound.OCEAN.ordinal] > 0.001f ||
                    volumes[AmbientSound.WIND.ordinal] > 0.001f ||
                    volumes[AmbientSound.PINK.ordinal] > 0.001f
                val pink = if (needPink) nextPink(pinkState) else 0f

                var vol = volumes[AmbientSound.RAIN.ordinal]
                if (vol > 0.001f) {
                    for (ch in 0..1) {
                        val white = Random.nextFloat() * 2f - 1f
                        rainLp[ch] += 0.12f * (white - rainLp[ch])
                        if (Random.nextFloat() < 0.00015f) {
                            rainDropEnv[ch] = 0.5f + Random.nextFloat() * 0.5f
                        }
                        rainDropEnv[ch] *= 0.9993f
                        val sample = (rainLp[ch] * 1.3f + white * rainDropEnv[ch] * 0.6f) * vol
                        if (ch == 0) left += sample else right += sample
                    }
                }

                vol = volumes[AmbientSound.OCEAN.ordinal]
                if (vol > 0.001f) {
                    val wave = ((sin(2 * PI * 0.06 * t) + 1.0) / 2.0).pow(2.2).toFloat()
                    val ripple = ((sin(2 * PI * 0.13 * t + 0.9) + 1.0) / 2.0).toFloat()
                    val sample = pink * (0.2f + 0.65f * wave + 0.15f * ripple) * 1.6f * vol
                    left += sample
                    right += sample
                }

                vol = volumes[AmbientSound.WIND.ordinal]
                if (vol > 0.001f) {
                    val gust = ((sin(2 * PI * 0.05 * t + 1.7) + 1.0) / 2.0).toFloat()
                    val coeff = 0.02f + 0.06f * gust
                    windLp += coeff * (pink - windLp)
                    val sample = windLp * (0.6f + 0.8f * gust) * 5f * vol
                    left += sample
                    right += sample
                }

                vol = volumes[AmbientSound.FIRE.ordinal]
                if (vol > 0.001f) {
                    val white = Random.nextFloat() * 2f - 1f
                    fireBrown = (fireBrown + 0.02f * white).coerceIn(-1f, 1f)
                    if (Random.nextFloat() < 0.00008f) {
                        fireCrackleEnv = 0.7f + Random.nextFloat() * 0.3f
                    }
                    fireCrackleEnv *= 0.995f
                    val sample = (fireBrown * 0.5f + white * fireCrackleEnv) * vol
                    left += sample
                    right += sample
                }

                vol = volumes[AmbientSound.STREAM.ordinal]
                if (vol > 0.001f) {
                    for (ch in 0..1) {
                        val white = Random.nextFloat() * 2f - 1f
                        streamLp[ch] += 0.15f * (white - streamLp[ch])
                        val highpassed = white - streamLp[ch]
                        val babbleFreq = if (ch == 0) 1.1 else 1.3
                        val babble = ((sin(2 * PI * babbleFreq * t) + 1.0) / 2.0).toFloat()
                        val sample = highpassed * (0.7f + 0.3f * babble) * 0.7f * vol
                        if (ch == 0) left += sample else right += sample
                    }
                }

                vol = volumes[AmbientSound.WHITE.ordinal]
                if (vol > 0.001f) {
                    left += (Random.nextFloat() * 2f - 1f) * 0.35f * vol
                    right += (Random.nextFloat() * 2f - 1f) * 0.35f * vol
                }

                vol = volumes[AmbientSound.PINK.ordinal]
                if (vol > 0.001f) {
                    left += pink * vol
                    right += pink * vol
                }

                vol = volumes[AmbientSound.BROWN.ordinal]
                if (vol > 0.001f) {
                    val white = Random.nextFloat() * 2f - 1f
                    brownLast = (brownLast + 0.02f * white).coerceIn(-1f, 1f)
                    left += brownLast * vol
                    right += brownLast * vol
                }

                val scale = fadeScale * 0.9f
                left = (left * scale).coerceIn(-1f, 1f)
                right = (right * scale).coerceIn(-1f, 1f)

                buffer[i * 2] = (left * Short.MAX_VALUE).toInt().toShort()
                buffer[i * 2 + 1] = (right * Short.MAX_VALUE).toInt().toShort()
            }

            audioTrack?.write(buffer, 0, samplesPerBuffer * 2)
            sampleCounter += samplesPerBuffer
        }
    }

    private fun nextPink(state: FloatArray): Float {
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
}
