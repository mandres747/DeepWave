package de.binauralbeats.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.binauralbeats.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("language_tag")
        private val KEY_AMBIENT_VOLUMES = stringPreferencesKey("ambient_volumes")
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val languageTag: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: ""
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setLanguage(tag: String) {
        context.settingsDataStore.edit { it[KEY_LANGUAGE] = tag }
    }

    // Serialized as "RAIN:0.5;OCEAN:0.2" — enum names as keys so the format
    // survives reordering of AmbientSound entries.
    val ambientVolumes: Flow<Map<String, Float>> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_AMBIENT_VOLUMES]?.split(";")?.mapNotNull { entry ->
            val parts = entry.split(":")
            val value = parts.getOrNull(1)?.toFloatOrNull()
            if (parts.size == 2 && value != null) parts[0] to value else null
        }?.toMap() ?: emptyMap()
    }

    suspend fun setAmbientVolumes(volumes: Map<String, Float>) {
        val serialized = volumes.entries.joinToString(";") { "${it.key}:${it.value}" }
        context.settingsDataStore.edit { it[KEY_AMBIENT_VOLUMES] = serialized }
    }
}
