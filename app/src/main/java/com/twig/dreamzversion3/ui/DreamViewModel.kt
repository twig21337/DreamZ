package com.twig.dreamzversion3.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.twig.dreamzversion3.data.BackupFrequency
import com.twig.dreamzversion3.data.DreamDraft
import com.twig.dreamzversion3.data.DreamEntry
import com.twig.dreamzversion3.data.DreamFilter
import com.twig.dreamzversion3.data.DreamLayoutMode
import com.twig.dreamzversion3.data.DreamRepo
import com.twig.dreamzversion3.data.UserPreferencesRepository
import com.twig.dreamzversion3.drive.DriveSyncWorker
import com.twig.dreamzversion3.signs.Dreamsign
import com.twig.dreamzversion3.signs.extractDreamsigns
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DreamViewModel(
    private val repo: DreamRepo,
    private val preferences: UserPreferencesRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val curatedTags = listOf("Lucid", "Nightmare", "Recurring", "Symbolic", "Adventure", "Mystical", "Flying", "Falling")

    private val draftFlow = MutableStateFlow(DreamDraft())
    private val syncState = MutableStateFlow(SyncState())
    private val searchQuery = MutableStateFlow("")
    private val selectedTag = MutableStateFlow<String?>(null)
    private val selectedDate = MutableStateFlow<DateRange?>(null)

    private val darkThemeFlow = preferences.isDarkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val layoutModeState = preferences.layoutModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DreamLayoutMode.CARDS)

    private val backupFrequencyState = preferences.backupFrequencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupFrequency.OFF)

    private val filterFlow = combine(searchQuery, selectedTag, selectedDate) { query, tag, date ->
        DreamFilter(
            query = query,
            tag = tag,
            startMillis = date?.start,
            endMillis = date?.end
        )
    }

    private val entriesFlow = filterFlow
        .flatMapLatest { filter -> repo.observeFiltered(filter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allEntriesFlow = repo.observeFiltered(DreamFilter())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val dreamsignsFlow = allEntriesFlow.map { entries ->
        val texts = entries.map { it.title + " " + it.body }
        extractDreamsigns(texts, topK = 20)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val availableTagsFlow = allEntriesFlow.map { entries ->
        (curatedTags + entries.flatMap { it.tags })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), curatedTags)

    val uiState: StateFlow<DreamUiState> = combine(
        entriesFlow,
        draftFlow,
        dreamsignsFlow,
        syncState,
        darkThemeFlow,
        layoutModeState,
        backupFrequencyState,
        availableTagsFlow,
        searchQuery,
        selectedTag,
        selectedDate
    ) { entries, draft, signs, sync, isDark, layoutMode, backupFrequency, tags, query, tag, date ->
        DreamUiState(
            entries = entries,
            draft = draft,
            dreamsigns = signs,
            syncState = sync,
            isDarkTheme = isDark,
            layoutMode = layoutMode,
            backupFrequency = backupFrequency,
            availableTags = tags,
            searchQuery = query,
            activeTag = tag,
            selectedDateRange = date
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

    fun toggleDraftTag(tag: String) {
        val normalized = normalizeTag(tag)
        if (normalized.isEmpty()) return
        val current = draftFlow.value.tags.toMutableList()
        val existingIndex = current.indexOfFirst { it.equals(normalized, ignoreCase = true) }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(normalized)
        }
        draftFlow.value = draftFlow.value.copy(tags = current)
    }

    fun addCustomTag(tag: String) {
        val normalized = normalizeTag(tag)
        if (normalized.isEmpty()) return
        val current = draftFlow.value.tags
        if (current.any { it.equals(normalized, ignoreCase = true) }) return
        draftFlow.value = draftFlow.value.copy(tags = current + normalized)
    }

    fun updateIntensityRating(value: Float) {
        draftFlow.value = draftFlow.value.copy(intensityRating = value.toRating())
    }

    fun updateEmotionRating(value: Float) {
        draftFlow.value = draftFlow.value.copy(emotionRating = value.toRating())
    }

    fun updateLucidityRating(value: Float) {
        draftFlow.value = draftFlow.value.copy(lucidityRating = value.toRating())
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

    fun updateSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun selectTag(tag: String) {
        selectedTag.value = if (selectedTag.value == tag) null else tag
    }

    fun clearTagFilter() {
        selectedTag.value = null
    }

    fun setDateFilter(millis: Long?) {
        selectedDate.value = millis?.let { toDateRange(it) }
    }

    fun clearDateFilter() {
        selectedDate.value = null
    }

    fun clearFilters() {
        searchQuery.value = ""
        selectedTag.value = null
        selectedDate.value = null
    }

    fun setLayoutMode(mode: DreamLayoutMode) {
        viewModelScope.launch { preferences.setLayoutMode(mode) }
    }

    fun scheduleBackup(frequency: BackupFrequency) {
        viewModelScope.launch {
            preferences.setBackupFrequency(frequency)
            val token = preferences.getDriveToken()
            schedulePeriodicBackup(frequency, token, showDisabledMessage = true)
        }
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

        viewModelScope.launch {
            preferences.persistDriveToken(token)
            schedulePeriodicBackup(backupFrequencyState.value, token)
        }
    }

    private suspend fun observeSyncWork() {
        merge(
            workManager.getWorkInfosForUniqueWorkLiveData(DriveSyncWorker.WORK_NAME).asFlow(),
            workManager.getWorkInfosForUniqueWorkLiveData(DriveSyncWorker.PERIODIC_WORK_NAME).asFlow()
        ).collect { infos ->
            val info = infos.firstOrNull()
            syncState.value = info?.toSyncState() ?: SyncState()
        }
    }

    private fun schedulePeriodicBackup(
        frequency: BackupFrequency,
        token: String?,
        showDisabledMessage: Boolean = false
    ) {
        workManager.cancelUniqueWork(DriveSyncWorker.PERIODIC_WORK_NAME)
        if (frequency == BackupFrequency.OFF) {
            if (showDisabledMessage) {
                syncState.update { it.copy(message = "Automatic backups disabled", errorMessage = null) }
            }
            return
        }
        if (token.isNullOrBlank()) {
            syncState.update {
                it.copy(
                    message = "Connect Google Drive to enable automatic backups",
                    errorMessage = null
                )
            }
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DriveSyncWorker>(frequency.intervalDays, TimeUnit.DAYS)
            .setInputData(workDataOf(DriveSyncWorker.KEY_TOKEN to token))
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DriveSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        syncState.update {
            it.copy(
                message = "Automatic backups scheduled ${frequency.displayName()}",
                errorMessage = null
            )
        }
    }

    private fun toDateRange(millis: Long): DateRange {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        val end = start + TimeUnit.DAYS.toMillis(1) - 1
        return DateRange(start, end)
    }

    private fun normalizeTag(tag: String): String {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    private fun Float.toRating(): Int = this.coerceIn(0f, 10f).roundToInt()
}

data class DreamUiState(
    val entries: List<DreamEntry> = emptyList(),
    val draft: DreamDraft = DreamDraft(),
    val dreamsigns: List<Dreamsign> = emptyList(),
    val syncState: SyncState = SyncState(),
    val isDarkTheme: Boolean = false,
    val layoutMode: DreamLayoutMode = DreamLayoutMode.CARDS,
    val backupFrequency: BackupFrequency = BackupFrequency.OFF,
    val availableTags: List<String> = emptyList(),
    val searchQuery: String = "",
    val activeTag: String? = null,
    val selectedDateRange: DateRange? = null
)

data class SyncState(
    val message: String? = null,
    val errorMessage: String? = null,
    val inProgress: Boolean = false,
    val isQueued: Boolean = false
)

data class DateRange(val start: Long, val end: Long)

private fun BackupFrequency.displayName(): String = when (this) {
    BackupFrequency.WEEKLY -> "weekly"
    BackupFrequency.MONTHLY -> "monthly"
    BackupFrequency.OFF -> ""
}

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
