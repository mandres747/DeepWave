package de.binauralbeats.app.audio

import de.binauralbeats.app.data.RhythmPulse
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The sound of one pulse and the timing of the next, shared by the live engine
 * and the offline exporter.
 *
 * Both were going to need the same two things, and the app already carries one
 * copy too many of its audio maths: applyModulation lives in both
 * BinauralGenerator and WavExporter. Splitting these out means an exported WAV
 * is the same sound as the live playback by construction, not because two
 * implementations happen to agree today.
 */

/** A single click, from its attack until it has died away. */
class ClickVoice(private val sampleRate: Int) {

    private var age = -1
    private var accented = false

    val isRinging: Boolean get() = age >= 0

    fun trigger(accented: Boolean) {
        this.accented = accented
        age = 0
    }

    /**
     * The sample for the current position, then advances by one. Returns 0
     * when no click is ringing, so callers can add it unconditionally.
     */
    fun nextSample(): Float {
        if (age < 0) return 0f

        val seconds = age.toDouble() / sampleRate
        // Accents sit a fifth above and ring slightly longer, which reads as
        // emphasis without being loud enough to startle.
        val decay = if (accented) 42.0 else 55.0
        val envelope = exp(-seconds * decay).toFloat()
        if (envelope < 0.0005f) {
            age = -1
            return 0f
        }

        val frequency = if (accented) 1320.0 else 880.0
        val gain = if (accented) 1f else 0.72f
        val body = sin(2 * PI * frequency * seconds).toFloat()
        // A very short noise transient gives the click an edge to sit on;
        // without it a pure sine reads as a beep.
        val transient = if (age < sampleRate / 400) {
            (Random.nextFloat() * 2f - 1f) * 0.35f
        } else {
            0f
        }

        age++
        return (body + transient) * envelope * gain
    }
}

/**
 * Decides which sample a pulse falls on, cycling a pattern forever.
 *
 * Counts in double precision because a tempo like 90 BPM lands on 666.67 ms;
 * rounding to whole milliseconds drifts about a second per quarter hour.
 */
class PulseScheduler(private val sampleRate: Int) {

    private var samplesToNext = 0.0
    private var index = 0

    /**
     * Call once per sample. Returns the pulse firing at this sample, or null.
     * An empty pattern never fires.
     */
    fun advance(pattern: List<RhythmPulse>): RhythmPulse? {
        if (pattern.isEmpty()) return null

        var fired: RhythmPulse? = null
        if (samplesToNext <= 0.0) {
            fired = pattern[index % pattern.size]
            index = (index + 1) % pattern.size
            samplesToNext += fired.intervalMillis / 1000.0 * sampleRate
        }
        samplesToNext -= 1.0
        return fired
    }

    /** Starts over, e.g. when playback restarts. */
    fun reset() {
        samplesToNext = 0.0
        index = 0
    }
}
