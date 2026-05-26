package de.binauralbeats.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.R
import de.binauralbeats.app.data.JournalEntry
import de.binauralbeats.app.ui.theme.LocalBinauralColors
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class SessionStats(
    val totalSessions: Int,
    val totalMinutes: Int,
    val avgRating: Float,
    val currentStreak: Int,
    val bestStreak: Int,
    val thisWeekSessions: Int,
    val topPresets: List<Pair<String, Int>>
)

fun computeStats(entries: List<JournalEntry>): SessionStats {
    if (entries.isEmpty()) return SessionStats(0, 0, 0f, 0, 0, 0, emptyList())

    val totalMinutes = entries.sumOf { it.totalDurationMinutes }
    val avgRating = entries.map { it.rating }.average().toFloat()

    val days = entries.map { entry ->
        val cal = Calendar.getInstance().apply { timeInMillis = entry.completedAt }
        cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }.distinct().sorted()

    var currentStreak = 1
    var bestStreak = 1
    var streak = 1
    val today = Calendar.getInstance().let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }

    for (i in days.size - 1 downTo 1) {
        if (days[i] - days[i - 1] == 1) {
            streak++
            bestStreak = maxOf(bestStreak, streak)
        } else {
            streak = 1
        }
    }
    if (days.isNotEmpty()) {
        bestStreak = maxOf(bestStreak, streak)
        streak = 1
        if (days.last() == today || days.last() == today - 1) {
            for (i in days.size - 1 downTo 1) {
                if (days[i] - days[i - 1] == 1) streak++ else break
            }
            currentStreak = streak
        } else {
            currentStreak = 0
        }
    }

    val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
    val thisWeek = entries.count { it.completedAt >= weekAgo }

    val topPresets = entries.groupBy { it.presetName }
        .mapValues { it.value.size }
        .entries.sortedByDescending { it.value }
        .take(5)
        .map { it.key to it.value }

    return SessionStats(entries.size, totalMinutes, avgRating, currentStreak, bestStreak, thisWeek, topPresets)
}

@Composable
fun StatisticsOverlay(
    entries: List<JournalEntry>,
    onClose: () -> Unit
) {
    val colors = LocalBinauralColors.current
    val stats = remember(entries) { computeStats(entries) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceDark.copy(alpha = 0.95f))
            .clickable(enabled = false) {}
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.stats_header),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accentPrimary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        stringResource(R.string.close),
                        fontSize = 14.sp,
                        color = colors.accentPrimary,
                        modifier = Modifier.clickable { onClose() }
                    )
                }
            }

            if (stats.totalSessions == 0) {
                item {
                    Text(
                        stringResource(R.string.stats_no_data),
                        fontSize = 14.sp,
                        color = colors.onSurfaceMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = stringResource(R.string.stats_total_sessions),
                            value = "${stats.totalSessions}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.stats_total_time),
                            value = stringResource(R.string.stats_hours_short, stats.totalMinutes / 60, stats.totalMinutes % 60),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = stringResource(R.string.stats_avg_rating),
                            value = "%.1f ★".format(stats.avgRating),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.stats_this_week),
                            value = "${stats.thisWeekSessions}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = stringResource(R.string.stats_current_streak),
                            value = "${stats.currentStreak} ${stringResource(R.string.stats_days)}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.stats_best_streak),
                            value = "${stats.bestStreak} ${stringResource(R.string.stats_days)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (stats.topPresets.isNotEmpty()) {
                    item {
                        Surface(
                            color = colors.overlay.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.stats_top_presets),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accentPrimary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                stats.topPresets.forEachIndexed { index, (name, count) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${index + 1}. $name",
                                            fontSize = 14.sp,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            "${count}x",
                                            fontSize = 14.sp,
                                            color = colors.onSurfaceMuted,
                                            fontWeight = FontWeight.SemiBold
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
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalBinauralColors.current
    Surface(
        color = colors.overlay.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accentPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = colors.onSurfaceMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
