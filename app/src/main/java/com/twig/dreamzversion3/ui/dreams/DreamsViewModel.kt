package com.twig.dreamzversion3.ui.dreams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twig.dreamzversion3.data.dream.DreamRepositories
import com.twig.dreamzversion3.data.dream.DreamRepository
import com.twig.dreamzversion3.model.dream.Dream
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class DreamsViewModel(
    private val repository: DreamRepository = DreamRepositories.inMemory
) : ViewModel() {

    private val _dreamEntryState = MutableStateFlow(DreamEntryUiState())
    private val dreamEntryState: StateFlow<DreamEntryUiState> = _dreamEntryState.asStateFlow()
    private val _listMode = MutableStateFlow(DreamListMode.Card)
    private val listMode: StateFlow<DreamListMode> = _listMode.asStateFlow()

    val uiState: StateFlow<DreamsUiState> = combine(
        repository.dreams,
        dreamEntryState,
        listMode
    ) { dreams, entry, mode ->
        DreamsUiState(dreams = dreams, entry = entry, listMode = mode)
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

    fun onMoodChange(mood: String) {
        _dreamEntryState.update { it.copy(mood = mood) }
    }

    fun onTagsInputChange(tags: String) {
        _dreamEntryState.update { it.copy(tagsInput = tags) }
    }

    fun onLucidityChange(lucidity: Float) {
        _dreamEntryState.update { it.copy(lucidity = lucidity) }
    }

    fun onIntensityChange(intensity: Float) {
        _dreamEntryState.update { it.copy(intensity = intensity) }
    }

    fun onEmotionChange(emotion: Float) {
        _dreamEntryState.update { it.copy(emotion = emotion) }
    }

    fun onRecurringChange(isRecurring: Boolean) {
        _dreamEntryState.update { it.copy(isRecurring = isRecurring) }
    }

    fun saveDream(onSaved: () -> Unit) {
        val entry = dreamEntryState.value
        if (entry.title.isBlank() && entry.description.isBlank()) {
            return
        }

        val dream = Dream(
            id = entry.dreamId ?: UUID.randomUUID().toString(),
            title = entry.title.ifBlank { "Untitled Dream" },
            description = entry.description,
            mood = entry.mood,
            lucidity = entry.lucidity,
            intensity = entry.intensity,
            emotion = entry.emotion,
            isRecurring = entry.isRecurring,
            tags = entry.tags
        )

        if (entry.isEditing) {
            repository.updateDream(dream)
        } else {
            repository.addDream(dream)
        }
        resetEntry()
        onSaved()
    }

    fun resetEntry() {
        _dreamEntryState.value = DreamEntryUiState()
    }

    fun startEditing(dreamId: String) {
        if (dreamEntryState.value.dreamId == dreamId) {
            return
        }
        val dream = repository.getDream(dreamId) ?: return
        _dreamEntryState.value = DreamEntryUiState(
            dreamId = dream.id,
            title = dream.title,
            description = dream.description,
            mood = dream.mood,
            lucidity = dream.lucidity,
            intensity = dream.intensity,
            emotion = dream.emotion,
            isRecurring = dream.isRecurring,
            tagsInput = dream.tags.joinToString(", ")
        )
    }

    fun toggleListMode() {
        _listMode.update { current ->
            when (current) {
                DreamListMode.List -> DreamListMode.Card
                DreamListMode.Card -> DreamListMode.List
            }
        }
    }
}

data class DreamsUiState(
    val dreams: List<Dream> = emptyList(),
    val entry: DreamEntryUiState = DreamEntryUiState(),
    val listMode: DreamListMode = DreamListMode.Card
)

data class DreamEntryUiState(
    val dreamId: String? = null,
    val title: String = "",
    val description: String = "",
    val mood: String = "",
    val lucidity: Float = 5f,
    val intensity: Float = 5f,
    val emotion: Float = 5f,
    val isRecurring: Boolean = false,
    val tagsInput: String = ""
) {
    val isEditing: Boolean = dreamId != null
    val tags: List<String>
        get() = tagsInput.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}

enum class DreamListMode {
    List,
    Card
}
