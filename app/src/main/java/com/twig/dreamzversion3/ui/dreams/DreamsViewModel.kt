package com.twig.dreamzversion3.ui.dreams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twig.dreamzversion3.data.dream.DreamRepositories
import com.twig.dreamzversion3.data.dream.DreamRepository
import com.twig.dreamzversion3.model.dream.Dream
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class DreamsViewModel(
    private val repository: DreamRepository = DreamRepositories.inMemory
) : ViewModel() {

    private val _dreamEntryState = MutableStateFlow(DreamEntryUiState())
    private val dreamEntryState: StateFlow<DreamEntryUiState> = _dreamEntryState.asStateFlow()

    val uiState: StateFlow<DreamsUiState> = combine(
        repository.dreams,
        dreamEntryState
    ) { dreams, entry ->
        DreamsUiState(dreams = dreams, entry = entry)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DreamsUiState()
    )

    fun onTitleChange(title: String) {
        _dreamEntryState.update { it.copy(title = title) }
    }

    fun onDescriptionChange(description: String) {
        _dreamEntryState.update { it.copy(description = description) }
    }

    fun onLucidityChange(lucidity: Float) {
        _dreamEntryState.update { it.copy(lucidity = lucidity) }
    }

    fun onRecurringChange(isRecurring: Boolean) {
        _dreamEntryState.update { it.copy(isRecurring = isRecurring) }
    }

    fun saveDream(onSaved: () -> Unit) {
        val entry = dreamEntryState.value
        if (entry.title.isBlank() && entry.description.isBlank()) {
            return
        }

        repository.addDream(
            Dream(
                id = UUID.randomUUID().toString(),
                title = entry.title.ifBlank { "Untitled Dream" },
                description = entry.description,
                lucidity = entry.lucidity,
                isRecurring = entry.isRecurring
            )
        )
        resetEntry()
        onSaved()
    }

    fun resetEntry() {
        _dreamEntryState.value = DreamEntryUiState()
    }
}

data class DreamsUiState(
    val dreams: List<Dream> = emptyList(),
    val entry: DreamEntryUiState = DreamEntryUiState()
)

data class DreamEntryUiState(
    val title: String = "",
    val description: String = "",
    val lucidity: Float = 5f,
    val isRecurring: Boolean = false
)
