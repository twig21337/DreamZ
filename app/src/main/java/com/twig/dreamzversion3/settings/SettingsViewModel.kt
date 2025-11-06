package com.twig.dreamzversion3.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.twig.dreamzversion3.data.ThemeMode
import com.twig.dreamzversion3.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val themeModeState = preferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val uiState: StateFlow<SettingsUiState> = themeModeState
        .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState()
    )

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    companion object {
        fun factory(
            preferences: UserPreferencesRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        SettingsViewModel(preferences) as T
                    } else {
                        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
                    }
                }
            }
        }
    }
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val availableThemeModes: List<ThemeMode> = ThemeMode.values().toList()
)
