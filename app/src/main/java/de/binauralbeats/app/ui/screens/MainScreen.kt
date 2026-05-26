package de.binauralbeats.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import android.content.Intent
import de.binauralbeats.app.FeatureFlagsImpl
import de.binauralbeats.app.R
import de.binauralbeats.app.audio.BinauralGenerator
import de.binauralbeats.app.data.CustomPreset
import de.binauralbeats.app.data.PresetCategory
import de.binauralbeats.app.data.Presets
import de.binauralbeats.app.ui.BinauralViewModel
import de.binauralbeats.app.ui.components.BreathingGuide
import de.binauralbeats.app.ui.components.FrequencyCurve
import de.binauralbeats.app.ui.components.WaveformVisualizer
import de.binauralbeats.app.ui.theme.LocalBinauralColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BinauralViewModel) {
    val colors = LocalBinauralColors.current
    val generator = remember { BinauralGenerator() }
    val customPresets by viewModel.customPresets.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val languageTag by viewModel.languageTag.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(colors.surfaceDark, colors.primaryDark, colors.primaryMid, colors.surfaceVariant)
                    )
                )
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.app_name),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        stringResource(R.string.app_subtitle),
                        fontSize = 13.sp,
                        color = colors.onSurfaceMuted,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Headphones hint + Journal + Settings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = colors.accentPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Headphones, null, tint = colors.accentPrimary, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.headphones_recommended), fontSize = 12.sp, color = colors.accentPrimary)
                        }
                    }

                    Surface(
                        onClick = { viewModel.showJournal = true },
                        color = colors.accentPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Book, null, tint = colors.accentPrimary, modifier = Modifier.size(20.dp))
                            if (journalEntries.isNotEmpty()) {
                                Text(
                                    "${journalEntries.size}",
                                    fontSize = 12.sp,
                                    color = colors.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = { viewModel.showSettings = true },
                        color = colors.accentPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Default.Settings, null, tint = colors.accentPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Waveform Visualizer
            item {
                val currentFreq = viewModel.editablePhases
                    .getOrNull(viewModel.currentPhaseIndex)?.frequency ?: 10f
                WaveformVisualizer(
                    beatFrequency = currentFreq,
                    isPlaying = viewModel.isPlaying
                )
            }

            // Frequency Curve (phase profile)
            item {
                FrequencyCurve(
                    phases = viewModel.editablePhases,
                    totalProgress = viewModel.totalProgress,
                    isPlaying = viewModel.isPlaying
                )
            }

            // Progress
            if (viewModel.isPlaying || viewModel.totalProgress > 0f) {
                item {
                    Column {
                        val animatedProgress by animateFloatAsState(
                            targetValue = viewModel.totalProgress,
                            label = "progress"
                        )
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = colors.accentPrimary,
                            trackColor = colors.overlay.copy(alpha = 0.06f)
                        )

                        val phases = viewModel.editablePhases
                        val phase = phases.getOrNull(viewModel.currentPhaseIndex)
                        if (phase != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(R.string.phase_info, viewModel.currentPhaseIndex + 1, phases.size),
                                    fontSize = 12.sp,
                                    color = colors.onSurfaceMuted
                                )
                                Text(
                                    stringResource(generator.getBandLabelRes(phase.frequency)),
                                    fontSize = 12.sp,
                                    color = colors.accentPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${phase.frequency} Hz · ${phase.durationMinutes} min",
                                    fontSize = 12.sp,
                                    color = colors.onSurfaceMuted
                                )
                            }
                        }
                    }
                }
            }

            // Guidance text
            viewModel.currentGuidance?.let { guidance ->
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                        Surface(
                            color = colors.accentPrimary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                guidance,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 14.sp,
                                color = colors.accentPrimary,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            // Play controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.isPlaying) {
                        FilledIconButton(
                            onClick = { viewModel.togglePlayback() },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.accentPrimary
                            )
                        ) {
                            Icon(
                                if (viewModel.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = stringResource(if (viewModel.isPaused) R.string.resume else R.string.pause),
                                modifier = Modifier.size(32.dp),
                                tint = colors.onAccent
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        OutlinedIconButton(
                            onClick = { viewModel.stop() },
                            modifier = Modifier.size(48.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = Brush.linearGradient(listOf(Color(0x66FF6B6B), Color(0x66FF6B6B)))
                            )
                        ) {
                            Icon(Icons.Default.Stop, stringResource(R.string.stop), tint = Color(0xFFFF8A8A))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.play() },
                            modifier = Modifier
                                .height(56.dp)
                                .widthIn(min = 200.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentPrimary),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = colors.onAccent)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.start_label),
                                color = colors.onAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // WAV Export + Share
            item {
                WavExportSection(viewModel)
            }

            // Breathing Guide
            item {
                BreathingGuide(isActive = viewModel.isPlaying)
            }

            // Controls (Carrier + Volume + Noise + Transition)
            item {
                ControlsSection(viewModel)
            }

            // Phase Editor
            item {
                PhaseEditorCard(
                    viewModel = viewModel,
                    activeIndex = viewModel.currentPhaseIndex,
                    isPlaying = viewModel.isPlaying
                )
            }

            // Presets by category
            item {
                Text(
                    stringResource(R.string.presets_header),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentPrimary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Custom presets section
            if (customPresets.isNotEmpty()) {
                item {
                    CustomPresetsCard(
                        presets = customPresets,
                        selectedId = viewModel.selectedCustomPresetId,
                        onSelect = { custom ->
                            viewModel.selectCustomPreset(custom)
                            if (!viewModel.isPlaying) viewModel.play()
                        },
                        onDelete = { viewModel.deleteCustomPreset(it) }
                    )
                }
            }

            if (viewModel.customPresetLimitReached) {
                item {
                    Surface(
                        color = Color(0x33FF6B6B),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.preset_limit_reached, viewModel.features.maxCustomPresets),
                                fontSize = 12.sp,
                                color = Color(0xFFFF8A8A),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "✕",
                                fontSize = 14.sp,
                                color = colors.onSurfaceMuted,
                                modifier = Modifier.clickable { viewModel.customPresetLimitReached = false }
                            )
                        }
                    }
                }
            }

            val categories = Presets.byCategory.entries.toList()
            items(categories, key = { it.key }) { (category, presets) ->
                PresetCategoryCard(
                    category = category,
                    presets = presets,
                    selectedKey = viewModel.selectedPreset?.key,
                    onSelect = { preset ->
                        viewModel.selectPreset(preset)
                        if (!viewModel.isPlaying) viewModel.play()
                    }
                )
            }

            // Bottom spacer
            item { Spacer(Modifier.height(32.dp)) }
        }

        // --- Dialogs & Overlays ---

        if (viewModel.showSaveDialog) {
            SavePresetDialog(
                initialName = viewModel.selectedPreset?.let {
                    stringResource(R.string.modified_preset_name, stringResource(it.nameRes))
                } ?: stringResource(R.string.default_preset_name),
                onSave = { name -> viewModel.saveAsCustomPreset(name) },
                onDismiss = { viewModel.showSaveDialog = false }
            )
        }

        if (viewModel.showRatingDialog) {
            RatingDialog(
                presetName = viewModel.activePresetName,
                onSave = { rating, moods, notes ->
                    viewModel.saveJournalEntry(rating, moods, notes)
                },
                onDismiss = { viewModel.dismissRating() }
            )
        }

        if (viewModel.showJournal) {
            JournalOverlay(
                entries = journalEntries,
                onDelete = { viewModel.deleteJournalEntry(it) },
                onClose = { viewModel.showJournal = false }
            )
        }

        if (viewModel.showSettings) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                SettingsSheet(
                    currentTheme = themeMode,
                    currentLanguage = languageTag,
                    onThemeChange = { viewModel.setThemeMode(it) },
                    onLanguageChange = { viewModel.setLanguage(it) },
                    onClose = { viewModel.showSettings = false }
                )
            }
        }
    }
}

@Composable
private fun ControlsSection(viewModel: BinauralViewModel) {
    val colors = LocalBinauralColors.current

    Surface(
        color = colors.overlay.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.carrier_frequency), fontSize = 12.sp, color = colors.accentPrimary, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = viewModel.carrierFrequency,
                    onValueChange = { viewModel.carrierFrequency = it },
                    valueRange = 100f..500f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentPrimary,
                        activeTrackColor = colors.accentPrimary
                    )
                )
                Text(
                    "${viewModel.carrierFrequency.toInt()} Hz",
                    fontSize = 13.sp,
                    color = colors.accentPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(stringResource(R.string.volume), fontSize = 12.sp, color = colors.accentPrimary, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = viewModel.masterVolume,
                    onValueChange = { viewModel.masterVolume = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentPrimary,
                        activeTrackColor = colors.accentPrimary
                    )
                )
                Text(
                    "${(viewModel.masterVolume * 100).toInt()}%",
                    fontSize = 13.sp,
                    color = colors.accentPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(stringResource(R.string.noise_volume), fontSize = 12.sp, color = colors.accentPrimary, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = viewModel.noiseVolume,
                    onValueChange = { viewModel.noiseVolume = it },
                    valueRange = 0f..0.5f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentPrimary,
                        activeTrackColor = colors.accentPrimary
                    )
                )
                Text(
                    "${(viewModel.noiseVolume * 100).toInt()}%",
                    fontSize = 13.sp,
                    color = colors.accentPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(stringResource(R.string.transition_time), fontSize = 12.sp, color = colors.accentPrimary, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = viewModel.transitionTimeMs.toFloat(),
                    onValueChange = { viewModel.transitionTimeMs = it.toInt() },
                    valueRange = 0f..3000f,
                    steps = 5,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentPrimary,
                        activeTrackColor = colors.accentPrimary
                    )
                )
                Text(
                    "${viewModel.transitionTimeMs} ms",
                    fontSize = 13.sp,
                    color = colors.accentPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun WavExportSection(viewModel: BinauralViewModel) {
    val colors = LocalBinauralColors.current

    Surface(
        color = colors.overlay.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.wav_export),
                        fontSize = 12.sp,
                        color = colors.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.wav_export_desc),
                        fontSize = 11.sp,
                        color = colors.onSurfaceMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val context = LocalContext.current
                    FilledIconButton(
                        onClick = {
                            val uri = viewModel.generateShareUri()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, uri.toString())
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_via)))
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.accentPrimary.copy(alpha = 0.15f)
                        )
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.share),
                            tint = colors.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (FeatureFlagsImpl.wavExportEnabled) {
                        if (viewModel.isExporting) {
                            CircularProgressIndicator(
                                progress = { viewModel.exportProgress },
                                modifier = Modifier.size(36.dp),
                                color = colors.accentPrimary,
                                strokeWidth = 3.dp
                            )
                        } else {
                            FilledIconButton(
                                onClick = { viewModel.exportWav() },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colors.accentPrimary.copy(alpha = 0.15f)
                                )
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = stringResource(R.string.export),
                                    tint = colors.accentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (viewModel.isExporting) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { viewModel.exportProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colors.accentPrimary,
                    trackColor = colors.overlay.copy(alpha = 0.06f)
                )
                Text(
                    "${(viewModel.exportProgress * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = colors.onSurfaceMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            viewModel.exportResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                val isError = viewModel.isExportError
                Surface(
                    color = if (isError) Color(0x33FF6B6B) else colors.accentPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            result,
                            fontSize = 12.sp,
                            color = if (isError) Color(0xFFFF8A8A) else colors.accentPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "✕",
                            fontSize = 14.sp,
                            color = colors.onSurfaceMuted,
                            modifier = Modifier.clickable { viewModel.exportResult = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCategoryCard(
    category: PresetCategory,
    presets: List<de.binauralbeats.app.data.Preset>,
    selectedKey: String?,
    onSelect: (de.binauralbeats.app.data.Preset) -> Unit
) {
    val colors = LocalBinauralColors.current
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = colors.overlay.copy(alpha = 0.04f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${category.emoji} ${stringResource(category.labelRes)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceMuted
                )
                Text(
                    if (expanded) "▾" else "▸",
                    fontSize = 12.sp,
                    color = colors.onSurfaceMuted
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    presets.forEach { preset ->
                        val isSelected = preset.key == selectedKey
                        val bgColor by animateColorAsState(
                            if (isSelected) colors.accentPrimary.copy(alpha = 0.15f)
                            else Color.Transparent,
                            label = "presetBg"
                        )
                        val borderColor = if (isSelected) colors.accentPrimary else colors.overlay.copy(alpha = 0.15f)

                        Surface(
                            onClick = { onSelect(preset) },
                            color = bgColor,
                            shape = RoundedCornerShape(24.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = Brush.linearGradient(listOf(borderColor, borderColor))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${preset.emoji} ${stringResource(preset.nameRes)}",
                                    fontSize = 13.sp,
                                    color = if (isSelected) colors.accentPrimary else colors.onSurface.copy(0.7f),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    "${preset.totalDurationMinutes} min",
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomPresetsCard(
    presets: List<CustomPreset>,
    selectedId: String?,
    onSelect: (CustomPreset) -> Unit,
    onDelete: (String) -> Unit
) {
    val colors = LocalBinauralColors.current
    var expanded by remember { mutableStateOf(true) }

    Surface(
        color = colors.accentPrimary.copy(alpha = 0.06f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🎵 ${stringResource(R.string.custom_presets)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentPrimary
                )
                Text(
                    if (expanded) "▾" else "▸",
                    fontSize = 12.sp,
                    color = colors.accentPrimary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    presets.forEach { preset ->
                        val isSelected = preset.id == selectedId
                        val bgColor by animateColorAsState(
                            if (isSelected) colors.accentPrimary.copy(alpha = 0.15f)
                            else Color.Transparent,
                            label = "customBg"
                        )
                        val borderColor = if (isSelected) colors.accentPrimary else colors.overlay.copy(alpha = 0.15f)

                        Surface(
                            onClick = { onSelect(preset) },
                            color = bgColor,
                            shape = RoundedCornerShape(24.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = Brush.linearGradient(listOf(borderColor, borderColor))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${preset.emoji} ${preset.name}",
                                    fontSize = 13.sp,
                                    color = if (isSelected) colors.accentPrimary else colors.onSurface.copy(0.7f),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${preset.totalDurationMinutes} min",
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceMuted
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onDelete(preset.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, null,
                                        Modifier.size(14.dp),
                                        tint = colors.onSurface.copy(0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
