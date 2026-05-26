package de.binauralbeats.app.data

import de.binauralbeats.app.R

object PremiumPresetProviderImpl : PremiumPresetProvider {
    override val presets: List<Preset> = listOf(
        // 30-Tage Schlaf-Reset-Programm (progressive Tiefschlaf-Optimierung)
        Preset("sleep_30d_w1", R.string.preset_sleep_30d_w1, "🌙", PresetCategory.PREMIUM_SLEEP, listOf(
            Phase(10f, 10, ModulationType.BREATHING),
            Phase(7f, 15),
            Phase(4f, 20),
            Phase(2f, 15),
            Phase(4f, 10, ModulationType.BREATHING)
        )),
        Preset("sleep_30d_w2", R.string.preset_sleep_30d_w2, "🌙", PresetCategory.PREMIUM_SLEEP, listOf(
            Phase(8f, 10, ModulationType.BREATHING),
            Phase(5f, 15),
            Phase(2f, 30),
            Phase(1f, 20),
            Phase(4f, 10, ModulationType.DYNAMIC)
        )),
        Preset("sleep_30d_w3", R.string.preset_sleep_30d_w3, "🌙", PresetCategory.PREMIUM_SLEEP, listOf(
            Phase(8f, 8, ModulationType.BREATHING),
            Phase(4f, 15),
            Phase(1.5f, 35),
            Phase(0.5f, 25),
            Phase(4f, 7, ModulationType.DYNAMIC)
        )),
        Preset("sleep_30d_w4", R.string.preset_sleep_30d_w4, "🌙", PresetCategory.PREMIUM_SLEEP, listOf(
            Phase(7f, 8, ModulationType.BREATHING),
            Phase(3f, 12),
            Phase(1f, 40),
            Phase(0.5f, 30),
            Phase(3f, 10, ModulationType.DYNAMIC)
        )),

        // ADHS Focus Bundle
        Preset("adhd_focus", R.string.preset_adhd_focus, "🧠", PresetCategory.PREMIUM_FOCUS, listOf(
            Phase(12f, 5, ModulationType.DYNAMIC),
            Phase(15f, 10),
            Phase(18f, 25, ModulationType.PULSE),
            Phase(14f, 15),
            Phase(18f, 20, ModulationType.PULSE),
            Phase(12f, 5, ModulationType.BREATHING)
        )),
        Preset("adhd_calm", R.string.preset_adhd_calm, "🧠", PresetCategory.PREMIUM_FOCUS, listOf(
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(8f, 15, ModulationType.BREATHING),
            Phase(10f, 10, ModulationType.BREATHING),
            Phase(12f, 5)
        )),
        Preset("exam_cram", R.string.preset_exam_cram, "🧠", PresetCategory.PREMIUM_FOCUS, listOf(
            Phase(12f, 5, ModulationType.DYNAMIC),
            Phase(16f, 25),
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(16f, 25),
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(14f, 20),
            Phase(10f, 5)
        )),
        Preset("writer_flow", R.string.preset_writer_flow, "🧠", PresetCategory.PREMIUM_FOCUS, listOf(
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(8f, 10, ModulationType.DYNAMIC),
            Phase(6f, 20),
            Phase(10f, 15, ModulationType.DYNAMIC),
            Phase(7f, 20),
            Phase(10f, 5, ModulationType.BREATHING)
        )),

        // Therapeutische Presets
        Preset("tinnitus", R.string.preset_tinnitus, "💆", PresetCategory.PREMIUM_THERAPY, listOf(
            Phase(10f, 5, ModulationType.BREATHING, BackgroundNoise.PINK),
            Phase(8f, 15, background = BackgroundNoise.PINK),
            Phase(6f, 15, background = BackgroundNoise.PINK),
            Phase(8f, 10, ModulationType.BREATHING, BackgroundNoise.PINK),
            Phase(10f, 5, background = BackgroundNoise.PINK)
        )),
        Preset("migraine", R.string.preset_migraine, "💆", PresetCategory.PREMIUM_THERAPY, listOf(
            Phase(10f, 5, ModulationType.BREATHING),
            Phase(5f, 15, background = BackgroundNoise.BROWN),
            Phase(2.5f, 15, background = BackgroundNoise.BROWN),
            Phase(5f, 10, ModulationType.BREATHING),
            Phase(8f, 5)
        )),
        Preset("ptsd_calm", R.string.preset_ptsd_calm, "💆", PresetCategory.PREMIUM_THERAPY, listOf(
            Phase(10f, 10, ModulationType.BREATHING),
            Phase(8f, 15, ModulationType.BREATHING, BackgroundNoise.BROWN),
            Phase(6f, 15, background = BackgroundNoise.BROWN),
            Phase(8f, 10, ModulationType.BREATHING),
            Phase(10f, 5)
        )),
        Preset("jet_lag", R.string.preset_jet_lag, "💆", PresetCategory.PREMIUM_THERAPY, listOf(
            Phase(14f, 10, ModulationType.DYNAMIC),
            Phase(10f, 10, ModulationType.BREATHING),
            Phase(6f, 15),
            Phase(3f, 20),
            Phase(6f, 10, ModulationType.DYNAMIC),
            Phase(10f, 5)
        ))
    )
}
