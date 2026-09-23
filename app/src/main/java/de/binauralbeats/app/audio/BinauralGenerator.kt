package de.binauralbeats.app.audio

import android.media.AudioFormat
import android.media.AudioTrack
import androidx.annotation.StringRes
import de.binauralbeats.app.R
import de.binauralbeats.app.data.BackgroundNoise
import de.binauralbeats.app.data.ModulationType
import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.data.ToneType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class BinauralGenerator(
    private val sampleRate: Int = 44100
) {
    private var audioTrack: AudioTrack? = null
    private var generatorThread: Thread? = null

    @Volatile
    var isPlaying = false
        private set

    @Volatile
    var isPaused = false
        private set

    // Sleep-timer fade; multiplied into the envelope. Float writes are atomic.
    @Volatile
    var fadeScale = 1f

    @Volatile
    var currentPhaseIndex = 0
        private set

    @Volatile
    private var elapsedSeconds = 0.0

    @Volatile
    private var totalElapsedSeconds = 0.0

    var onPhaseChanged: ((Int) -> Unit)? = null
    var onProgressUpdate: ((phaseIndex: Int, phaseProgress: Float, totalProgress: Float) -> Unit)? = null
    var onCompleted: (() -> Unit)? = null

    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(sampleRate * 2)

    fun start(
        phases: List<Phase>,
        carrier: Float = 200f,
        vol: Float = 0.7f,
        noiseVol: Float = 0.15f,
        transitionMs: Int = 500,
        usage: PlaybackUsage = PlaybackUsage.MEDIA,
        initialFade: Float = 1f
    ) {
        stop()
        isPlaying = true
        isPaused = false
        fadeScale = initialFade
        currentPhaseIndex = 0
        elapsedSeconds = 0.0
        totalElapsedSeconds = 0.0

        val track = AudioTrack.Builder()
            .setAudioAttributes(usage.attributes)
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

        // The thread gets its own track instead of reading the field: after a
        // restart the field already points at the next session's track.
        generatorThread = Thread {
            generateAudio(track, phases, carrier, vol, noiseVol, transitionMs)
        }.apply {
            priority = Thread.MAX_PRIORITY
            isDaemon = true
            start()
        }
    }

    fun pause() {
        isPaused = true
        audioTrack?.pause()
    }

    fun resume() {
        isPaused = false
        audioTrack?.play()
    }

    fun stop() {
        isPlaying = false
        isPaused = false
        val thread = generatorThread
        generatorThread = null
        thread?.interrupt()
        audioTrack?.let {
            try {
                // stop() unblocks a pending write(); let the thread leave before
                // the native track is released underneath it.
                it.stop()
                if (thread !== Thread.currentThread()) thread?.join(200)
                it.release()
            } catch (_: Exception) {}
        }
        audioTrack = null
    }

    private fun generateAudio(
        track: AudioTrack,
        phases: List<Phase>,
        carrier: Float,
        vol: Float,
        noiseVol: Float,
        transitionMs: Int
    ) {
        val totalDurationSec = phases.sumOf { it.durationMinutes } * 60.0
        val samplesPerBuffer = bufferSize / 4
        val buffer = ShortArray(samplesPerBuffer * 2)
        var sampleCounter = 0L
        val fadeSamples = (sampleRate * transitionMs / 1000.0).toInt()

        var pinkState = FloatArray(7)
        var lastBrown = 0f

        // isPlaying alone is not enough: start() sets it back to true right
        // after stopping, so an old thread would keep running. The interrupt
        // flag belongs to this thread only.
        val thread = Thread.currentThread()
        fun active() = isPlaying && !thread.isInterrupted

        for (phaseIdx in phases.indices) {
            if (!active()) return
            currentPhaseIndex = phaseIdx
            onPhaseChanged?.invoke(phaseIdx)

            val phase = phases[phaseIdx]
            val phaseDurationSec = phase.durationMinutes * 60.0
            val phaseSamples = (phaseDurationSec * sampleRate).toLong()
            var phaseSampleCount = 0L
            elapsedSeconds = 0.0

            while (phaseSampleCount < phaseSamples && active()) {
                if (isPaused) {
                    // stop() while paused interrupts this sleep; uncaught, the
                    // exception killed the whole app (Play Vitals, 1.3.0).
                    try {
                        Thread.sleep(50)
                    } catch (_: InterruptedException) {
                        return
                    }
                    continue
                }

                val samplesToGenerate = minOf(
                    samplesPerBuffer.toLong(),
                    phaseSamples - phaseSampleCount
                ).toInt()

                for (i in 0 until samplesToGenerate) {
                    val t = (sampleCounter + i).toDouble() / sampleRate
                    val phaseT = (phaseSampleCount + i).toDouble() / sampleRate

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
                            val noise = generatePinkNoise(pinkState) * noiseVol
                            leftSample += noise
                            rightSample += noise
                        }
                        BackgroundNoise.BROWN -> {
                            lastBrown = generateBrownNoise(lastBrown)
                            val noise = lastBrown * noiseVol
                            leftSample += noise
                            rightSample += noise
                        }
                        BackgroundNoise.NONE -> {}
                    }

                    val fadeEnvelope = if (fadeSamples > 0) when {
                        phaseSampleCount + i < fadeSamples ->
                            (phaseSampleCount + i).toFloat() / fadeSamples
                        phaseSamples - (phaseSampleCount + i) < fadeSamples ->
                            (phaseSamples - (phaseSampleCount + i)).toFloat() / fadeSamples
                        else -> 1f
                    } else 1f

                    val globalFade = if (fadeSamples > 0) when {
                        phaseIdx == 0 && phaseSampleCount + i < fadeSamples ->
                            (phaseSampleCount + i).toFloat() / fadeSamples
                        phaseIdx == phases.lastIndex && phaseSamples - (phaseSampleCount + i) < fadeSamples ->
                            (phaseSamples - (phaseSampleCount + i)).toFloat() / fadeSamples
                        else -> 1f
                    } else 1f

                    val envelope = fadeEnvelope * globalFade * vol * fadeScale

                    leftSample = (leftSample * envelope).coerceIn(-1f, 1f)
                    rightSample = (rightSample * envelope).coerceIn(-1f, 1f)

                    buffer[i * 2] = (leftSample * Short.MAX_VALUE).toInt().toShort()
                    buffer[i * 2 + 1] = (rightSample * Short.MAX_VALUE).toInt().toShort()
                }

                track.write(buffer, 0, samplesToGenerate * 2)
                sampleCounter += samplesToGenerate
                phaseSampleCount += samplesToGenerate
                elapsedSeconds = phaseSampleCount.toDouble() / sampleRate
                totalElapsedSeconds = calculateTotalElapsed(phases, phaseIdx, elapsedSeconds)

                val phaseProgress = (elapsedSeconds / phaseDurationSec).toFloat().coerceIn(0f, 1f)
                val totalProgress = (totalElapsedSeconds / totalDurationSec).toFloat().coerceIn(0f, 1f)
                onProgressUpdate?.invoke(phaseIdx, phaseProgress, totalProgress)
            }
        }

        if (active()) {
            isPlaying = false
            onCompleted?.invoke()
        }
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

    private fun calculateTotalElapsed(phases: List<Phase>, currentIdx: Int, currentPhaseElapsed: Double): Double {
        var total = 0.0
        for (i in 0 until currentIdx) {
            total += phases[i].durationMinutes * 60.0
        }
        return total + currentPhaseElapsed
    }

    @StringRes
    fun getBandLabelRes(freq: Float): Int = when {
        freq < 1f -> R.string.band_delta_sub
        freq < 4f -> R.string.band_delta
        freq < 8f -> R.string.band_theta
        freq < 13f -> R.string.band_alpha
        freq < 30f -> R.string.band_beta
        else -> R.string.band_gamma
    }

    fun getBandColor(freq: Float): Long = when {
        freq < 4f -> 0xFF9C27B0   // Purple - Delta
        freq < 8f -> 0xFF2196F3   // Blue - Theta
        freq < 13f -> 0xFFA8E6CF  // Green - Alpha
        freq < 30f -> 0xFFFFEB3B  // Yellow - Beta
        else -> 0xFFFF5722        // Orange - Gamma
    }
}
