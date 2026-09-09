package de.binauralbeats.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.binauralbeats.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("language_tag")
        private val KEY_AMBIENT_VOLUMES = stringPreferencesKey("ambient_volumes")
        private val KEY_ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        private val KEY_COMPLETED_SESSIONS = intPreferencesKey("completed_sessions")
        private val KEY_REVIEW_PROMPT_HANDLED = booleanPreferencesKey("review_prompt_handled")
        private val KEY_FIRST_SEEN_VERSION_CODE = intPreferencesKey("first_seen_version_code")
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

    // --- Onboarding ---

    /** False until the walkthrough was finished or skipped; drives the first-launch overlay. */
    suspend fun isOnboardingSeen(): Boolean =
        context.settingsDataStore.data.first()[KEY_ONBOARDING_SEEN] ?: false

    suspend fun setOnboardingSeen() {
        context.settingsDataStore.edit { it[KEY_ONBOARDING_SEEN] = true }
    }

    // --- Review prompt ---

    val completedSessions: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_COMPLETED_SESSIONS] ?: 0
    }

    /** Returns the new total so callers can decide about the prompt without a second read. */
    suspend fun incrementCompletedSessions(): Int {
        var total = 0
        context.settingsDataStore.edit { prefs ->
            total = (prefs[KEY_COMPLETED_SESSIONS] ?: 0) + 1
            prefs[KEY_COMPLETED_SESSIONS] = total
        }
        return total
    }

    /** True once the user rated or declined — the prompt is never shown again. */
    suspend fun isReviewPromptHandled(): Boolean =
        context.settingsDataStore.data.first()[KEY_REVIEW_PROMPT_HANDLED] ?: false

    suspend fun setReviewPromptHandled() {
        context.settingsDataStore.edit { it[KEY_REVIEW_PROMPT_HANDLED] = true }
    }

    // --- First-run marker ---

    /**
     * Version code this install was first seen on, or null before the marker
     * was ever written. See FirstRunMarker for why this exists.
     */
    suspend fun firstSeenVersionCode(): Int? =
        context.settingsDataStore.data.first()[KEY_FIRST_SEEN_VERSION_CODE]

    /**
     * Writes the marker once and never again. "Earlier data" is decided from
     * the store itself: anything already in there means an earlier version of
     * the app ran on this device before the marker existed.
     */
    suspend fun ensureFirstSeenVersionCode(currentVersionCode: Int) {
        context.settingsDataStore.edit { prefs ->
            val stored = prefs[KEY_FIRST_SEEN_VERSION_CODE]
            val hasEarlierData = prefs.asMap().keys.any { it != KEY_FIRST_SEEN_VERSION_CODE }
            FirstRunMarker.resolve(stored, hasEarlierData, currentVersionCode)?.let {
                prefs[KEY_FIRST_SEEN_VERSION_CODE] = it
            }
        }
    }
}
