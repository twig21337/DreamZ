package com.twig.dreamzversion3.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState
}

data class SettingsUiState(
    val title: String = "Settings",
    val description: String = "Adjust themes, backups, and notification preferences."
)
