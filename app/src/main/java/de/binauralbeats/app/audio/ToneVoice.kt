package de.binauralbeats.app.audio

import de.binauralbeats.app.data.ToneType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * The tone of a session, one stereo sample at a time. Shared by
 * BinauralGenerator, WavExporter and WakePreview, so what is exported or
 * previewed is the sound that plays - the same reason RhythmVoice exists.
 *
 * Phase accumulators, not sin(2π·f·t). The old form is only right for a
 * constant f: the frequency actually heard is the derivative of the phase,
 * d/dt[f(t)·t] = f + t·f', and with t the time since the start of the
 * session the error grows every second. A 10 Hz SWEEP was heard as a beat
 * swinging between -45 and +73 Hz after one minute and ±950 Hz after twenty
 * (checked 2026-09-24). Advancing each phase by f/sampleRate per sample makes
 * the heard frequency exactly f, however f moves - which is also what lets a
 * wake ramp glide between bands.
 */
class ToneVoice(private val sampleRate: Int) {

    private var leftPhase = 0.0
    private var rightPhase = 0.0
    private var pulsePhase = 0.0

    /**
     * Writes the next left/right sample (each -1..1) into [out] and advances.
     * [pulseDepth] only matters for isochronic tones: 1 switches the tone fully
     * off between pulses, 0.5 lets it fall to half.
     */
    fun next(
        carrier: Double,
        beat: Double,
        type: ToneType,
        pulseDepth: Double,
        out: FloatArray
    ) {
        leftPhase = wrap(leftPhase + carrier / sampleRate)
        when (type) {
            ToneType.BINAURAL -> {
                rightPhase = wrap(rightPhase + (carrier + beat) / sampleRate)
                out[0] = sin(2 * PI * leftPhase).toFloat()
                out[1] = sin(2 * PI * rightPhase).toFloat()
            }
            ToneType.ISOCHRONIC -> {
                pulsePhase = wrap(pulsePhase + beat / sampleRate)
                val gate = isochronicGate(pulsePhase, edgeFraction(beat, sampleRate), pulseDepth)
                val s = (sin(2 * PI * leftPhase) * gate).toFloat()
                out[0] = s
                out[1] = s
            }
        }
    }

    companion object {
        /** Length of each pulse flank. Long enough to kill the click, short enough to keep pulses distinct. */
        const val EDGE_SECONDS = 0.010

        private fun wrap(p: Double) = p - floor(p)

        /** The flank as a fraction of one pulse cycle, capped so on and off still exist at high rates. */
        fun edgeFraction(beat: Double, sampleRate: Int): Double =
            (EDGE_SECONDS * beat).coerceIn(1.0 / sampleRate, 0.2)

        /**
         * Gain for a pulse cycle position [phase] (0..1): on for the first
         * half, off for the second, with raised-cosine flanks of [edge]
         * cycles. A hard on/off step is a broadband click at every flank -
         * measured -25 dB above 1 kHz against the tone for a 14 Hz pulse on
         * a 200 Hz carrier. [depth] scales how far "off" goes.
         */
        fun isochronicGate(phase: Double, edge: Double, depth: Double): Double {
            val on = when {
                phase < edge -> 0.5 - 0.5 * cos(PI * phase / edge)
                phase < 0.5 - edge -> 1.0
                phase < 0.5 -> 0.5 - 0.5 * cos(PI * (0.5 - phase) / edge)
                else -> 0.0
            }
            return 1.0 - depth.coerceIn(0.0, 1.0) * (1.0 - on)
        }
    }
}
