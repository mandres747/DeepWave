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

private val Context.presetDataStore: DataStore<Preferences> by preferencesDataStore(name = "custom_presets")

class PresetRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_PRESETS = stringPreferencesKey("custom_presets_json")
    }

    val customPresets: Flow<List<CustomPreset>> = context.presetDataStore.data.map { prefs ->
        val raw = prefs[KEY_PRESETS] ?: "[]"
        try {
            json.decodeFromString<List<CustomPreset>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun save(preset: CustomPreset) {
        context.presetDataStore.edit { prefs ->
            val current = loadAll(prefs)
            val updated = current.filter { it.id != preset.id } + preset
            prefs[KEY_PRESETS] = json.encodeToString(updated)
        }
    }

    suspend fun delete(presetId: String) {
        context.presetDataStore.edit { prefs ->
            val current = loadAll(prefs)
            prefs[KEY_PRESETS] = json.encodeToString(current.filter { it.id != presetId })
        }
    }

    private fun loadAll(prefs: Preferences): List<CustomPreset> {
        val raw = prefs[KEY_PRESETS] ?: "[]"
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
