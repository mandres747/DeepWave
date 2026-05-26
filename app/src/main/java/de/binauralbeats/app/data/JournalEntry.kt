package de.binauralbeats.app.data

import androidx.annotation.StringRes
import de.binauralbeats.app.R
import kotlinx.serialization.Serializable

@Serializable
data class JournalEntry(
    val id: String,
    val presetName: String,
    val presetKey: String? = null,
    val totalDurationMinutes: Int,
    val completedAt: Long,
    val rating: Int,
    val moods: List<String>,
    val notes: String = ""
)

data class Mood(
    val key: String,
    val emoji: String,
    @StringRes val labelRes: Int
)

object Moods {
    val all = listOf(
        Mood("relaxed", "😌", R.string.mood_relaxed),
        Mood("focused", "🎯", R.string.mood_focused),
        Mood("sleepy", "😴", R.string.mood_sleepy),
        Mood("energetic", "⚡", R.string.mood_energetic),
        Mood("creative", "🎨", R.string.mood_creative),
        Mood("calm", "🧘", R.string.mood_calm),
        Mood("neutral", "😐", R.string.mood_neutral)
    )

    fun findByKey(key: String): Mood? = all.firstOrNull { it.key == key }
}
