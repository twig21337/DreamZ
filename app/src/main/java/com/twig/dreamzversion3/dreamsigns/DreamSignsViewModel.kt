package com.twig.dreamzversion3.dreamsigns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.twig.dreamzversion3.data.dream.DreamRepositories
import com.twig.dreamzversion3.data.dream.DreamRepository
import com.twig.dreamzversion3.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class DreamSignsViewModel(
    private val repository: DreamRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _promotedKeys = MutableStateFlow<Set<String>>(emptySet())
    private val promotedKeys: StateFlow<Set<String>> = _promotedKeys.asStateFlow()
    private val blacklistState: StateFlow<Set<String>> = preferences.dreamSignBlacklistFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val uiState: StateFlow<DreamSignsUiState> = combine(
        repository.dreams,
        promotedKeys,
        blacklistState
    ) { dreams, promoted, blacklist ->
        val filteredPromoted = promoted.filterNot { it in blacklist }.toSet()
        if (filteredPromoted.size != promoted.size) {
            _promotedKeys.value = filteredPromoted
        }
        val candidates = buildDreamSignCandidates(dreams, blacklist = blacklist)
        val candidatesByKey = candidates.associateBy { it.key }
        val promotedCandidates = filteredPromoted.map { key ->
            candidatesByKey[key] ?: DreamSignCandidate(
                key = key,
                displayText = key.toDisplayName(),
                count = 0,
                sources = emptySet(),
                dreamTitles = emptyList()
            )
        }
        val remainingCandidates = candidates.filterNot { candidate -> candidate.key in filteredPromoted }
        val allCandidates = promotedCandidates + remainingCandidates
        val maxCount = allCandidates.maxOfOrNull { it.count } ?: 0
        DreamSignsUiState(
            hasDreams = dreams.isNotEmpty(),
            promotedSigns = promotedCandidates,
            candidateSigns = remainingCandidates,
            maxCount = maxCount,
            blacklistedSigns = blacklist.sorted()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DreamSignsUiState()
    )

    fun promoteSign(key: String) {
        _promotedKeys.update { current -> current + key }
    }

    fun removePromotedSign(key: String) {
        _promotedKeys.update { current -> current - key }
    }

    fun addToBlacklist(term: String) {
        viewModelScope.launch { preferences.addDreamSignBlacklistTerm(term) }
    }

    fun removeFromBlacklist(term: String) {
        viewModelScope.launch { preferences.removeDreamSignBlacklistTerm(term) }
    }

    companion object {
        fun factory(
            repository: DreamRepository = DreamRepositories.inMemory,
            preferences: UserPreferencesRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DreamSignsViewModel(repository, preferences)
            }
        }
    }
}

private fun String.toDisplayName(): String =
    split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        .ifBlank { this }

enum class DreamSignSource {
    Description,
    Tag
}

data class DreamSignCandidate(
    val key: String,
    val displayText: String,
    val count: Int,
    val sources: Set<DreamSignSource>,
    val dreamTitles: List<String>
)

data class DreamSignsUiState(
    val hasDreams: Boolean = false,
    val promotedSigns: List<DreamSignCandidate> = emptyList(),
    val candidateSigns: List<DreamSignCandidate> = emptyList(),
    val maxCount: Int = 0,
    val blacklistedSigns: List<String> = emptyList()
) {
    val hasCandidates: Boolean = candidateSigns.isNotEmpty()
}
