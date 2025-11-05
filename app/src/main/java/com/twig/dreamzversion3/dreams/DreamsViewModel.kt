package com.twig.dreamzversion3.dreams

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DreamsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DreamsUiState())
    val uiState: StateFlow<DreamsUiState> = _uiState
}

data class DreamsUiState(
    val title: String = "Dreams",
    val description: String = "Review and explore your dream journal entries."
)
