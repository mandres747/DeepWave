package de.binauralbeats.app.data

interface PremiumPresetProvider {
    val presets: List<Preset>
    val categories: List<PresetCategory>
        get() = presets.map { it.category }.distinct()
}
