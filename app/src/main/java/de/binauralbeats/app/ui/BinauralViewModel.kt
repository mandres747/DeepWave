package de.binauralbeats.app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.binauralbeats.app.audio.WavExporter
import de.binauralbeats.app.data.BackgroundNoise
import de.binauralbeats.app.data.CustomPreset
import de.binauralbeats.app.data.JournalEntry
import de.binauralbeats.app.data.JournalRepository
import de.binauralbeats.app.data.ModulationType
import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.data.Preset
import de.binauralbeats.app.data.PresetRepository
import de.binauralbeats.app.data.PremiumPresetProviderImpl
import de.binauralbeats.app.data.Presets
import de.binauralbeats.app.data.SettingsRepository
import de.binauralbeats.app.data.ToneType
import de.binauralbeats.app.FeatureFlagsImpl
import de.binauralbeats.app.R
import de.binauralbeats.app.service.AudioPlaybackService
import de.binauralbeats.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class BinauralViewModel(application: Application) : AndroidViewModel(application) {

    val features = FeatureFlagsImpl

    private val presetRepo = PresetRepository(application)
    private val journalRepo = JournalRepository(application)
    private val settingsRepo = SettingsRepository(application)

    // --- Built-in preset selection ---

    var selectedPreset by mutableStateOf<Preset?>(null)
        private set

    var selectedCustomPresetId by mutableStateOf<String?>(null)
        private set

    // --- Audio controls ---

    var carrierFrequency by mutableFloatStateOf(200f)
    var masterVolume by mutableFloatStateOf(0.7f)
    var noiseVolume by mutableFloatStateOf(0.15f)
    var transitionTimeMs by mutableIntStateOf(500)

    // --- Playback state ---

    var isPlaying by mutableStateOf(false)
        private set

    var isPaused by mutableStateOf(false)
        private set

    var currentPhaseIndex by mutableIntStateOf(0)
        private set

    var phaseProgress by mutableFloatStateOf(0f)
        private set

    var totalProgress by mutableFloatStateOf(0f)
        private set

    var currentGuidance by mutableStateOf<String?>(null)
        private set

    // --- Editable phases (working copy) ---

    var editablePhases by mutableStateOf(listOf(Phase(10f, 20)))
        private set

    var isEditing by mutableStateOf(false)

    val isModified: Boolean
        get() {
            val source = selectedPreset?.phases
            return source != null && source != editablePhases
        }

    private val app: Application get() = getApplication()

    val activePresetName: String
        get() = selectedPreset?.let { app.getString(it.nameRes) }
            ?: app.getString(R.string.custom_session)

    // --- Custom presets (persisted) ---

    val customPresets = presetRepo.customPresets.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // --- Journal (persisted) ---

    val journalEntries = journalRepo.entries.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    var showSaveDialog by mutableStateOf(false)
    var showJournal by mutableStateOf(false)
    var showRatingDialog by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showStatistics by mutableStateOf(false)

    val allByCategory = buildMap {
        putAll(Presets.byCategory)
        if (features.premiumPresetsEnabled) {
            PremiumPresetProviderImpl.presets.groupBy { it.category }.forEach { (cat, presets) ->
                put(cat, presets)
            }
        }
    }

    // --- Settings ---

    val themeMode = settingsRepo.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM
    )

    val languageTag = settingsRepo.languageTag.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    // --- WAV Export ---

    var isExporting by mutableStateOf(false)
        private set
    var exportProgress by mutableFloatStateOf(0f)
        private set
    var exportResult by mutableStateOf<String?>(null)
    var isExportError by mutableStateOf(false)
        private set

    private var sessionStartTime: Long = 0L

    // --- Service binding ---

    private var service: AudioPlaybackService? = null

    fun bindService(svc: AudioPlaybackService) {
        service = svc
        svc.generator.onPhaseChanged = { idx ->
            currentPhaseIndex = idx
            val phase = editablePhases.getOrNull(idx)
            currentGuidance = if (phase != null && phase.guidanceRes != 0) {
                app.getString(phase.guidanceRes)
            } else {
                phase?.guidance
            }
        }
        svc.generator.onProgressUpdate = { _, phaseProg, totalProg ->
            phaseProgress = phaseProg
            totalProgress = totalProg
        }
        svc.generator.onCompleted = {
            isPlaying = false
            isPaused = false
            totalProgress = 1f
            showRatingDialog = true
        }
    }

    // --- Preset selection ---

    fun selectPreset(preset: Preset) {
        selectedPreset = preset
        selectedCustomPresetId = null
        editablePhases = preset.phases.toList()
        preset.carrierOverride?.let { carrierFrequency = it }
        isEditing = false
    }

    fun selectCustomPreset(custom: CustomPreset) {
        selectedPreset = null
        selectedCustomPresetId = custom.id
        editablePhases = custom.phases.toList()
        isEditing = false
    }

    fun clearPreset() {
        selectedPreset = null
        selectedCustomPresetId = null
        editablePhases = listOf(Phase(10f, 20))
        isEditing = false
    }

    fun resetToOriginal() {
        selectedPreset?.let { editablePhases = it.phases.toList() }
    }

    // --- Phase editing ---

    fun updatePhase(index: Int, phase: Phase) {
        editablePhases = editablePhases.toMutableList().also { it[index] = phase }
    }

    fun addPhase() {
        val last = editablePhases.lastOrNull() ?: Phase(10f, 10)
        editablePhases = editablePhases + last.copy()
    }

    fun removePhase(index: Int) {
        if (editablePhases.size > 1) {
            editablePhases = editablePhases.toMutableList().also { it.removeAt(index) }
        }
    }

    fun duplicatePhase(index: Int) {
        val phase = editablePhases[index]
        editablePhases = editablePhases.toMutableList().also { it.add(index + 1, phase.copy()) }
    }

    fun movePhase(from: Int, to: Int) {
        if (from == to) return
        editablePhases = editablePhases.toMutableList().also {
            val item = it.removeAt(from)
            it.add(to, item)
        }
    }

    // --- Custom preset persistence ---

    var customPresetLimitReached by mutableStateOf(false)

    fun saveAsCustomPreset(name: String, emoji: String = "🎵") {
        viewModelScope.launch {
            val isUpdate = selectedCustomPresetId != null
            if (!isUpdate) {
                val current = presetRepo.currentCount()
                if (current >= features.maxCustomPresets) {
                    customPresetLimitReached = true
                    showSaveDialog = false
                    return@launch
                }
            }
            val preset = CustomPreset(
                id = selectedCustomPresetId ?: UUID.randomUUID().toString(),
                name = name,
                emoji = emoji,
                phases = editablePhases,
                createdAt = System.currentTimeMillis(),
                sourcePresetKey = selectedPreset?.key
            )
            presetRepo.save(preset)
            selectedCustomPresetId = preset.id
            customPresetLimitReached = false
            showSaveDialog = false
        }
    }

    fun deleteCustomPreset(id: String) {
        viewModelScope.launch {
            presetRepo.delete(id)
            if (selectedCustomPresetId == id) clearPreset()
        }
    }

    // --- Playback ---

    fun play() {
        val svc = service ?: return
        isPlaying = true
        isPaused = false
        currentPhaseIndex = 0
        phaseProgress = 0f
        totalProgress = 0f
        sessionStartTime = System.currentTimeMillis()
        svc.startPlayback(editablePhases, carrierFrequency, masterVolume, noiseVolume, transitionTimeMs)
    }

    fun pause() {
        service?.generator?.pause()
        isPaused = true
    }

    fun resume() {
        service?.generator?.resume()
        isPaused = false
    }

    fun stop() {
        service?.stopPlayback()
        isPlaying = false
        isPaused = false
        phaseProgress = 0f
        totalProgress = 0f
        currentGuidance = null
    }

    fun togglePlayback() {
        when {
            !isPlaying -> play()
            isPaused -> resume()
            else -> pause()
        }
    }

    // --- Journal ---

    fun saveJournalEntry(rating: Int, moods: List<String>, notes: String) {
        viewModelScope.launch {
            val entry = JournalEntry(
                id = UUID.randomUUID().toString(),
                presetName = activePresetName,
                presetKey = selectedPreset?.key ?: selectedCustomPresetId,
                totalDurationMinutes = editablePhases.sumOf { it.durationMinutes },
                completedAt = System.currentTimeMillis(),
                rating = rating,
                moods = moods,
                notes = notes
            )
            journalRepo.save(entry)
            showRatingDialog = false
        }
    }

    fun dismissRating() {
        showRatingDialog = false
    }

    fun deleteJournalEntry(id: String) {
        viewModelScope.launch { journalRepo.delete(id) }
    }

    // --- WAV Export ---

    fun exportWav() {
        if (isExporting || !features.wavExportEnabled) return
        isExporting = true
        exportProgress = 0f
        exportResult = null

        viewModelScope.launch {
            val name = activePresetName.replace(Regex("[^a-zA-Z0-9äöüÄÖÜß _-]"), "")
            val timestamp = System.currentTimeMillis()
            val filename = "${name}_$timestamp"

            val result = WavExporter().export(
                context = getApplication(),
                phases = editablePhases,
                carrier = carrierFrequency,
                volume = masterVolume,
                noiseVolume = noiseVolume,
                transitionMs = transitionTimeMs,
                filename = filename,
                onProgress = { exportProgress = it }
            )

            isExporting = false
            isExportError = !result.success
            exportResult = if (result.success) app.getString(R.string.export_success, result.filename)
            else app.getString(R.string.export_error, result.error ?: "")
        }
    }

    // --- Session Sharing ---

    fun generateShareUri(): Uri {
        val phasesParam = editablePhases.joinToString(";") { p ->
            "${p.frequency},${p.durationMinutes},${p.modulation.name},${p.background.name},${p.toneType.name}"
        }
        return Uri.parse("binauralbeats://session?carrier=${carrierFrequency.toInt()}&phases=$phasesParam")
    }

    fun importFromUri(uri: Uri): Boolean {
        if (uri.scheme != "binauralbeats" || uri.host != "session") return false
        try {
            uri.getQueryParameter("carrier")?.toFloatOrNull()?.let { carrierFrequency = it }
            val phasesStr = uri.getQueryParameter("phases") ?: return false
            val imported = phasesStr.split(";").map { part ->
                val fields = part.split(",")
                Phase(
                    frequency = fields[0].toFloat(),
                    durationMinutes = fields[1].toInt(),
                    modulation = ModulationType.valueOf(fields.getOrElse(2) { "STATIC" }),
                    background = BackgroundNoise.valueOf(fields.getOrElse(3) { "NONE" }),
                    toneType = ToneType.valueOf(fields.getOrElse(4) { "BINAURAL" })
                )
            }
            if (imported.isEmpty()) return false
            clearPreset()
            editablePhases = imported
            return true
        } catch (_: Exception) {
            return false
        }
    }

    // --- Settings ---

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setLanguage(tag: String) {
        viewModelScope.launch { settingsRepo.setLanguage(tag) }
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
