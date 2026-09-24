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
 * - each partial carries a quieter second sine a fraction of a hertz
 *   away, which gives the slow shimmer ("beating") a real bowl has because
 *   it is never perfectly round.
 */
class ChimeVoice(private val sampleRate: Int, private val fundamental: Double = FUNDAMENTAL_HZ) {

    private class Partial(val ratio: Double, val gain: Double, val decayPerSecond: Double, val beatHz: Double)

    // Upper partials are quieter than on a real bowl: at C5 they land at
    // 1.4, 2.7 and 4.3 kHz, where a phone speaker is most efficient, and at
    // the old gains the strike sounded thin and metallic.
    private val partials = listOf(
        Partial(1.00, 1.00, 0.22, 0.35),
        Partial(2.71, 0.45, 0.45, 0.80),
        Partial(5.12, 0.20, 0.95, 1.30),
        Partial(8.18, 0.08, 1.90, 2.10)
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
            // The beating partner is at 30 %: two equal sines cancel to silence
            // on every beat, an on/off tremolo; at 30 % the level swings by
            // about 5 dB, the slow shimmer of a real bowl.
            sum += env * (sin(2 * PI * f * t) + BEAT_PARTNER * sin(2 * PI * (f + p.beatHz) * t)) / (1 + BEAT_PARTNER)
        }
        if (loudest < SILENCE) {
            age = -1
            return 0f
        }
        age++
        return (sum / norm * attack * strength).toFloat()
    }

    companion object {
        /**
         * C5. The first choice, 262 Hz, is below what a phone speaker gives
         * off; only the thin upper partials came through. C5 carries and sits
         * a pure fourth above the ramp's G4 carrier (WakeRamps.CARRIER_HZ).
         */
        const val FUNDAMENTAL_HZ = 523.25
        private const val BEAT_PARTNER = 0.3
        private const val ATTACK_SECONDS = 0.004
        private const val SILENCE = 0.001
    }
}
