package com.twig.dreamzversion3.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.twig.dreamzversion3.data.ThemeMode
import com.twig.dreamzversion3.data.UserPreferencesRepository
import com.twig.dreamzversion3.drive.DriveDocumentPreview
import com.twig.dreamzversion3.drive.DriveSyncManager
import com.twig.dreamzversion3.drive.DriveSyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: UserPreferencesRepository,
    private val driveSyncManager: DriveSyncManager
) : ViewModel() {

    private val driveState = MutableStateFlow(DriveState())

    // themeModeFlow emits a ThemeMode
    private val themeModeState = preferences.themeModeFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ThemeMode.SYSTEM
        )

    // uiState emits a SettingsUiState
    val uiState: StateFlow<SettingsUiState> = combine(
        themeModeState,
        driveState
    ) { themeMode, drive ->
        SettingsUiState(
            themeMode = themeMode,
            availableThemeModes = ThemeMode.values().toList(),
            driveTokenInput = drive.tokenInput,
            isDriveLinked = !drive.storedToken.isNullOrBlank(),
            isSyncing = drive.isSyncing,
            syncMessage = drive.statusMessage,
            lastSyncedCount = drive.lastSyncedCount,
            drivePreviews = driveSyncManager.buildPreview()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState()
    )

    init {
        viewModelScope.launch {
            val storedToken = preferences.getDriveToken()
            driveState.update { current ->
                current.copy(
                    storedToken = storedToken,
                    tokenInput = storedToken.orEmpty()
                )
            }
        }
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun onDriveTokenChange(value: String) {
        driveState.update { it.copy(tokenInput = value) }
    }

    fun connectDrive() {
        val token = driveState.value.tokenInput.trim()
        if (token.isEmpty()) return
        viewModelScope.launch {
            preferences.persistDriveToken(token)
            driveState.update { current ->
                current.copy(
                    storedToken = token,
                    tokenInput = token,
                    statusMessage = "Connected to Google Drive"
                )
            }
        }
    }

    fun disconnectDrive() {
        viewModelScope.launch {
            preferences.clearDriveToken()
            driveState.update { current ->
                current.copy(
                    storedToken = null,
                    statusMessage = "Drive disconnected",
                    lastSyncedCount = null
                )
            }
        }
    }

    fun syncNow() {
        val token = driveState.value.storedToken?.takeIf { it.isNotBlank() }
            ?: driveState.value.tokenInput.trim()
        if (token.isEmpty()) {
            driveState.update { it.copy(statusMessage = "Add a Drive token before syncing") }
            return
        }
        viewModelScope.launch {
            driveState.update { it.copy(isSyncing = true, statusMessage = "Syncing…") }
            try {
                val result = driveSyncManager.sync(token)
                preferences.persistDriveToken(token)
                val message = when (result) {
                    is DriveSyncResult.Success ->
                        if (result.count > 0) {
                            "Synced ${result.count} dreams"
                        } else {
                            "All dreams are already synced"
                        }
                    DriveSyncResult.Empty -> "No dreams to sync"
                }
                driveState.update {
                    it.copy(
                        storedToken = token,
                        tokenInput = token,
                        isSyncing = false,
                        statusMessage = message,
                        lastSyncedCount = when (result) {
                            is DriveSyncResult.Success -> result.count
                            DriveSyncResult.Empty -> 0
                        }
                    )
                }
            } catch (t: Throwable) {
                driveState.update {
                    it.copy(
                        isSyncing = false,
                        statusMessage = t.message ?: "Sync failed"
                    )
                }
            }
        }
    }

    data class DriveState(
        val storedToken: String? = null,
        val tokenInput: String = "",
        val isSyncing: Boolean = false,
        val statusMessage: String? = null,
        val lastSyncedCount: Int? = null
    )

    companion object {
        fun factory(
            preferences: UserPreferencesRepository,
            driveSyncManager: DriveSyncManager
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        SettingsViewModel(preferences, driveSyncManager) as T
                    } else {
                        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
                    }
                }
            }
        }
    }
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val availableThemeModes: List<ThemeMode> = ThemeMode.values().toList(),
    val driveTokenInput: String = "",
    val isDriveLinked: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val lastSyncedCount: Int? = null,
    val drivePreviews: List<DriveDocumentPreview> = emptyList()
)
