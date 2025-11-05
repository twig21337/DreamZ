package com.twig.dreamzversion3.dreamsigns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twig.dreamzversion3.data.dream.DreamRepositories
import com.twig.dreamzversion3.data.dream.DreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class DreamSignsViewModel(
    private val repository: DreamRepository = DreamRepositories.inMemory
) : ViewModel() {

    private val _promotedKeys = MutableStateFlow<Set<String>>(emptySet())
    private val promotedKeys: StateFlow<Set<String>> = _promotedKeys.asStateFlow()

    val uiState: StateFlow<DreamSignsUiState> = combine(
        repository.dreams,
        promotedKeys
    ) { dreams, promoted ->
        val candidates = buildDreamSignCandidates(dreams)
        val promotedCandidates = promoted.map { key ->
            candidates.firstOrNull { it.key == key } ?: DreamSignCandidate(
                key = key,
                displayText = key.toDisplayName(),
                count = 0,
                sources = emptySet()
            )
        }
        val remainingCandidates = candidates.filterNot { candidate -> candidate.key in promoted }
        DreamSignsUiState(
            hasDreams = dreams.isNotEmpty(),
            promotedSigns = promotedCandidates,
            candidateSigns = remainingCandidates
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
    val sources: Set<DreamSignSource>
)

data class DreamSignsUiState(
    val hasDreams: Boolean = false,
    val promotedSigns: List<DreamSignCandidate> = emptyList(),
    val candidateSigns: List<DreamSignCandidate> = emptyList()
) {
    val hasCandidates: Boolean = candidateSigns.isNotEmpty()
}
