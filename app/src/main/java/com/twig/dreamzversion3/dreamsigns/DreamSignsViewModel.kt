package com.twig.dreamzversion3.dreamsigns

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DreamSignsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DreamSignsUiState())
    val uiState: StateFlow<DreamSignsUiState> = _uiState
}

data class DreamSignsUiState(
    val title: String = "Dream Signs",
    val description: String = "Track recurring themes and symbols in your dreams."
)
