package de.binauralbeats.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.R
import de.binauralbeats.app.data.RhythmMode
import de.binauralbeats.app.data.RhythmPattern
import de.binauralbeats.app.data.RhythmStep
import de.binauralbeats.app.ui.components.BreathingPattern
import de.binauralbeats.app.ui.theme.LocalBinauralColors

/**
 * Controls for the audible tempo track. Three modes share one engine: a fixed
 * tempo, a program that changes tempo over time, or one cue per breath phase
 * so the pulse lines up with the breathing guide on the main screen.
 */
@Composable
fun RhythmSheet(
    mode: RhythmMode,
    bpm: Int,
    accentEvery: Int,
    breathPattern: BreathingPattern,
    steps: List<RhythmStep>,
    currentStep: RhythmStep?,
    remainingSeconds: Int,
    volume: Float,
    isPlaying: Boolean,
    isUnlocked: Boolean,
    price: String?,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    /** Opens the bundle offer; null while the bundle is not on offer. */
    onShowBundle: (() -> Unit)? = null,
    onModeChange: (RhythmMode) -> Unit,
    onBpmChange: (Int) -> Unit,
    onAccentChange: (Int) -> Unit,
    onBreathPatternChange: (BreathingPattern) -> Unit,
    onStepChange: (Int, RhythmStep) -> Unit,
    onStepRemove: (Int) -> Unit,
    onStepAdd: () -> Unit,
    cuesEnabled: Boolean,
    onCuesChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalBinauralColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceDark.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.rhythm_header),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentPrimary,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, stringResource(R.string.close), tint = colors.onSurface)
                }
            }

            HorizontalDivider(color = colors.overlay.copy(0.06f))
            Spacer(Modifier.height(12.dp))

            Text(
                stringResource(R.string.rhythm_hint),
                fontSize = 12.sp,
                color = colors.onSurfaceMuted,
                lineHeight = 18.sp
            )

            if (!isUnlocked) {
                Spacer(Modifier.height(20.dp))
                RhythmLockedCard(price = price, onPurchase = onPurchase, onRestore = onRestore, onShowBundle = onShowBundle)
                Spacer(Modifier.height(24.dp))
                return@Column
            }

            Spacer(Modifier.height(20.dp))

            RhythmLabel(stringResource(R.string.rhythm_mode))
            RhythmChipRow(
                options = listOf(
                    RhythmMode.TEMPO to stringResource(R.string.rhythm_mode_tempo),
                    RhythmMode.PROGRAM to stringResource(R.string.rhythm_mode_program),
                    RhythmMode.BREATH to stringResource(R.string.rhythm_mode_breath)
                ),
                selected = mode,
                onSelect = onModeChange
            )

            Spacer(Modifier.height(20.dp))

            when (mode) {
                RhythmMode.TEMPO -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RhythmLabel(stringResource(R.string.rhythm_tempo))
                        Text(
                            stringResource(R.string.rhythm_bpm, bpm),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentPrimary
                        )
                    }
                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = { onBpmChange(it.toInt()) },
                        valueRange = RhythmPattern.MIN_BPM.toFloat()..RhythmPattern.MAX_BPM.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accentPrimary,
                            activeTrackColor = colors.accentPrimary
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    RhythmLabel(stringResource(R.string.rhythm_accent))
                    RhythmChipRow(
                        options = listOf(
                            0 to stringResource(R.string.rhythm_accent_off),
                            2 to stringResource(R.string.rhythm_accent_every, 2),
                            3 to stringResource(R.string.rhythm_accent_every, 3),
                            4 to stringResource(R.string.rhythm_accent_every, 4)
                        ),
                        selected = accentEvery,
                        onSelect = onAccentChange
                    )
                }

                RhythmMode.PROGRAM -> {
                    Text(
                        stringResource(R.string.rhythm_program_hint),
                        fontSize = 12.sp,
                        color = colors.onSurfaceMuted,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    steps.forEachIndexed { index, step ->
                        RhythmStepRow(
                            step = step,
                            isCurrent = step === currentStep,
                            canRemove = steps.size > 1,
                            onChange = { onStepChange(index, it) },
                            onRemove = { onStepRemove(index) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Surface(
                        onClick = onStepAdd,
                        color = colors.overlay.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                stringResource(R.string.rhythm_add_step),
                                fontSize = 13.sp,
                                color = colors.accentPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    RhythmLabel(stringResource(R.string.rhythm_cues))
                    RhythmChipRow(
                        options = listOf(
                            false to stringResource(R.string.rhythm_cues_off),
                            true to stringResource(R.string.rhythm_cues_on)
                        ),
                        selected = cuesEnabled,
                        onSelect = onCuesChange
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.rhythm_cues_hint),
                        fontSize = 11.sp,
                        color = colors.onSurfaceMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (currentStep != null) stringResource(
                            R.string.rhythm_program_running,
                            currentStep.bpm,
                            formatRemaining(remainingSeconds)
                        ) else stringResource(
                            R.string.rhythm_program_total,
                            steps.size,
                            steps.sumOf { it.durationMinutes }
                        ),
                        fontSize = 12.sp,
                        color = if (currentStep != null) colors.accentPrimary else colors.onSurfaceMuted
                    )
                }

                RhythmMode.BREATH -> {
                    RhythmChipRow(
                        options = BreathingPattern.entries.map { it to stringResource(it.labelRes) },
                        selected = breathPattern,
                        onSelect = onBreathPatternChange
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.rhythm_breath_hint),
                        fontSize = 12.sp,
                        color = colors.onSurfaceMuted,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RhythmLabel(stringResource(R.string.rhythm_volume))
                Text(
                    "${(volume * 100).toInt()}%",
                    fontSize = 13.sp,
                    color = colors.onSurfaceMuted
                )
            }
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accentPrimary,
                    activeTrackColor = colors.accentPrimary
                )
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onTogglePlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.onAccent
                )
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Text(
                    "  " + stringResource(
                        when {
                            mode == RhythmMode.PROGRAM && isPlaying -> R.string.rhythm_stop_program
                            mode == RhythmMode.PROGRAM -> R.string.rhythm_start_program
                            isPlaying -> R.string.rhythm_stop
                            else -> R.string.rhythm_start
                        }
                    ),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Shown instead of the controls until the add-on is bought. Deliberately
 * placed where the controls would be, rather than as a dialog on top of them:
 * the user should see what the feature is before being asked to pay.
 */
@Composable
private fun RhythmLockedCard(
    price: String?,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onShowBundle: (() -> Unit)?
) {
    val colors = LocalBinauralColors.current

    Surface(
        color = colors.accentPrimary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.rhythm_locked_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.rhythm_locked_body),
                fontSize = 13.sp,
                color = colors.onSurfaceMuted,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onPurchase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.onAccent
                )
            ) {
                Text(
                    if (price != null) stringResource(R.string.rhythm_unlock_price, price)
                    else stringResource(R.string.rhythm_unlock),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.rhythm_restore),
                    fontSize = 12.sp,
                    color = colors.onSurfaceMuted
                )
            }
            onShowBundle?.let { BundleHint(it) }
        }
    }
}

/** One program step: tempo and length, each nudged by a pair of buttons. */
@Composable
private fun RhythmStepRow(
    step: RhythmStep,
    isCurrent: Boolean,
    canRemove: Boolean,
    onChange: (RhythmStep) -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalBinauralColors.current

    Surface(
        color = if (isCurrent) colors.accentPrimary.copy(alpha = 0.14f)
        else colors.overlay.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Stepper(
                label = stringResource(R.string.rhythm_step_bpm, step.bpm),
                onLess = { onChange(step.copy(bpm = step.bpm - 5)) },
                onMore = { onChange(step.copy(bpm = step.bpm + 5)) },
                modifier = Modifier.weight(1f)
            )
            Stepper(
                label = stringResource(R.string.rhythm_step_minutes, step.durationMinutes),
                onLess = { onChange(step.copy(durationMinutes = step.durationMinutes - 1)) },
                onMore = { onChange(step.copy(durationMinutes = step.durationMinutes + 1)) },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, enabled = canRemove) {
                Icon(
                    Icons.Default.Delete,
                    stringResource(R.string.rhythm_step_remove),
                    tint = if (canRemove) colors.onSurfaceMuted
                    else colors.onSurfaceMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun Stepper(
    label: String,
    onLess: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalBinauralColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onLess, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Remove,
                stringResource(R.string.rhythm_less),
                tint = colors.accentPrimary,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        IconButton(onClick = onMore, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Add,
                stringResource(R.string.rhythm_more),
                tint = colors.accentPrimary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun RhythmLabel(text: String) {
    val colors = LocalBinauralColors.current
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.onSurfaceMuted,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun <T> RhythmChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    val colors = LocalBinauralColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                onClick = { onSelect(value) },
                color = if (isSelected) colors.accentPrimary.copy(alpha = 0.15f)
                else colors.overlay.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) colors.accentPrimary else colors.onSurfaceMuted
                    )
                }
            }
        }
    }
}
