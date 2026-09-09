package de.binauralbeats.app.data

import kotlinx.serialization.Serializable

/**
 * One audible pulse and the gap that follows it.
 *
 * The interval is a Double on purpose. A metronome at 90 BPM sits at 666.67 ms
 * per beat; rounding that to whole milliseconds drifts by about a second per
 * quarter hour, which is exactly the kind of slow slide a listener notices
 * without being able to name it. The engine accumulates these in double
 * precision and only rounds when it places a sample.
 */
@Serializable
data class RhythmPulse(
    val intervalMillis: Double,
    val accented: Boolean
)

/**
 * Patterns the engine can play. Both shapes below are the same list of pulses
 * cycled forever - a metronome is just a pattern whose intervals happen to be
 * equal, and a breathing cue is one whose intervals are not. Keeping a single
 * representation means the engine has no idea which of the two it is playing.
 */
object RhythmPattern {

    const val MIN_BPM = 40
    const val MAX_BPM = 200

    /**
     * @param accentEvery emphasise every n-th pulse; 0 for no accent at all.
     */
    fun metronome(bpm: Int, accentEvery: Int = 0): List<RhythmPulse> {
        val safeBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
        val interval = 60_000.0 / safeBpm
        if (accentEvery <= 1) {
            return listOf(RhythmPulse(interval, accented = accentEvery == 1))
        }
        return List(accentEvery) { index ->
            RhythmPulse(interval, accented = index == 0)
        }
    }

    /**
     * One cue per breath phase, so the audible pattern matches what the
     * breathing guide shows. Zero-length phases are dropped - a pattern
     * without a second hold must not produce a pulse at the same instant as
     * the next inhale.
     */
    fun breathing(inhaleSec: Int, hold1Sec: Int, exhaleSec: Int, hold2Sec: Int): List<RhythmPulse> =
        listOf(inhaleSec, hold1Sec, exhaleSec, hold2Sec)
            .mapIndexed { index, seconds -> index to seconds }
            .filter { (_, seconds) -> seconds > 0 }
            .map { (index, seconds) ->
                RhythmPulse(seconds * 1000.0, accented = index == 0)
            }
}

/** One segment of a program: hold this tempo for this long. */
@Serializable
data class RhythmStep(
    val bpm: Int,
    val durationMinutes: Int,
    val accentEvery: Int = 4
) {
    val pulses: List<RhythmPulse> get() = RhythmPattern.metronome(bpm, accentEvery)
}

@Serializable
data class RhythmProgram(
    val id: String,
    val name: String,
    val steps: List<RhythmStep>
) {
    val totalMinutes: Int get() = steps.sumOf { it.durationMinutes }
}

/**
 * Which step of a program is playing after a given number of seconds, or null
 * once the program has run out. Pulled out as a pure function so the awkward
 * cases - empty program, a step with zero length, the exact boundary between
 * two steps - are settled by tests rather than inside the audio thread.
 */
fun stepAt(steps: List<RhythmStep>, elapsedSeconds: Int): RhythmStep? {
    if (elapsedSeconds < 0) return steps.firstOrNull { it.durationMinutes > 0 }
    var boundary = 0
    for (step in steps) {
        if (step.durationMinutes <= 0) continue
        boundary += step.durationMinutes * 60
        if (elapsedSeconds < boundary) return step
    }
    return null
}

/** Whether the pulse follows a fixed tempo or the breathing guide. */
enum class RhythmMode { TEMPO, BREATH }

/**
 * What the rhythm sheet remembers between launches. The breath pattern is
 * carried as its enum name so the data layer stays free of the UI enum it
 * belongs to.
 */
data class RhythmSettings(
    val mode: RhythmMode = RhythmMode.TEMPO,
    val bpm: Int = 120,
    val accentEvery: Int = 4,
    val volume: Float = 0.6f,
    val breathPatternName: String = "RELAXING"
)
