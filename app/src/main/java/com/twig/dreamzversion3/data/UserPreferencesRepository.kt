package com.twig.dreamzversion3.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val KEY_DRAFT_TITLE = stringPreferencesKey("draft_title")
private val KEY_DRAFT_BODY = stringPreferencesKey("draft_body")
private val KEY_DRAFT_MOOD = stringPreferencesKey("draft_mood")
private val KEY_DRAFT_LUCID = booleanPreferencesKey("draft_lucid")
private val KEY_DRAFT_TAGS = stringPreferencesKey("draft_tags")
private val KEY_DRAFT_INTENSITY = intPreferencesKey("draft_intensity")
private val KEY_DRAFT_EMOTION = intPreferencesKey("draft_emotion")
private val KEY_DRAFT_LUCIDITY = intPreferencesKey("draft_lucidity")
private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
private val KEY_LAYOUT_MODE = stringPreferencesKey("layout_mode")
private val KEY_BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
private val KEY_DRIVE_TOKEN = stringPreferencesKey("drive_token")

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val draftFlow: Flow<DreamDraft> = dataStore.data.map { prefs ->
        DreamDraft(
            title = prefs[KEY_DRAFT_TITLE] ?: "",
            body = prefs[KEY_DRAFT_BODY] ?: "",
            mood = prefs[KEY_DRAFT_MOOD] ?: "",
            lucid = prefs[KEY_DRAFT_LUCID] ?: false,
            tags = prefs[KEY_DRAFT_TAGS]?.split("|")?.filter { it.isNotEmpty() } ?: emptyList(),
            intensityRating = prefs[KEY_DRAFT_INTENSITY] ?: 0,
            emotionRating = prefs[KEY_DRAFT_EMOTION] ?: 0,
            lucidityRating = prefs[KEY_DRAFT_LUCIDITY] ?: 0
        )
    }

    val isDarkThemeFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: false
    }

    val layoutModeFlow: Flow<DreamLayoutMode> = dataStore.data.map { prefs ->
        prefs[KEY_LAYOUT_MODE]?.let(DreamLayoutMode::valueOf) ?: DreamLayoutMode.CARDS
    }

    val backupFrequencyFlow: Flow<BackupFrequency> = dataStore.data.map { prefs ->
        prefs[KEY_BACKUP_FREQUENCY]?.let { BackupFrequency.valueOf(it) } ?: BackupFrequency.OFF
    }

    suspend fun persistDraft(draft: DreamDraft) {
        dataStore.edit { prefs ->
            prefs[KEY_DRAFT_TITLE] = draft.title
            prefs[KEY_DRAFT_BODY] = draft.body
            prefs[KEY_DRAFT_MOOD] = draft.mood
            prefs[KEY_DRAFT_LUCID] = draft.lucid
            prefs[KEY_DRAFT_TAGS] = draft.tags.joinToString("|")
            prefs[KEY_DRAFT_INTENSITY] = draft.intensityRating
            prefs[KEY_DRAFT_EMOTION] = draft.emotionRating
            prefs[KEY_DRAFT_LUCIDITY] = draft.lucidityRating
        }
    }

    suspend fun clearDraft() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_DRAFT_TITLE)
            prefs.remove(KEY_DRAFT_BODY)
            prefs.remove(KEY_DRAFT_MOOD)
            prefs.remove(KEY_DRAFT_LUCID)
            prefs.remove(KEY_DRAFT_TAGS)
            prefs.remove(KEY_DRAFT_INTENSITY)
            prefs.remove(KEY_DRAFT_EMOTION)
            prefs.remove(KEY_DRAFT_LUCIDITY)
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = enabled
        }
    }

    suspend fun setLayoutMode(mode: DreamLayoutMode) {
        dataStore.edit { prefs ->
            prefs[KEY_LAYOUT_MODE] = mode.name
        }
    }

    suspend fun setBackupFrequency(frequency: BackupFrequency) {
        dataStore.edit { prefs ->
            prefs[KEY_BACKUP_FREQUENCY] = frequency.name
        }
    }

    suspend fun persistDriveToken(token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_DRIVE_TOKEN] = token
        }
    }

    suspend fun clearDriveToken() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_DRIVE_TOKEN)
        }
    }

    suspend fun getDriveToken(): String? = dataStore.data.first()[KEY_DRIVE_TOKEN]
}

enum class DreamLayoutMode { LIST, CARDS }

enum class BackupFrequency(val intervalDays: Long) {
    OFF(0),
    WEEKLY(7),
    MONTHLY(30);
}
