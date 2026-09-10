package de.binauralbeats.app.audio

import de.binauralbeats.app.data.ModulationType
import kotlin.math.PI
import kotlin.math.sin

/**
 * How a phase's beat frequency moves over that phase.
 *
 * Lived in two places until now - BinauralGenerator for live playback and
 * WavExporter for offline rendering - which meant an exported file could
 * quietly drift away from what the app plays. One definition, both callers.
 */
fun applyModulation(
    baseFreq: Float,
    type: ModulationType,
    timeInPhase: Double,
    phaseDuration: Double
): Float = when (type) {
    ModulationType.STATIC -> baseFreq
    ModulationType.BREATHING -> {
        val breathCycle = sin(2 * PI * 0.1 * timeInPhase)
        baseFreq + (baseFreq * 0.15f * breathCycle).toFloat()
    }
    ModulationType.PULSE -> {
        val pulse = if (sin(2 * PI * 4 * timeInPhase) > 0) 1f else 0.3f
        baseFreq * pulse
    }
    ModulationType.DYNAMIC -> {
        val progress = (timeInPhase / phaseDuration).toFloat()
        baseFreq * (1f - 0.3f * progress)
    }
    ModulationType.SWEEP -> {
        val sweep = sin(2 * PI * 0.05 * timeInPhase)
        baseFreq + (baseFreq * 0.25f * sweep).toFloat()
    }
}
