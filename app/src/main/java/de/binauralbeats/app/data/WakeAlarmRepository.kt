package de.binauralbeats.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Own store rather than a key in "settings": the alarm receivers read it from
// a cold process at boot, and the settings store is touched first thing by
// BinauralViewModel for the first-run marker - keeping them apart means a
// receiver can never be the one that "first" opens the settings store.
private val Context.wakeAlarmStore: DataStore<Preferences> by preferencesDataStore(name = "wake_alarms")

class WakeAlarmRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("alarms_json")

    val alarms: Flow<List<WakeAlarm>> = context.wakeAlarmStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<List<WakeAlarm>>(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun all(): List<WakeAlarm> = alarms.first()

    suspend fun get(id: String): WakeAlarm? = all().firstOrNull { it.id == id }

    suspend fun upsert(alarm: WakeAlarm) = update { list ->
        if (list.any { it.id == alarm.id }) list.map { if (it.id == alarm.id) alarm else it }
        else list + alarm
    }

    suspend fun delete(id: String) = update { list -> list.filterNot { it.id == id } }

    private suspend fun update(change: (List<WakeAlarm>) -> List<WakeAlarm>) {
        context.wakeAlarmStore.edit { prefs ->
            val current = prefs[key]
                ?.let { runCatching { json.decodeFromString<List<WakeAlarm>>(it) }.getOrNull() }
                ?: emptyList()
            prefs[key] = json.encodeToString(change(current))
        }
    }
}
