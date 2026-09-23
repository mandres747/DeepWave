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
