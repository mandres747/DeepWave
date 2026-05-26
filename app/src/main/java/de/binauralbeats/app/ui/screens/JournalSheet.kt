package de.binauralbeats.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.R
import de.binauralbeats.app.data.JournalEntry
import de.binauralbeats.app.data.Moods
import de.binauralbeats.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingDialog(
    presetName: String,
    onSave: (rating: Int, moods: List<String>, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var selectedMoods by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Column {
                Text(stringResource(R.string.rate_session), color = Color.White, fontSize = 18.sp)
                Text(presetName, color = OnSurfaceMuted, fontSize = 12.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Star rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = stringResource(R.string.stars_desc, star),
                                tint = if (star <= rating) Color(0xFFFFD700) else Color.White.copy(0.3f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Mood tags
                Text(stringResource(R.string.mood_label), fontSize = 12.sp, color = OnSurfaceMuted)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Moods.all.forEach { mood ->
                        val isSelected = mood.key in selectedMoods
                        val chipColor by animateColorAsState(
                            if (isSelected) AccentPrimary.copy(0.2f) else Color.White.copy(0.06f),
                            label = "mood"
                        )
                        val borderColor = if (isSelected) AccentPrimary else Color.White.copy(0.15f)

                        Surface(
                            onClick = {
                                selectedMoods = if (isSelected) selectedMoods - mood.key
                                else selectedMoods + mood.key
                            },
                            color = chipColor,
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = Brush.linearGradient(listOf(borderColor, borderColor))
                            )
                        ) {
                            Text(
                                "${mood.emoji} ${stringResource(mood.labelRes)}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                color = if (isSelected) AccentPrimary else Color.White.copy(0.6f)
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label), color = OnSurfaceMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = Color.White.copy(0.15f),
                        focusedLabelColor = AccentPrimary,
                        cursorColor = AccentPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(rating, selectedMoods.toList(), notes) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                enabled = rating > 0
            ) {
                Text(stringResource(R.string.save), color = PrimaryDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.skip), color = OnSurfaceMuted)
            }
        }
    )
}

@Composable
fun JournalOverlay(
    entries: List<JournalEntry>,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yy · HH:mm", Locale.getDefault()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SurfaceDark.copy(alpha = 0.97f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.journal_header),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPrimary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        stringResource(R.string.journal_entries, entries.size),
                        fontSize = 11.sp,
                        color = OnSurfaceMuted
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, stringResource(R.string.close), tint = Color.White)
                }
            }

            HorizontalDivider(color = Color.White.copy(0.06f))

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.journal_empty),
                        fontSize = 14.sp,
                        color = OnSurfaceMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            } else {
                // Stats summary
                val avgRating = entries.map { it.rating }.average()
                val totalMinutes = entries.sumOf { it.totalDurationMinutes }
                val topMoods = entries.flatMap { it.moods }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .map { it.key }

                Surface(
                    color = Color.White.copy(0.04f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(stringResource(R.string.stat_sessions), "${entries.size}")
                        StatItem(stringResource(R.string.stat_total), stringResource(R.string.time_format, totalMinutes / 60, totalMinutes % 60))
                        StatItem(stringResource(R.string.stat_avg_rating), "${"%.1f".format(avgRating)}★")
                    }
                }

                if (topMoods.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(stringResource(R.string.stat_top), fontSize = 11.sp, color = OnSurfaceMuted)
                        topMoods.forEach { moodKey ->
                            val mood = Moods.findByKey(moodKey)
                            Text(
                                if (mood != null) "${mood.emoji} ${stringResource(mood.labelRes)}" else moodKey,
                                fontSize = 11.sp, color = AccentPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Entry list
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        JournalEntryCard(entry, dateFormat, onDelete)
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentPrimary)
        Text(label, fontSize = 10.sp, color = OnSurfaceMuted)
    }
}

@Composable
private fun JournalEntryCard(
    entry: JournalEntry,
    dateFormat: SimpleDateFormat,
    onDelete: (String) -> Unit
) {
    Surface(
        color = Color.White.copy(0.04f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.presetName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        "${dateFormat.format(Date(entry.completedAt))} · ${entry.totalDurationMinutes} min",
                        fontSize = 11.sp,
                        color = OnSurfaceMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Stars
                    Text(
                        "★".repeat(entry.rating) + "☆".repeat(5 - entry.rating),
                        fontSize = 12.sp,
                        color = Color(0xFFFFD700)
                    )

                    IconButton(
                        onClick = { onDelete(entry.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = Color.White.copy(0.3f))
                    }
                }
            }

            if (entry.moods.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    entry.moods.forEach { moodKey ->
                        val mood = Moods.findByKey(moodKey)
                        Surface(
                            color = AccentPrimary.copy(0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (mood != null) "${mood.emoji} ${stringResource(mood.labelRes)}" else moodKey,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = AccentPrimary
                            )
                        }
                    }
                }
            }

            if (entry.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.notes,
                    fontSize = 12.sp,
                    color = Color.White.copy(0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
