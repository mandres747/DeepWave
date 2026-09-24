package de.binauralbeats.app.ui

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.binauralbeats.app.FeatureFlagsImpl
import de.binauralbeats.app.R
import de.binauralbeats.app.audio.WavExporter
import de.binauralbeats.app.billing.Access
import de.binauralbeats.app.billing.AccessState
import de.binauralbeats.app.billing.Entitlements
import de.binauralbeats.app.billing.EntitlementsImpl
import de.binauralbeats.app.billing.PurchaseResult
import de.binauralbeats.app.data.AmbientSound
import de.binauralbeats.app.data.BackgroundNoise
import de.binauralbeats.app.data.CustomPreset
import de.binauralbeats.app.data.JournalEntry
import de.binauralbeats.app.data.JournalRepository
import de.binauralbeats.app.data.ModulationType
import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.data.PremiumPresetProviderImpl
import de.binauralbeats.app.data.Preset
import de.binauralbeats.app.data.PresetRepository
import de.binauralbeats.app.data.Presets
import de.binauralbeats.app.data.RhythmMode
import de.binauralbeats.app.data.RhythmPattern
import de.binauralbeats.app.data.RhythmPulse
import de.binauralbeats.app.data.RhythmSettings
import de.binauralbeats.app.data.RhythmStep
import de.binauralbeats.app.data.SettingsRepository
import de.binauralbeats.app.data.ToneType
import de.binauralbeats.app.data.defaultRhythmProgram
import de.binauralbeats.app.data.stepAt
import de.binauralbeats.app.service.AudioPlaybackService
import de.binauralbeats.app.ui.components.BreathingPattern
import de.binauralbeats.app.ui.theme.ThemeMode
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BinauralViewModel(application: Application) : AndroidViewModel(application) {

    val features = FeatureFlagsImpl

    // --- Premium (free download since 1.4.0, see docs/FREEMIUM_KONZEPT.md) ---

    /** What this user may use; combines purchases with the former-buyer rule. */
    val access: StateFlow<Access> = AccessState.access

    /** The Premium unlock sheet, opened by every locked element. */
    var showPremium by mutableStateOf(false)

    /** True when Premium is unlocked; otherwise opens the unlock sheet. */
    fun requirePremium(): Boolean {
        if (access.value.premium) return true
        showPremium = true
        return false
    }

    fun customPresetLimit(): Int =
        if (access.value.premium) features.maxCustomPresets else Access.FREE_CUSTOM_PRESETS

    fun purchase(activity: Activity, productId: String, onResult: (PurchaseResult) -> Unit) =
        EntitlementsImpl.purchase(activity, productId, onResult)

    val prices: StateFlow<Map<String, String>> get() = EntitlementsImpl.prices

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
    var showMixer by mutableStateOf(false)
    var showRhythm by mutableStateOf(false)

    // --- Onboarding & store review prompt ---

    var showOnboarding by mutableStateOf(false)
        private set

    var showReviewPrompt by mutableStateOf(false)
        private set

    /**
     * The store prompt waits for the session rating dialog to close - showing
     * both at once would stack two dialogs on top of each other.
     */
    private var reviewPromptPending = false

    init {
        viewModelScope.launch {
            // Must run before anything else touches the settings store: the
            // marker decides "new install" from the store being empty.
            settingsRepo.ensureFirstSeenVersionCode(currentVersionCode())
            // Access needs it to recognise people who bought the former paid app.
            AccessState.firstSeenVersionCode.value = settingsRepo.firstSeenVersionCode()
            showOnboarding = !settingsRepo.isOnboardingSeen()
        }
    }

    private fun currentVersionCode(): Int {
        val info = app.packageManager.getPackageInfo(app.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info).toInt()
    }

    fun finishOnboarding() {
        showOnboarding = false
        viewModelScope.launch { settingsRepo.setOnboardingSeen() }
    }

    val storeUrl: String? get() = FeatureFlagsImpl.storeUrl

    /** Opens the store listing; the prompt never appears again once used. */
    fun openStorePage() {
        val url = FeatureFlagsImpl.storeUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            app.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No browser and no store app - nothing sensible left to do.
        }
        dismissReviewPrompt()
    }

    fun dismissReviewPrompt() {
        showReviewPrompt = false
        viewModelScope.launch { settingsRepo.setReviewPromptHandled() }
    }

    private fun recordCompletedSession() {
        viewModelScope.launch {
            val total = settingsRepo.incrementCompletedSessions()
            reviewPromptPending = ReviewPromptDecision.shouldPrompt(
                completedSessions = total,
                alreadyHandled = settingsRepo.isReviewPromptHandled(),
                storeAvailable = FeatureFlagsImpl.storeUrl != null
            )
        }
    }

    private fun showReviewPromptIfPending() {
        if (reviewPromptPending) {
            reviewPromptPending = false
            showReviewPrompt = true
        }
    }

    // --- Ambient mixer & sleep timer ---

    val ambientVolumes = mutableStateMapOf<AmbientSound, Float>().apply {
        AmbientSound.entries.forEach { put(it, 0f) }
    }

    var isAmbientPlaying by mutableStateOf(false)
        private set

    var sleepTimerMinutes by mutableIntStateOf(0)
        private set

    var sleepTimerRemainingSec by mutableIntStateOf(0)
        private set

    init {
        viewModelScope.launch {
            val saved = settingsRepo.ambientVolumes.first()
            saved.forEach { (name, volume) ->
                AmbientSound.entries.find { it.name == name }?.let { ambientVolumes[it] = volume }
            }
        }
    }

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
            recordCompletedSession()
        }
        svc.onSleepTimerTick = { seconds ->
            sleepTimerRemainingSec = seconds
        }
        svc.onSleepTimerFinished = {
            isPlaying = false
            isPaused = false
            isAmbientPlaying = false
            isRhythmPlaying = false
            sleepTimerMinutes = 0
            sleepTimerRemainingSec = 0
            currentGuidance = null
        }
        isAmbientPlaying = svc.ambient.isPlaying
        isRhythmPlaying = svc.rhythm.isPlaying
        svc.spokenCuesEnabled = rhythmCuesEnabled
        svc.onRhythmStepChanged = { step, remaining ->
            rhythmCurrentStep = step
            rhythmRemainingSec = remaining
            if (step == null) isRhythmPlaying = false
        }
        pushRhythmToEngine()

        // The service (foreground, own lifecycle) may already be playing when this
        // ViewModel is (re)created - e.g. Activity/process recreated while the tone
        // kept running in the background. Without this, the UI shows "Start" over a
        // session that is actually still playing.
        computeRestoredPlaybackState(
            generatorIsPlaying = svc.generator.isPlaying,
            generatorIsPaused = svc.generator.isPaused,
            generatorCurrentPhaseIndex = svc.generator.currentPhaseIndex,
            lastPlaybackParams = svc.lastPlaybackParams
        )?.let { restored ->
            restored.phases?.let { editablePhases = it }
            restored.carrier?.let { carrierFrequency = it }
            restored.volume?.let { masterVolume = it }
            restored.noiseVolume?.let { noiseVolume = it }
            restored.transitionMs?.let { transitionTimeMs = it }
            isPlaying = true
            isPaused = restored.isPaused
            currentPhaseIndex = restored.currentPhaseIndex
            val phase = editablePhases.getOrNull(currentPhaseIndex)
            currentGuidance = if (phase != null && phase.guidanceRes != 0) {
                app.getString(phase.guidanceRes)
            } else {
                phase?.guidance
            }
        }
    }

    // --- Ambient mixer ---

    fun setAmbientVolume(sound: AmbientSound, volume: Float) {
        ambientVolumes[sound] = volume
        service?.ambient?.setVolume(sound, volume)
        viewModelScope.launch {
            settingsRepo.setAmbientVolumes(ambientVolumes.entries.associate { it.key.name to it.value })
        }
    }

    fun toggleAmbient() {
        val svc = service ?: return
        if (isAmbientPlaying) {
            svc.stopAmbient()
            isAmbientPlaying = false
        } else {
            // Starting an all-silent mix would confuse — default to gentle rain.
            if (ambientVolumes.values.none { it > 0.001f }) {
                setAmbientVolume(AmbientSound.RAIN, 0.5f)
            }
            AmbientSound.entries.forEach { svc.ambient.setVolume(it, ambientVolumes[it] ?: 0f) }
            svc.startAmbient()
            isAmbientPlaying = true
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerMinutes = minutes
        sleepTimerRemainingSec = minutes * 60
        service?.setSleepTimer(minutes)
    }

    // --- Preset selection ---

    /** False when the preset is Premium and locked; the unlock sheet opens instead. */
    fun selectPreset(preset: Preset): Boolean {
        if (preset.category.isPremium && !requirePremium()) return false
        selectedPreset = preset
        selectedCustomPresetId = null
        editablePhases = preset.phases.toList()
        preset.carrierOverride?.let { carrierFrequency = it }
        isEditing = false
        return true
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
                if (current >= customPresetLimit()) {
                    customPresetLimitReached = true
                    // Free users are pointed at what lifts the limit.
                    if (!access.value.premium && features.isPremium) showPremium = true
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
            showReviewPromptIfPending()
        }
    }

    fun dismissRating() {
        showRatingDialog = false
        showReviewPromptIfPending()
    }

    fun deleteJournalEntry(id: String) {
        viewModelScope.launch { journalRepo.delete(id) }
    }

    // --- WAV Export ---

    fun exportWav() {
        if (isExporting || !features.wavExportEnabled) return
        if (!requirePremium()) return
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
                rhythmPulsesAt = rhythmPulsesForExport(),
                rhythmVolume = rhythmVolume,
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
        service?.cancelSleepTimer()
        service?.stopAmbient()
        super.onCleared()
    }

    // --- Rhythm layer ---

    /** Whether this build has the layer at all (premium only). */
    val rhythmLayerAvailable: Boolean get() = FeatureFlagsImpl.rhythmLayerAvailable

    /** Whether the user may use it: bought on its own or with the bundle. */
    val rhythmLayerOwned: StateFlow<Boolean> = access.map { it.rhythm }
        .stateIn(viewModelScope, SharingStarted.Eagerly, access.value.rhythm)

    val rhythmLayerPrice: StateFlow<String?> = EntitlementsImpl.prices.map { it[Entitlements.PRODUCT_RHYTHM_LAYER] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun purchaseRhythmLayer(activity: Activity, onResult: (PurchaseResult) -> Unit) {
        EntitlementsImpl.purchase(activity, Entitlements.PRODUCT_RHYTHM_LAYER, onResult)
    }

    /**
     * Play already knows the purchase on every device of the account, so
     * "restore" is just asking again.
     */
    fun refreshEntitlements() = EntitlementsImpl.refresh()

    var isRhythmPlaying by mutableStateOf(false)
        private set

    var rhythmMode by mutableStateOf(RhythmMode.TEMPO)
        private set

    var rhythmBpm by mutableIntStateOf(120)
        private set

    var rhythmAccentEvery by mutableIntStateOf(4)
        private set

    var rhythmVolume by mutableFloatStateOf(0.6f)
        private set

    /**
     * The breath that the guide draws and the rhythm track ticks - one setting,
     * not two. Guide and sheet used to keep separate pickers, so the circle
     * could show 4-7-8 while the clicks played Box.
     */
    var breathPattern by mutableStateOf(BreathingPattern.RELAXING)
        private set

    var isBreathRunning by mutableStateOf(false)
        private set

    /** Start of the breath on the wall clock, used when no track is playing. */
    private var breathStartedAt = 0L

    /**
     * How far into the breath we are.
     *
     * While the audible track runs this comes from the AudioTrack, so the circle
     * follows what the user hears rather than what the app has computed - the
     * two are apart by the output latency, which is not small. Otherwise the
     * wall clock answers, which is what the guide needs on its own and in the
     * F-Droid build, where there is no rhythm track to ask.
     */
    fun breathElapsedMillis(): Long {
        val engine = service?.rhythm
        if (isRhythmPlaying && rhythmMode == RhythmMode.BREATH && engine?.isPlaying == true) {
            return engine.playedMillis
        }
        return SystemClock.elapsedRealtime() - breathStartedAt
    }

    fun toggleBreath() {
        if (isBreathRunning) isBreathRunning = false else startBreath()
    }

    /**
     * Starts the breath at zero. If the audible track is already running it is
     * restarted as well, so both begin on the same inhale rather than each
     * carrying on from wherever it happened to stand.
     */
    private fun startBreath() {
        breathStartedAt = SystemClock.elapsedRealtime()
        isBreathRunning = true
        if (isRhythmPlaying && rhythmMode == RhythmMode.BREATH) {
            val svc = service ?: return
            pushRhythmToEngine()
            // start() stops first, so the pulse scheduler begins at zero again.
            svc.startRhythm()
            isRhythmPlaying = svc.rhythm.isPlaying
        }
    }

    var rhythmCuesEnabled by mutableStateOf(false)
        private set

    fun updateRhythmCues(enabled: Boolean) {
        rhythmCuesEnabled = enabled
        service?.spokenCuesEnabled = enabled
        persistRhythm()
    }

    /** Steps of the multi-tempo program, edited in place by the sheet. */
    val rhythmSteps = mutableStateListOf<RhythmStep>().apply { addAll(defaultRhythmProgram) }

    /** The step the service is currently playing, null when no program runs. */
    var rhythmCurrentStep by mutableStateOf<RhythmStep?>(null)
        private set

    var rhythmRemainingSec by mutableIntStateOf(0)
        private set

    fun addRhythmStep() {
        rhythmSteps.add(rhythmSteps.lastOrNull() ?: RhythmStep(bpm = 110, durationMinutes = 5))
        persistRhythmProgram()
    }

    fun removeRhythmStep(index: Int) {
        if (index !in rhythmSteps.indices) return
        // A program with no steps would start and stop in the same tick.
        if (rhythmSteps.size <= 1) return
        rhythmSteps.removeAt(index)
        persistRhythmProgram()
    }

    fun updateRhythmStep(index: Int, step: RhythmStep) {
        if (index !in rhythmSteps.indices) return
        rhythmSteps[index] = step.copy(
            bpm = step.bpm.coerceIn(RhythmPattern.MIN_BPM, RhythmPattern.MAX_BPM),
            durationMinutes = step.durationMinutes.coerceIn(1, 180)
        )
        persistRhythmProgram()
    }

    private fun persistRhythmProgram() {
        viewModelScope.launch { settingsRepo.setRhythmProgram(rhythmSteps.toList()) }
    }

    init {
        EntitlementsImpl.connect(app)
        viewModelScope.launch {
            val saved = settingsRepo.rhythmSettings.first()
            rhythmMode = saved.mode
            rhythmBpm = saved.bpm
            rhythmAccentEvery = saved.accentEvery
            rhythmVolume = saved.volume
            breathPattern = runCatching { BreathingPattern.valueOf(saved.breathPatternName) }
                .getOrDefault(BreathingPattern.RELAXING)
            rhythmCuesEnabled = saved.cuesEnabled
            val savedProgram = settingsRepo.rhythmProgram.first()
            rhythmSteps.clear()
            rhythmSteps.addAll(savedProgram)
            pushRhythmToEngine()
        }
    }

    fun updateRhythmMode(mode: RhythmMode) {
        rhythmMode = mode
        pushRhythmToEngine()
        persistRhythm()
    }

    fun updateRhythmBpm(bpm: Int) {
        rhythmBpm = bpm.coerceIn(RhythmPattern.MIN_BPM, RhythmPattern.MAX_BPM)
        pushRhythmToEngine()
        persistRhythm()
    }

    fun updateRhythmAccentEvery(accentEvery: Int) {
        rhythmAccentEvery = accentEvery
        pushRhythmToEngine()
        persistRhythm()
    }

    fun updateBreathPattern(pattern: BreathingPattern) {
        breathPattern = pattern
        pushRhythmToEngine()
        // A different pattern is a different breath. Restarting puts circle and
        // clicks back on a shared inhale instead of leaving both mid-phase in a
        // rhythm that no longer exists.
        if (isBreathRunning) startBreath()
        persistRhythm()
    }

    fun updateRhythmVolume(volume: Float) {
        rhythmVolume = volume.coerceIn(0f, 1f)
        service?.rhythm?.volume = rhythmVolume
        persistRhythm()
    }

    fun toggleRhythm() {
        val svc = service ?: return
        if (isRhythmPlaying) {
            // Hand the breath back to the wall clock at the exact point the
            // audio had reached, so the circle does not jump by the output
            // latency the moment the clicks stop.
            val heard = breathElapsedMillis()
            svc.stopRhythm()
            isRhythmPlaying = false
            rhythmCurrentStep = null
            rhythmRemainingSec = 0
            if (isBreathRunning) breathStartedAt = SystemClock.elapsedRealtime() - heard
        } else if (rhythmMode == RhythmMode.PROGRAM) {
            // The service drives a program over time; the engine only ever
            // receives whatever pattern the current step produces.
            svc.rhythm.volume = rhythmVolume
            svc.spokenCuesEnabled = rhythmCuesEnabled
            svc.startRhythmProgram(rhythmSteps.toList())
            isRhythmPlaying = true
        } else {
            pushRhythmToEngine()
            svc.startRhythm()
            isRhythmPlaying = svc.rhythm.isPlaying
            // In breath mode the audible track defines the phase, so the guide
            // starts with it rather than continuing from where it stood.
            if (rhythmMode == RhythmMode.BREATH && isRhythmPlaying) {
                breathStartedAt = SystemClock.elapsedRealtime()
                isBreathRunning = true
            }
        }
    }

    /**
     * The engine only ever sees a list of pulses - which of the two modes
     * produced it is decided here and nowhere else.
     */
    private fun pushRhythmToEngine() {
        val engine = service?.rhythm ?: return
        engine.volume = rhythmVolume
        // While a program runs, the service owns the pattern - overwriting it
        // here would freeze the tempo at whatever the sheet last showed.
        if (rhythmMode == RhythmMode.PROGRAM) return
        engine.pattern = when (rhythmMode) {
            RhythmMode.PROGRAM -> return
            RhythmMode.TEMPO -> RhythmPattern.metronome(rhythmBpm, rhythmAccentEvery)
            RhythmMode.BREATH -> breathPattern.let {
                RhythmPattern.breathing(it.inhale, it.hold1, it.exhale, it.hold2)
            }
        }
        engine.volume = rhythmVolume
    }

    /**
     * The rhythm track as the exporter needs it, or null when there is nothing
     * to mix in. Only owned add-ons contribute - an export must not hand out
     * what was not bought - and only a rhythm the user actually has running,
     * so a silent sheet does not quietly end up in the file.
     */
    private fun rhythmPulsesForExport(): ((Int) -> List<RhythmPulse>)? {
        if (!FeatureFlagsImpl.rhythmLayerAvailable) return null
        if (!access.value.rhythm) return null
        if (!isRhythmPlaying) return null

        if (rhythmMode == RhythmMode.PROGRAM) {
            val steps = rhythmSteps.toList()
            return { seconds -> stepAt(steps, seconds)?.pulses ?: emptyList() }
        }

        val fixed = when (rhythmMode) {
            RhythmMode.TEMPO -> RhythmPattern.metronome(rhythmBpm, rhythmAccentEvery)
            RhythmMode.BREATH -> breathPattern.let {
                RhythmPattern.breathing(it.inhale, it.hold1, it.exhale, it.hold2)
            }
            RhythmMode.PROGRAM -> emptyList()
        }
        return { _ -> fixed }
    }

    private fun persistRhythm() {
        viewModelScope.launch {
            settingsRepo.setRhythmSettings(
                RhythmSettings(
                    mode = rhythmMode,
                    bpm = rhythmBpm,
                    accentEvery = rhythmAccentEvery,
                    volume = rhythmVolume,
                    breathPatternName = breathPattern.name,
                    cuesEnabled = rhythmCuesEnabled
                )
            )
        }
    }

}
