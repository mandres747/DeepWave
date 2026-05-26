package de.binauralbeats.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.journalDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_journal")

class JournalRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_ENTRIES = stringPreferencesKey("journal_entries_json")
    }

    val entries: Flow<List<JournalEntry>> = context.journalDataStore.data.map { prefs ->
        val raw = prefs[KEY_ENTRIES] ?: "[]"
        try {
            json.decodeFromString<List<JournalEntry>>(raw).sortedByDescending { it.completedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun save(entry: JournalEntry) {
        context.journalDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val updated = current.filter { it.id != entry.id } + entry
            prefs[KEY_ENTRIES] = json.encodeToString(updated)
        }
    }

    suspend fun delete(entryId: String) {
        context.journalDataStore.edit { prefs ->
            val current = loadAll(prefs)
            prefs[KEY_ENTRIES] = json.encodeToString(current.filter { it.id != entryId })
        }
    }

    private fun loadAll(prefs: Preferences): List<JournalEntry> {
        val raw = prefs[KEY_ENTRIES] ?: "[]"
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
