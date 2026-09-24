package de.binauralbeats.app.alarm

import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.data.ToneType
import kotlin.math.floor
import kotlin.math.pow

/**
 * The sessions a wake alarm plays before it rings. They are ordinary phase
 * lists run by the existing BinauralGenerator; nothing about Phase changes.
 *
 * All ramps are isochronic: a binaural beat only exists when each ear gets its
 * own tone, and in the morning the headphones are usually out and the sound
 * comes from the speaker. See docs/KLANGWECKER_KONZEPT.md, section 4.
 */
object WakeRamps {

    const val DEFAULT_KEY = "fresh"

    /**
     * Carrier of the ramp tone: G4. A phone speaker gives off little below
     * 300-500 Hz, so the app's usual 200 Hz carrier came out of it as a buzz
     * of distortion products. G4 sits a pure fourth under the bowl's C5
     * (ChimeVoice.FUNDAMENTAL_HZ); 200 Hz against the old 262 Hz bowl was
     * 467 cents, between a major third and a fourth, and sounded off.
     * Decided 2026-09-24.
     */
    const val CARRIER_HZ = 392f

    /**
     * Pulses drop to half rather than to silence. The upper bands (14-18 Hz)
     * sit where fully modulated tones start to sound rough (roughness sets in
     * around 15 Hz, Zwicker); half depth keeps the pulse audible without the
     * rasp. Decided 2026-09-24.
     */
    const val PULSE_DEPTH = 0.5f

    /** Share of each phase spent gliding into the next band's frequency. */
    private const val GLIDE_SHARE = 0.3

    /** Ramp lengths offered in the UI. The definitions below are written for 20. */
    val DURATIONS = listOf(10, 20, 30)

    private fun iso(frequency: Float, minutes: Int) =
        Phase(frequency, minutes, toneType = ToneType.ISOCHRONIC)

    val all: Map<String, List<Phase>> = linkedMapOf(
        "gentle" to listOf(iso(4f, 6), iso(7f, 7), iso(10f, 7)),
        "fresh" to listOf(iso(6f, 5), iso(10f, 7), iso(14f, 8)),
        "energetic" to listOf(iso(8f, 4), iso(12f, 6), iso(18f, 10))
    )

    fun ramp(key: String, minutes: Int): List<Phase> =
        stretch(all[key] ?: all.getValue(DEFAULT_KEY), minutes)

    /**
     * Scales [phases] so their durations add up to exactly [targetMinutes],
     * keeping the proportions. Phase durations are whole minutes, so plain
     * rounding can miss the total by one or two; the leftover minutes go to
     * the phases that lost most to rounding (largest-remainder method). No
     * phase ends up at zero, because a zero-length phase would be skipped and
     * the ramp would jump a frequency band.
     */
    fun stretch(phases: List<Phase>, targetMinutes: Int): List<Phase> {
        if (phases.isEmpty()) return phases
        val target = targetMinutes.coerceAtLeast(phases.size)
        val source = phases.sumOf { it.durationMinutes }.coerceAtLeast(1)

        val exact = phases.map { it.durationMinutes.toDouble() * target / source }
        val minutes = exact.map { floor(it).toInt().coerceAtLeast(1) }.toIntArray()

        var missing = target - minutes.sum()
        val byRemainder = exact.indices.sortedByDescending { exact[it] - floor(exact[it]) }
        var i = 0
        while (missing > 0) {
            minutes[byRemainder[i % byRemainder.size]]++
            missing--
            i++
        }
        // The coerceAtLeast(1) above can overshoot on very short targets;
        // take it back from the longest phases.
        while (missing < 0) {
            val longest = minutes.indices.maxBy { minutes[it] }
            minutes[longest]--
            missing++
        }
        return phases.mapIndexed { idx, p -> p.copy(durationMinutes = minutes[idx]) }
    }

    /**
     * Beat frequency [seconds] into [ramp]: each band holds, then glides
     * linearly into the next during the last [GLIDE_SHARE] of its phase, so
     * the pulse speeds up smoothly instead of jumping at every phase seam.
     * After the ramp the last band holds. Needs ToneVoice's phase
     * accumulation - with sin(2π·f·t) a moving f was not heard as f.
     */
    fun frequencyAt(ramp: List<Phase>, seconds: Double): Double {
        if (ramp.isEmpty()) return 0.0
        var start = 0.0
        for ((i, p) in ramp.withIndex()) {
            val length = p.durationMinutes * 60.0
            val end = start + length
            if (seconds < end) {
                val next = ramp.getOrNull(i + 1) ?: return p.frequency.toDouble()
                val glideFrom = end - length * GLIDE_SHARE
                if (seconds < glideFrom) return p.frequency.toDouble()
                val x = (seconds - glideFrom) / (end - glideFrom)
                return p.frequency + (next.frequency - p.frequency) * x
            }
            start = end
        }
        return ramp.last().frequency.toDouble()
    }

    /**
     * Levels of the three layers, as engine volumes. Kept below [HEADROOM] in
     * sum: three full layers added up to 1.69 of full scale and the system
     * limiter distorted them. At the wake time the pulse steps back to 30 %
     * so the bowl stands clear (decided 2026-09-24).
     */
    data class Levels(val pulse: Float, val ambient: Float, val chime: Float)

    const val HEADROOM = 0.9f
    private const val PULSE_AT_WAKE = 0.3f
    private const val CHIME_SHARE = 0.8f

    fun levels(volume: Float, ambientVolume: Float, hasAmbient: Boolean, ringing: Boolean): Levels {
        val amb = if (hasAmbient) ambientVolume.coerceIn(0f, 1f) else 0f
        val v = volume.coerceIn(0f, 1f)
        val raw = if (ringing) Levels(v * PULSE_AT_WAKE, amb * 0.6f, v * CHIME_SHARE)
        else Levels(v, amb, 0f)
        val sum = raw.pulse + raw.ambient + raw.chime
        if (sum <= HEADROOM) return raw
        val k = HEADROOM / sum
        return Levels(raw.pulse * k, raw.ambient * k, raw.chime * k)
    }

    /**
     * Loudness over the ramp, 0 at the start and [maxVolume] at the end.
     * Hearing is roughly logarithmic, so a linear rise sounds loud almost at
     * once and then barely changes; a power curve with [exponent] 2-3 sounds
     * like an even rise.
     */
    fun volumeAt(elapsedMs: Long, totalMs: Long, maxVolume: Float, exponent: Double = 2.5): Float {
        if (totalMs <= 0L) return maxVolume
        val t = (elapsedMs.toDouble() / totalMs).coerceIn(0.0, 1.0)
        return (maxVolume * t.pow(exponent)).toFloat()
    }
}
