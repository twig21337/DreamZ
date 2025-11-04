package com.twig.dreamzversion3.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val KEY_DRAFT_TITLE = stringPreferencesKey("draft_title")
private val KEY_DRAFT_BODY = stringPreferencesKey("draft_body")
private val KEY_DRAFT_MOOD = stringPreferencesKey("draft_mood")
private val KEY_DRAFT_LUCID = booleanPreferencesKey("draft_lucid")
private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val draftFlow: Flow<DreamDraft> = dataStore.data.map { prefs ->
        DreamDraft(
            title = prefs[KEY_DRAFT_TITLE] ?: "",
            body = prefs[KEY_DRAFT_BODY] ?: "",
            mood = prefs[KEY_DRAFT_MOOD] ?: "",
            lucid = prefs[KEY_DRAFT_LUCID] ?: false
        )
    }

    val isDarkThemeFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: false
    }

    suspend fun persistDraft(draft: DreamDraft) {
        dataStore.edit { prefs ->
            prefs[KEY_DRAFT_TITLE] = draft.title
            prefs[KEY_DRAFT_BODY] = draft.body
            prefs[KEY_DRAFT_MOOD] = draft.mood
            prefs[KEY_DRAFT_LUCID] = draft.lucid
        }
    }

    suspend fun clearDraft() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_DRAFT_TITLE)
            prefs.remove(KEY_DRAFT_BODY)
            prefs.remove(KEY_DRAFT_MOOD)
            prefs.remove(KEY_DRAFT_LUCID)
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = enabled
        }
    }
}
