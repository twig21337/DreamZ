package com.twig.dreamzversion3.ui.dreams

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.twig.dreamzversion3.R
import com.twig.dreamzversion3.data.DreamDraft
import com.twig.dreamzversion3.data.DreamLayoutMode
import com.twig.dreamzversion3.data.UserPreferencesRepository
import com.twig.dreamzversion3.data.dream.DreamRepository
import com.twig.dreamzversion3.model.dream.Dream
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DreamsViewModel(
    private val repository: DreamRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val latestDraft = MutableStateFlow(DreamDraft())
    private val highlightTerms = MutableStateFlow<Set<String>>(emptySet())
    private val _dreamEntryState = MutableStateFlow(DreamEntryUiState())
    private val dreamEntryState: StateFlow<DreamEntryUiState> = _dreamEntryState.asStateFlow()
    private val _listMode = MutableStateFlow<DreamListMode?>(null)
    private val listMode: StateFlow<DreamListMode?> = _listMode.asStateFlow()
    private val _sortOption = MutableStateFlow(DreamSortOption.DateNewest)
    private val sortOption: StateFlow<DreamSortOption> = _sortOption.asStateFlow()
    private val _events = MutableSharedFlow<DreamEditorEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    val uiState: StateFlow<DreamsUiState> = combine(
        repository.dreams,
        dreamEntryState,
        listMode,
        sortOption
    ) { dreams, entry, mode, sort ->
        DreamsUiState(
            dreams = dreams.sortedWith(sort.comparator),
            entry = entry,
            listMode = mode ?: DreamListMode.Card,
            sortOption = sort,
            isInitialized = mode != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DreamsUiState()
    )

    init {
        viewModelScope.launch {
            preferences.layoutModeFlow
                .catch {
                    if (_listMode.value == null) {
                        _listMode.value = DreamListMode.Card
                    }
                    emit(DreamLayoutMode.CARDS)
                }
                .collect { mode ->
                    val listMode = mode.toDreamListMode()
                    if (_listMode.value != listMode) {
                        _listMode.value = listMode
                    }
                }
        }
        viewModelScope.launch {
            combine(
                preferences.promotedDreamSignsFlow,
                preferences.dreamSignBlacklistFlow
            ) { promoted, blacklist ->
                promoted.filterNot { it in blacklist }.toSet()
            }.collect { terms ->
                highlightTerms.value = terms
                _dreamEntryState.update { state ->
                    state.copy(highlightedDreamSigns = terms)
                }
            }
        }
        viewModelScope.launch {
            preferences.draftFlow.collect { draft ->
                latestDraft.value = draft
                val current = _dreamEntryState.value
                if (!current.isEditing) {
                    _dreamEntryState.value = draft.toEntryState(
                        highlights = highlightTerms.value,
                        keepExistingInput = current.tagInput
                    )
                }
            }
        }
        viewModelScope.launch {
            _dreamEntryState
                .debounce(800)
                .map { it.isEditing to it.toDraft() }
                .distinctUntilChanged()
                .collect { (isEditing, draft) ->
                    if (isEditing) return@collect
                    if (draft.isBlank()) {
                        preferences.clearDraft()
                    } else {
                        preferences.persistDraft(draft)
                    }
                }
        }
    }

    fun onTitleChange(title: String) {
        _dreamEntryState.update { it.copy(title = title) }
    }

    fun onDescriptionChange(description: String) {
        _dreamEntryState.update { it.copy(description = description) }
    }

    fun onMoodChange(mood: String) {
        _dreamEntryState.update { it.copy(mood = mood) }
    }

    fun onTagInputChanged(input: String) {
        val sanitized = input.replace('\n', ' ')
        val parts = sanitized.split(',', ';')
        val committed = if (parts.size > 1) parts.dropLast(1) else emptyList()
        val remainder = parts.lastOrNull() ?: sanitized
        _dreamEntryState.update { state ->
            var updated = state
            committed.map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { tag -> updated = updated.addTag(tag) }
            updated.copy(tagInput = remainder.trimStart())
        }
    }

    fun commitTagInput() {
        _dreamEntryState.update { state ->
            val input = state.tagInput.trim()
            if (input.isEmpty()) return@update state.copy(tagInput = "")
            state.addTag(input).copy(tagInput = "")
        }
    }

    fun removeTag(tag: String) {
        _dreamEntryState.update { state ->
            state.copy(tags = state.tags.filterNot { it.equals(tag, ignoreCase = true) })
        }
    }

    fun onLucidChange(isLucid: Boolean) {
        _dreamEntryState.update { it.copy(isLucid = isLucid) }
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

    fun saveDream(): Boolean {
        commitTagInput()
        val entry = dreamEntryState.value
        if (entry.title.isBlank() && entry.description.isBlank()) {
            return false
        }

        val now = System.currentTimeMillis()
        val createdAt = entry.createdAt ?: now
        val dream = Dream(
            id = entry.dreamId ?: UUID.randomUUID().toString(),
            title = entry.title.ifBlank { "Untitled Dream" },
            description = entry.description,
            mood = entry.mood,
            isLucid = entry.isLucid,
            intensity = entry.intensity,
            emotion = entry.emotion,
            isRecurring = entry.isRecurring,
            tags = entry.tags,
            createdAt = createdAt,
            updatedAt = if (entry.isEditing) now else entry.updatedAt
        )

        if (entry.isEditing) {
            repository.updateDream(dream)
        } else {
            repository.addDream(dream)
        }
        viewModelScope.launch {
            _events.emit(DreamEditorEvent.DreamSaved(dream.title))
        }
        resetEntry()
        viewModelScope.launch { preferences.clearDraft() }
        return true
    }

    fun deleteCurrentDream(): Boolean {
        val dreamId = dreamEntryState.value.dreamId ?: return false
        repository.deleteDream(dreamId)
        viewModelScope.launch {
            _events.emit(DreamEditorEvent.DreamDeleted)
        }
        resetEntry()
        viewModelScope.launch { preferences.clearDraft() }
        return true
    }

    fun resetEntry() {
        _dreamEntryState.value = DreamEntryUiState(highlightedDreamSigns = highlightTerms.value)
    }

    fun startNewEntry() {
        _dreamEntryState.value = latestDraft.value.toEntryState(highlights = highlightTerms.value)
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
            isLucid = dream.isLucid,
            intensity = dream.intensity,
            emotion = dream.emotion,
            isRecurring = dream.isRecurring,
            tags = dream.tags,
            createdAt = dream.createdAt,
            updatedAt = dream.updatedAt,
            highlightedDreamSigns = highlightTerms.value
        )
    }

    fun cancelEditing() {
        if (!dreamEntryState.value.isEditing) {
            viewModelScope.launch { preferences.clearDraft() }
        }
        resetEntry()
    }

    fun toggleListMode() {
        val current = _listMode.value ?: DreamListMode.Card
        val newMode = when (current) {
            DreamListMode.List -> DreamListMode.Card
            DreamListMode.Card -> DreamListMode.List
        }
        _listMode.value = newMode
        viewModelScope.launch {
            preferences.setLayoutMode(newMode.toLayoutMode())
        }
    }

    fun selectSortOption(option: DreamSortOption) {
        _sortOption.value = option
    }

    companion object {
        fun factory(
            repository: DreamRepository,
            preferences: UserPreferencesRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { DreamsViewModel(repository, preferences) }
        }
    }
}

private fun DreamLayoutMode.toDreamListMode(): DreamListMode = when (this) {
    DreamLayoutMode.LIST -> DreamListMode.List
    DreamLayoutMode.CARDS -> DreamListMode.Card
}

private fun DreamListMode.toLayoutMode(): DreamLayoutMode = when (this) {
    DreamListMode.List -> DreamLayoutMode.LIST
    DreamListMode.Card -> DreamLayoutMode.CARDS
}

data class DreamsUiState(
    val dreams: List<Dream> = emptyList(),
    val entry: DreamEntryUiState = DreamEntryUiState(),
    val listMode: DreamListMode = DreamListMode.Card,
    val sortOption: DreamSortOption = DreamSortOption.DateNewest,
    val isInitialized: Boolean = false
)

data class DreamEntryUiState(
    val dreamId: String? = null,
    val title: String = "",
    val description: String = "",
    val mood: String = "",
    val isLucid: Boolean = false,
    val intensity: Float = 5f,
    val emotion: Float = 5f,
    val isRecurring: Boolean = false,
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val highlightedDreamSigns: Set<String> = emptySet()
) {
    val isEditing: Boolean = dreamId != null
}

private fun DreamEntryUiState.addTag(tag: String): DreamEntryUiState {
    val normalized = tag.trim()
    if (normalized.isEmpty()) return this
    if (tags.any { it.equals(normalized, ignoreCase = true) }) return this
    return copy(tags = tags + normalized)
}

private fun DreamEntryUiState.toDraft(): DreamDraft = DreamDraft(
    title = title,
    body = description,
    mood = mood,
    lucid = isLucid,
    tags = tags,
    intensityRating = intensity.roundToInt().coerceIn(0, 10),
    emotionRating = emotion.roundToInt().coerceIn(0, 10),
    lucidityRating = if (isLucid) 10 else 0,
    recurring = isRecurring
)

private fun DreamDraft.toEntryState(
    highlights: Set<String>,
    keepExistingInput: String = ""
): DreamEntryUiState {
    val isDefaultDraft = title.isBlank() && body.isBlank() && mood.isBlank() && tags.isEmpty() && !lucid && intensityRating == 0 && emotionRating == 0 && !recurring
    if (isDefaultDraft) {
        return DreamEntryUiState(highlightedDreamSigns = highlights, tagInput = keepExistingInput)
    }
    return DreamEntryUiState(
        title = title,
        description = body,
        mood = mood,
        isLucid = lucid,
        intensity = intensityRating.coerceIn(0, 10).toFloat(),
        emotion = emotionRating.coerceIn(0, 10).toFloat(),
        isRecurring = recurring,
        tags = tags,
        tagInput = keepExistingInput,
        highlightedDreamSigns = highlights
    )
}

enum class DreamListMode {
    List,
    Card
}

enum class DreamSortOption(
    @StringRes val labelRes: Int,
    val comparator: Comparator<Dream>
) {
    DateNewest(
        R.string.dream_sort_recent,
        compareByDescending<Dream> { it.createdAt }
    ),
    DateOldest(
        R.string.dream_sort_oldest,
        compareBy { it.createdAt }
    ),
    Mood(
        R.string.dream_sort_mood,
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.mood.ifBlank { "~" } }
    ),
    Tag(
        R.string.dream_sort_tag,
        compareBy(String.CASE_INSENSITIVE_ORDER) {
            it.tags.firstOrNull()?.ifBlank { "~" } ?: "~"
        }
    ),
    Intensity(
        R.string.dream_sort_intensity,
        compareByDescending<Dream> { it.intensity }
    ),
    Emotion(
        R.string.dream_sort_emotion,
        compareByDescending<Dream> { it.emotion }
    ),
    Lucid(
        R.string.dream_sort_lucid,
        compareBy<Dream> { if (it.isLucid) 0 else 1 }.thenByDescending { it.createdAt }
    ),
    Recurring(
        R.string.dream_sort_recurring,
        compareBy<Dream> { if (it.isRecurring) 0 else 1 }.thenByDescending { it.createdAt }
    )
}

sealed class DreamEditorEvent {
    data class DreamSaved(val title: String) : DreamEditorEvent()
    data object DreamDeleted : DreamEditorEvent()
}
