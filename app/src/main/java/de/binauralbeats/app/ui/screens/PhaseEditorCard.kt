package de.binauralbeats.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.R
import de.binauralbeats.app.audio.BinauralGenerator
import de.binauralbeats.app.data.BackgroundNoise
import de.binauralbeats.app.data.ModulationType
import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.data.ToneType
import de.binauralbeats.app.ui.BinauralViewModel
import de.binauralbeats.app.ui.theme.*

@Composable
fun PhaseEditorCard(
    viewModel: BinauralViewModel,
    activeIndex: Int,
    isPlaying: Boolean
) {
    val generator = remember { BinauralGenerator() }
    val phases = viewModel.editablePhases
    val totalMinutes = phases.sumOf { it.durationMinutes }

    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.phases_header),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPrimary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        stringResource(R.string.phases_info, phases.size, totalMinutes),
                        fontSize = 11.sp,
                        color = OnSurfaceMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (viewModel.isModified) {
                        FilledTonalButton(
                            onClick = { viewModel.resetToOriginal() },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(0.08f)
                            )
                        ) {
                            Icon(Icons.Default.Restore, null, Modifier.size(14.dp), tint = OnSurfaceMuted)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.reset), fontSize = 10.sp, color = OnSurfaceMuted)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.isEditing = !viewModel.isEditing },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (viewModel.isEditing) Icons.Default.ExpandLess else Icons.Default.Edit,
                            null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Phase list
            phases.forEachIndexed { idx, phase ->
                PhaseRow(
                    index = idx,
                    phase = phase,
                    isActive = isPlaying && idx == activeIndex,
                    isEditing = viewModel.isEditing,
                    isLastPhase = phases.size <= 1,
                    generator = generator,
                    onUpdate = { viewModel.updatePhase(idx, it) },
                    onDuplicate = { viewModel.duplicatePhase(idx) },
                    onRemove = { viewModel.removePhase(idx) }
                )
            }

            // Add + Save buttons
            AnimatedVisibility(visible = viewModel.isEditing) {
                Column {
                    Spacer(Modifier.height(8.dp))

                    // Add phase
                    OutlinedButton(
                        onClick = { viewModel.addPhase() },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPrimary),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(AccentPrimary.copy(0.3f), AccentPrimary.copy(0.3f))
                            )
                        )
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_phase), fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Save as preset
                    Button(
                        onClick = { viewModel.showSaveDialog = true },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Icon(Icons.Default.Save, null, Modifier.size(16.dp), tint = PrimaryDark)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.save_as_preset), fontSize = 12.sp, color = PrimaryDark)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhaseRow(
    index: Int,
    phase: Phase,
    isActive: Boolean,
    isEditing: Boolean,
    isLastPhase: Boolean,
    generator: BinauralGenerator,
    onUpdate: (Phase) -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        when {
            isActive -> AccentPrimary.copy(alpha = 0.1f)
            expanded && isEditing -> Color.White.copy(alpha = 0.04f)
            else -> Color.Transparent
        },
        label = "phaseBg"
    )

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Column {
            // Compact row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isEditing) { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isActive) AccentPrimary else Color.White.copy(0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) PrimaryDark else Color.White.copy(0.6f)
                        )
                    }
                    Column {
                        Text(
                            "${phase.frequency} Hz · ${stringResource(generator.getBandLabelRes(phase.frequency))}",
                            fontSize = 13.sp,
                            color = if (isActive) Color.White else Color.White.copy(0.7f),
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            "${phase.modulation.name.lowercase()} · ${phase.durationMinutes} min",
                            fontSize = 11.sp,
                            color = OnSurfaceMuted
                        )
                    }
                }

                if (isEditing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp), tint = OnSurfaceMuted)
                        }
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(28.dp),
                            enabled = !isLastPhase
                        ) {
                            Icon(
                                Icons.Default.Delete, null, Modifier.size(14.dp),
                                tint = if (isLastPhase) Color.White.copy(0.15f) else Color(0xFFFF8A8A)
                            )
                        }
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            Modifier.size(16.dp),
                            tint = OnSurfaceMuted
                        )
                    }
                }
            }

            // Expanded editor
            AnimatedVisibility(visible = expanded && isEditing) {
                PhaseInlineEditor(phase = phase, onUpdate = onUpdate)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhaseInlineEditor(
    phase: Phase,
    onUpdate: (Phase) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Frequency slider (0.1 - 40 Hz)
        LabeledSlider(
            label = stringResource(R.string.frequency_label),
            value = phase.frequency,
            valueRange = 0.1f..40f,
            displayText = "${"%.1f".format(phase.frequency)} Hz",
            onValueChange = { onUpdate(phase.copy(frequency = "%.1f".format(it).toFloat())) }
        )

        // Duration slider (1 - 180 min)
        LabeledSlider(
            label = stringResource(R.string.duration_label),
            value = phase.durationMinutes.toFloat(),
            valueRange = 1f..180f,
            displayText = "${phase.durationMinutes} min",
            onValueChange = { onUpdate(phase.copy(durationMinutes = it.toInt())) }
        )

        // Modulation dropdown
        LabeledDropdown(
            label = stringResource(R.string.modulation_label),
            selected = phase.modulation.name,
            options = ModulationType.entries.map { it.name },
            onSelect = { onUpdate(phase.copy(modulation = ModulationType.valueOf(it))) }
        )

        // Background noise dropdown
        LabeledDropdown(
            label = stringResource(R.string.background_label),
            selected = phase.background.name,
            options = BackgroundNoise.entries.map { it.name },
            onSelect = { onUpdate(phase.copy(background = BackgroundNoise.valueOf(it))) }
        )

        // Tone type dropdown
        LabeledDropdown(
            label = stringResource(R.string.tone_type_label),
            selected = phase.toneType.name,
            options = ToneType.entries.map { it.name },
            onSelect = { onUpdate(phase.copy(toneType = ToneType.valueOf(it))) }
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = OnSurfaceMuted)
            Text(displayText, fontSize = 11.sp, color = AccentPrimary, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = AccentPrimary,
                activeTrackColor = AccentPrimary,
                inactiveTrackColor = Color.White.copy(0.08f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = OnSurfaceMuted)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Surface(
                onClick = { expanded = true },
                color = Color.White.copy(0.06f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.menuAnchor()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selected.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = AccentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ExpandMore, null, Modifier.size(14.dp), tint = OnSurfaceMuted)
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = SurfaceDark
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                color = if (option == selected) AccentPrimary else Color.White.copy(0.7f)
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SavePresetDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(stringResource(R.string.save_preset_title), color = Color.White)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.preset_name_label), color = OnSurfaceMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = Color.White.copy(0.15f),
                    focusedLabelColor = AccentPrimary,
                    cursorColor = AccentPrimary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save), color = PrimaryDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = OnSurfaceMuted)
            }
        }
    )
}
