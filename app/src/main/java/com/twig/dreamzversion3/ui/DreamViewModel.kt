package com.twig.dreamzversion3.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.twig.dreamzversion3.data.DreamDraft
import com.twig.dreamzversion3.data.DreamEntry
import com.twig.dreamzversion3.data.DreamRepo
import com.twig.dreamzversion3.data.UserPreferencesRepository
import com.twig.dreamzversion3.drive.DriveSyncWorker
import com.twig.dreamzversion3.signs.Dreamsign
import com.twig.dreamzversion3.signs.extractDreamsigns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DreamViewModel(
    private val repo: DreamRepo,
    private val preferences: UserPreferencesRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val draftFlow = MutableStateFlow(DreamDraft())
    private val syncState = MutableStateFlow(SyncState())

    private val entriesFlow = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val dreamsignsFlow = entriesFlow.map { entries ->
        val texts = entries.map { it.title + " " + it.body }
        extractDreamsigns(texts, topK = 20)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val darkThemeFlow = preferences.isDarkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<DreamUiState> = combine(
        entriesFlow,
        draftFlow,
        dreamsignsFlow,
        syncState,
        darkThemeFlow
    ) { entries, draft, signs, sync, isDark ->
        DreamUiState(
            entries = entries,
            draft = draft,
            dreamsigns = signs,
            syncState = sync,
            isDarkTheme = isDark
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DreamUiState())

    init {
        viewModelScope.launch {
            preferences.draftFlow.collect { draft ->
                if (draft != draftFlow.value) {
                    draftFlow.value = draft
                }
            }
        }
        viewModelScope.launch {
            draftFlow
                .debounce(600)
                .drop(1)
                .distinctUntilChanged()
                .collect { draft ->
                    if (draft.isBlank()) {
                        preferences.clearDraft()
                    } else {
                        preferences.persistDraft(draft)
                    }
                }
        }
        viewModelScope.launch { observeSyncWork() }
    }

    fun updateTitle(value: String) {
        draftFlow.value = draftFlow.value.copy(title = value)
    }

    fun updateBody(value: String) {
        draftFlow.value = draftFlow.value.copy(body = value)
    }

    fun updateMood(value: String) {
        draftFlow.value = draftFlow.value.copy(mood = value)
    }

    fun updateLucid(value: Boolean) {
        draftFlow.value = draftFlow.value.copy(lucid = value)
    }

    fun saveCurrentDraft() {
        val draft = draftFlow.value
        if (draft.isBlank()) return
        viewModelScope.launch {
            repo.save(draft.toEntry())
            clearDraft()
        }
    }

    fun clearDraft() {
        draftFlow.value = DreamDraft()
        viewModelScope.launch { preferences.clearDraft() }
    }

    fun delete(entry: DreamEntry) {
        viewModelScope.launch { repo.delete(entry) }
    }

    fun toggleDarkTheme() {
        val newValue = !darkThemeFlow.value
        viewModelScope.launch { preferences.setDarkTheme(newValue) }
    }

    fun enqueueSync(token: String) {
        val data = workDataOf(DriveSyncWorker.KEY_TOKEN to token)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DriveSyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            DriveSyncWorker.WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    private suspend fun observeSyncWork() {
        workManager.getWorkInfosForUniqueWorkLiveData(DriveSyncWorker.WORK_NAME)
            .asFlow()
            .collect { infos ->
                val info = infos.firstOrNull()
                syncState.value = info?.toSyncState() ?: SyncState()
            }
    }
}

data class DreamUiState(
    val entries: List<DreamEntry> = emptyList(),
    val draft: DreamDraft = DreamDraft(),
    val dreamsigns: List<Dreamsign> = emptyList(),
    val syncState: SyncState = SyncState(),
    val isDarkTheme: Boolean = false
)

data class SyncState(
    val message: String? = null,
    val errorMessage: String? = null,
    val inProgress: Boolean = false,
    val isQueued: Boolean = false
)

private fun WorkInfo.toSyncState(): SyncState {
    val status = progress.getString(DriveSyncWorker.KEY_STATUS)
    val outputStatus = outputData.getString(DriveSyncWorker.KEY_STATUS)
    val error = outputData.getString(DriveSyncWorker.KEY_ERROR)
    val isRunning = state == WorkInfo.State.RUNNING
    val isQueued = state == WorkInfo.State.ENQUEUED
    val message = when {
        isRunning -> status ?: "Syncing…"
        isQueued -> status ?: "Waiting for connection…"
        state == WorkInfo.State.SUCCEEDED -> outputStatus ?: status ?: "Sync complete"
        state == WorkInfo.State.FAILED -> error ?: "Sync failed"
        state == WorkInfo.State.CANCELLED -> "Sync cancelled"
        else -> status ?: outputStatus
    }
    val errorMessage = if (state == WorkInfo.State.FAILED) error else null
    return SyncState(
        message = message,
        errorMessage = errorMessage,
        inProgress = isRunning,
        isQueued = isQueued
    )
}
