package de.binauralbeats.app.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * One strike of a singing bowl, synthesized like the rhythm click - the app
 * ships no audio assets. Used by the wake alarm from the wake time on.
 *
 * What makes it a bowl rather than a bell or a beep:
 * - inharmonic partials (about 1 : 2.71 : 5.12 : 8.18, measured ratios of
 *   Tibetan-style bowls) instead of the 1 : 2 : 3 of a string or a pipe;
 * - the higher a partial, the sooner it dies, so the strike starts bright
 *   and settles into a low hum;
 * - each partial is two sines a fraction of a hertz apart, which gives the
 *   slow wobble ("beating") a real bowl has because it is never perfectly
 *   round.
 */
class ChimeVoice(private val sampleRate: Int, private val fundamental: Double = FUNDAMENTAL_HZ) {

    private class Partial(val ratio: Double, val gain: Double, val decayPerSecond: Double, val beatHz: Double)

    private val partials = listOf(
        Partial(1.00, 1.00, 0.22, 0.35),
        Partial(2.71, 0.55, 0.45, 0.80),
        Partial(5.12, 0.28, 0.95, 1.30),
        Partial(8.18, 0.12, 1.90, 2.10)
    )

    // Sum of the partial gains; dividing by it keeps a fresh strike within ±1.
    private val norm = partials.sumOf { it.gain }

    private var age = -1
    private var strength = 1f

    val isRinging: Boolean get() = age >= 0

    /** Strikes the bowl; [strength] 0..1 scales the whole strike. */
    fun strike(strength: Float = 1f) {
        this.strength = strength.coerceIn(0f, 1f)
        age = 0
    }

    /** The next sample in -1..1, or 0 once the strike has died away. */
    fun nextSample(): Float {
        if (age < 0) return 0f
        val t = age.toDouble() / sampleRate
        // A few milliseconds of attack: a sample that jumps straight from
        // silence to full level clicks audibly.
        val attack = (t / ATTACK_SECONDS).coerceAtMost(1.0)

        var sum = 0.0
        var loudest = 0.0
        for (p in partials) {
            val env = p.gain * exp(-t * p.decayPerSecond)
            loudest = maxOf(loudest, env)
            val f = fundamental * p.ratio
            sum += env * 0.5 * (sin(2 * PI * f * t) + sin(2 * PI * (f + p.beatHz) * t))
        }
        if (loudest < SILENCE) {
            age = -1
            return 0f
        }
        age++
        return (sum / norm * attack * strength).toFloat()
    }

    companion object {
        /** Low enough to sound warm, high enough for a phone speaker to carry. */
        const val FUNDAMENTAL_HZ = 262.0
        private const val ATTACK_SECONDS = 0.004
        private const val SILENCE = 0.001
    }
}
