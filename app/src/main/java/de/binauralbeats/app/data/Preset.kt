package de.binauralbeats.app.data

import androidx.annotation.StringRes
import de.binauralbeats.app.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class ToneType { BINAURAL, ISOCHRONIC }

@Serializable
enum class ModulationType { STATIC, BREATHING, PULSE, DYNAMIC, SWEEP }

@Serializable
enum class BackgroundNoise { NONE, PINK, BROWN }

@Serializable
data class Phase(
    val frequency: Float,
    val durationMinutes: Int,
    val modulation: ModulationType = ModulationType.STATIC,
    val background: BackgroundNoise = BackgroundNoise.NONE,
    val toneType: ToneType = ToneType.BINAURAL,
    val guidance: String? = null,
    @Transient @StringRes val guidanceRes: Int = 0
)

data class Preset(
    val key: String,
    @StringRes val nameRes: Int,
    val emoji: String,
    val category: PresetCategory,
    val phases: List<Phase>,
    val carrierOverride: Float? = null
) {
    val totalDurationMinutes: Int get() = phases.sumOf { it.durationMinutes }
}

enum class PresetCategory(@StringRes val labelRes: Int, val emoji: String) {
    SLEEP(R.string.cat_sleep, "😴"),
    FOCUS(R.string.cat_focus, "🎯"),
    MEDITATION(R.string.cat_meditation, "🧘"),
    ENERGY(R.string.cat_energy, "⚡"),
    CREATIVITY(R.string.cat_creativity, "🎨"),
    SPORT(R.string.cat_sport, "🏋️"),
    RELAXATION(R.string.cat_relaxation, "🌿"),
    SOLFEGGIO(R.string.cat_solfeggio, "🔔"),
    ISOCHRONIC(R.string.cat_isochronic, "🎵")
}

@Serializable
data class CustomPreset(
    val id: String,
    val name: String,
    val emoji: String = "🎵",
    val phases: List<Phase>,
    val createdAt: Long,
    val sourcePresetKey: String? = null
) {
    val totalDurationMinutes: Int get() = phases.sumOf { it.durationMinutes }
}

object Presets {
    val all: List<Preset> = listOf(
        // SCHLAF & ERHOLUNG
        Preset("power_nap", R.string.preset_power_nap, "💤", PresetCategory.SLEEP, listOf(
            Phase(10f, 3, ModulationType.BREATHING),
            Phase(7f, 5),
            Phase(5f, 10),
            Phase(10f, 2, ModulationType.DYNAMIC)
        )),
        Preset("deep_sleep", R.string.preset_deep_sleep, "😴", PresetCategory.SLEEP, listOf(
            Phase(7f, 10),
            Phase(4f, 20),
            Phase(1f, 40),
            Phase(0.5f, 20)
        )),
        Preset("full_night_sleep", R.string.preset_full_night, "🌙", PresetCategory.SLEEP, listOf(
            Phase(8f, 15, ModulationType.BREATHING),
            Phase(4f, 25),
            Phase(1f, 40),
            Phase(0.5f, 50),
            Phase(1f, 40),
            Phase(4f, 30),
            Phase(1f, 50),
            Phase(4f, 30),
            Phase(7f, 20, ModulationType.DYNAMIC)
        )),
        Preset("lucid_dreaming", R.string.preset_lucid_dream, "🌟", PresetCategory.SLEEP, listOf(
            Phase(10f, 10, ModulationType.BREATHING, guidanceRes = R.string.guide_eyes_close),
            Phase(7f, 15, guidanceRes = R.string.guide_thoughts),
            Phase(5f, 20, guidanceRes = R.string.guide_theta),
            Phase(40f, 5, ModulationType.PULSE, guidanceRes = R.string.guide_gamma_trigger),
            Phase(6f, 10, ModulationType.DYNAMIC, guidanceRes = R.string.guide_awareness)
        )),
        Preset("lucid_wbtb", R.string.preset_wbtb, "🌙", PresetCategory.SLEEP, listOf(
            Phase(8f, 15, ModulationType.BREATHING, guidanceRes = R.string.guide_wbtb_intent),
            Phase(4f, 60, guidanceRes = R.string.guide_deep_cycle1),
            Phase(1f, 90, guidanceRes = R.string.guide_deep_cycle2),
            Phase(6f, 30, guidanceRes = R.string.guide_rem_prep),
            Phase(40f, 10, ModulationType.PULSE, guidanceRes = R.string.guide_gamma_wbtb),
            Phase(6f, 90, ModulationType.DYNAMIC, guidanceRes = R.string.guide_rem_gamma),
            Phase(10f, 20, ModulationType.BREATHING, guidanceRes = R.string.guide_wake_journal)
        )),
        Preset("obe_monroe", R.string.preset_obe, "🔮", PresetCategory.SLEEP, listOf(
            Phase(10f, 10, ModulationType.BREATHING, guidanceRes = R.string.guide_pmr),
            Phase(7f, 15, guidanceRes = R.string.guide_theta_body),
            Phase(4.5f, 40, guidanceRes = R.string.guide_mind_awake),
            Phase(4f, 20, ModulationType.DYNAMIC, guidanceRes = R.string.guide_vibrations),
            Phase(7f, 5, ModulationType.BREATHING, guidanceRes = R.string.guide_return)
        )),

        // FOKUS & PRODUKTIVITÄT
        Preset("deep_focus", R.string.preset_deep_focus, "🎯", PresetCategory.FOCUS, listOf(
            Phase(10f, 5),
            Phase(14f, 70),
            Phase(12f, 10, ModulationType.BREATHING),
            Phase(10f, 5)
        )),
        Preset("pomodoro_focus", R.string.preset_pomodoro, "🍅", PresetCategory.FOCUS, listOf(
            Phase(12f, 2, ModulationType.DYNAMIC),
            Phase(16f, 21),
            Phase(12f, 2),
            Phase(8f, 5, ModulationType.BREATHING)
        )),
        Preset("flow_state", R.string.preset_flow, "🌊", PresetCategory.FOCUS, listOf(
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(12f, 10),
            Phase(15f, 35),
            Phase(12f, 10)
        )),
        Preset("exam_preparation", R.string.preset_study, "📚", PresetCategory.FOCUS, listOf(
            Phase(10f, 5),
            Phase(12f, 15),
            Phase(14f, 20),
            Phase(10f, 5, ModulationType.BREATHING)
        )),

        // MEDITATION & ENTSPANNUNG
        Preset("quick_meditation", R.string.preset_quick_med, "🧘", PresetCategory.MEDITATION, listOf(
            Phase(10f, 3, ModulationType.BREATHING),
            Phase(7f, 9, ModulationType.BREATHING),
            Phase(10f, 3)
        )),
        Preset("deep_meditation", R.string.preset_deep_med, "🕉️", PresetCategory.MEDITATION, listOf(
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(7f, 10, ModulationType.BREATHING),
            Phase(5f, 15),
            Phase(4f, 10),
            Phase(7f, 5, ModulationType.BREATHING)
        )),
        Preset("stress_relief", R.string.preset_stress, "😌", PresetCategory.MEDITATION, listOf(
            Phase(12f, 5, ModulationType.BREATHING),
            Phase(10f, 10, ModulationType.BREATHING),
            Phase(8f, 10, ModulationType.BREATHING),
            Phase(10f, 5)
        )),
        Preset("anxiety_relief", R.string.preset_anxiety, "💚", PresetCategory.MEDITATION, listOf(
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(8f, 10, ModulationType.BREATHING),
            Phase(10f, 5, ModulationType.BREATHING)
        )),

        // ENERGIE & AKTIVIERUNG
        Preset("morning_energy", R.string.preset_morning, "☀️", PresetCategory.ENERGY, listOf(
            Phase(10f, 3, ModulationType.DYNAMIC),
            Phase(14f, 5),
            Phase(18f, 5, ModulationType.PULSE),
            Phase(14f, 2)
        )),
        Preset("energy_boost", R.string.preset_energy, "⚡", PresetCategory.ENERGY, listOf(
            Phase(12f, 2, ModulationType.DYNAMIC),
            Phase(16f, 3, ModulationType.PULSE),
            Phase(20f, 3),
            Phase(14f, 2)
        )),
        Preset("afternoon_revival", R.string.preset_afternoon, "🔄", PresetCategory.ENERGY, listOf(
            Phase(10f, 3, ModulationType.BREATHING),
            Phase(7f, 5),
            Phase(12f, 7, ModulationType.DYNAMIC),
            Phase(16f, 5)
        )),

        // KREATIVITÄT
        Preset("creative_flow", R.string.preset_creative, "🎨", PresetCategory.CREATIVITY, listOf(
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(7f, 15, ModulationType.DYNAMIC),
            Phase(5f, 15),
            Phase(10f, 5)
        )),
        Preset("brainstorming", R.string.preset_brainstorm, "💡", PresetCategory.CREATIVITY, listOf(
            Phase(10f, 5, ModulationType.DYNAMIC),
            Phase(8f, 10, ModulationType.DYNAMIC),
            Phase(6f, 10, ModulationType.PULSE),
            Phase(10f, 5)
        )),

        // SPORT
        Preset("half_marathon", R.string.preset_marathon, "🏃", PresetCategory.SPORT, listOf(
            Phase(12f, 10),
            Phase(15f, 30, ModulationType.DYNAMIC),
            Phase(18f, 25),
            Phase(20f, 15),
            Phase(15f, 10, ModulationType.BREATHING)
        )),
        Preset("workout", R.string.preset_workout, "🏋️", PresetCategory.SPORT, listOf(
            Phase(12f, 5, ModulationType.DYNAMIC),
            Phase(16f, 15),
            Phase(18f, 15, ModulationType.PULSE),
            Phase(14f, 5, ModulationType.BREATHING),
            Phase(10f, 5, ModulationType.BREATHING)
        )),
        Preset("yoga_session", R.string.preset_yoga, "🧘‍♀️", PresetCategory.SPORT, listOf(
            Phase(10f, 10, ModulationType.BREATHING),
            Phase(8f, 20, ModulationType.BREATHING),
            Phase(6f, 20),
            Phase(10f, 10, ModulationType.BREATHING)
        )),

        // ENTSPANNUNG & REGENERATION
        Preset("pain_relief", R.string.preset_deep_relax, "🌙", PresetCategory.RELAXATION, listOf(
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(5f, 10),
            Phase(2f, 10),
            Phase(5f, 5)
        )),
        Preset("headache_relief", R.string.preset_calm, "😌", PresetCategory.RELAXATION, listOf(
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(8f, 10, ModulationType.BREATHING),
            Phase(10f, 5)
        )),
        Preset("immune_boost", R.string.preset_regen, "🌿", PresetCategory.RELAXATION, listOf(
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(7f, 10),
            Phase(4f, 15),
            Phase(7f, 10)
        )),

        // SOLFEGGIO-FREQUENZEN
        Preset("solf_396", R.string.preset_solf_396, "🔔", PresetCategory.SOLFEGGIO, listOf(
            Phase(10f, 5, ModulationType.BREATHING, guidanceRes = R.string.guide_solf_396),
            Phase(7f, 20, background = BackgroundNoise.PINK),
            Phase(4f, 10),
            Phase(7f, 5, ModulationType.BREATHING)
        ), carrierOverride = 396f),
        Preset("solf_528", R.string.preset_solf_528, "💚", PresetCategory.SOLFEGGIO, listOf(
            Phase(10f, 5, ModulationType.BREATHING, guidanceRes = R.string.guide_solf_528),
            Phase(7f, 15, background = BackgroundNoise.PINK),
            Phase(5f, 15),
            Phase(7f, 5, ModulationType.BREATHING)
        ), carrierOverride = 528f),
        Preset("solf_639", R.string.preset_solf_639, "🤝", PresetCategory.SOLFEGGIO, listOf(
            Phase(10f, 5, ModulationType.BREATHING, guidanceRes = R.string.guide_solf_639),
            Phase(8f, 15, ModulationType.BREATHING),
            Phase(6f, 10),
            Phase(10f, 5)
        ), carrierOverride = 639f),
        Preset("solf_741", R.string.preset_solf_741, "🗣️", PresetCategory.SOLFEGGIO, listOf(
            Phase(10f, 5, ModulationType.BREATHING, guidanceRes = R.string.guide_solf_741),
            Phase(12f, 15),
            Phase(10f, 10, ModulationType.BREATHING),
            Phase(8f, 5)
        ), carrierOverride = 741f),
        Preset("solf_852", R.string.preset_solf_852, "👁️", PresetCategory.SOLFEGGIO, listOf(
            Phase(10f, 5, ModulationType.BREATHING, guidanceRes = R.string.guide_solf_852),
            Phase(7f, 10),
            Phase(4f, 20),
            Phase(7f, 5, ModulationType.BREATHING)
        ), carrierOverride = 852f),
        Preset("solf_963", R.string.preset_solf_963, "✨", PresetCategory.SOLFEGGIO, listOf(
            Phase(10f, 5, ModulationType.BREATHING, guidanceRes = R.string.guide_solf_963),
            Phase(7f, 10),
            Phase(4f, 15, background = BackgroundNoise.PINK),
            Phase(2f, 10),
            Phase(7f, 5, ModulationType.BREATHING)
        ), carrierOverride = 963f),

        // ISOCHRONISCHE TÖNE
        Preset("iso_focus", R.string.preset_iso_focus, "🎵", PresetCategory.ISOCHRONIC, listOf(
            Phase(10f, 5, toneType = ToneType.ISOCHRONIC),
            Phase(14f, 40, toneType = ToneType.ISOCHRONIC),
            Phase(10f, 5, toneType = ToneType.ISOCHRONIC, modulation = ModulationType.BREATHING)
        )),
        Preset("iso_relax", R.string.preset_iso_relax, "🎵", PresetCategory.ISOCHRONIC, listOf(
            Phase(10f, 5, toneType = ToneType.ISOCHRONIC, modulation = ModulationType.BREATHING),
            Phase(7f, 15, toneType = ToneType.ISOCHRONIC),
            Phase(5f, 10, toneType = ToneType.ISOCHRONIC, background = BackgroundNoise.PINK),
            Phase(7f, 5, toneType = ToneType.ISOCHRONIC)
        )),
        Preset("iso_meditation", R.string.preset_iso_med, "🎵", PresetCategory.ISOCHRONIC, listOf(
            Phase(10f, 5, toneType = ToneType.ISOCHRONIC, modulation = ModulationType.BREATHING),
            Phase(7f, 10, toneType = ToneType.ISOCHRONIC),
            Phase(4f, 15, toneType = ToneType.ISOCHRONIC),
            Phase(7f, 5, toneType = ToneType.ISOCHRONIC)
        )),
        Preset("iso_energy", R.string.preset_iso_energy, "🎵", PresetCategory.ISOCHRONIC, listOf(
            Phase(12f, 3, toneType = ToneType.ISOCHRONIC, modulation = ModulationType.DYNAMIC),
            Phase(18f, 7, toneType = ToneType.ISOCHRONIC, modulation = ModulationType.PULSE),
            Phase(14f, 5, toneType = ToneType.ISOCHRONIC)
        ))
    )

    val byCategory: Map<PresetCategory, List<Preset>> = all.groupBy { it.category }
}
