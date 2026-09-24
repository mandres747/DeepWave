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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.R
import de.binauralbeats.app.data.AmbientSound
import de.binauralbeats.app.ui.theme.LocalBinauralColors
import java.util.Locale

@Composable
fun MixerSheet(
    volumes: Map<AmbientSound, Float>,
    isPlaying: Boolean,
    sleepTimerMinutes: Int,
    sleepTimerRemainingSec: Int,
    onVolumeChange: (AmbientSound, Float) -> Unit,
    onTogglePlay: () -> Unit,
    onSleepTimerSelect: (Int) -> Unit,
    onClose: () -> Unit,
    /** Shown under the timer while one is set; the wake-alarm line lives here. */
    belowSleepTimer: @Composable () -> Unit = {}
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
                    stringResource(R.string.mixer_header),
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
                stringResource(R.string.mixer_hint),
                fontSize = 12.sp,
                color = colors.onSurfaceMuted,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(12.dp))

            AmbientSound.entries.forEach { sound ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(sound.emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(sound.labelRes),
                        fontSize = 13.sp,
                        color = colors.onSurface,
                        modifier = Modifier.width(110.dp)
                    )
                    Slider(
                        value = volumes[sound] ?: 0f,
                        onValueChange = { onVolumeChange(sound, it) },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accentPrimary,
                            activeTrackColor = colors.accentPrimary,
                            inactiveTrackColor = colors.overlay.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.sleep_timer),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceMuted
                )
                if (sleepTimerRemainingSec > 0) {
                    Text(
                        formatRemaining(sleepTimerRemainingSec),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accentPrimary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            TimerChipRow(
                options = listOf(0, 15, 30),
                selected = sleepTimerMinutes,
                onSelect = onSleepTimerSelect
            )
            Spacer(Modifier.height(8.dp))
            TimerChipRow(
                options = listOf(45, 60, 90),
                selected = sleepTimerMinutes,
                onSelect = onSleepTimerSelect
            )
            if (sleepTimerMinutes > 0) {
                Spacer(Modifier.height(12.dp))
                belowSleepTimer()
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onTogglePlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentPrimary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    null,
                    tint = colors.onAccent
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(if (isPlaying) R.string.ambient_stop else R.string.ambient_play),
                    color = colors.onAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TimerChipRow(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val colors = LocalBinauralColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { minutes ->
            val isSelected = minutes == selected
            Surface(
                onClick = { onSelect(minutes) },
                color = if (isSelected) colors.accentPrimary.copy(alpha = 0.15f)
                else colors.overlay.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (minutes == 0) stringResource(R.string.sleep_timer_off)
                        else stringResource(R.string.sleep_timer_min, minutes),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) colors.accentPrimary else colors.onSurfaceMuted
                    )
                }
            }
        }
    }
}

internal fun formatRemaining(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
