package com.twig.dreamzversion3.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.twig.dreamzversion3.R
import com.twig.dreamzversion3.auth.buildGoogleSignInClient
import com.twig.dreamzversion3.auth.getLastAccount
import com.twig.dreamzversion3.data.UserPreferencesRepository
import com.twig.dreamzversion3.drive.DriveDocumentPreview
import com.twig.dreamzversion3.drive.DriveSyncManager
import com.twig.dreamzversion3.drive.DriveSyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val preferences: UserPreferencesRepository,
    private val driveSyncManager: DriveSyncManager,
    private val appContext: Context
) : ViewModel() {

    private val driveState = MutableStateFlow(
        DriveState(drivePreviews = driveSyncManager.buildPreview())
    )

    val uiState: StateFlow<AccountUiState> = driveState
        .map { state ->
            AccountUiState(
                accountName = state.accountName,
                accountEmail = state.accountEmail,
                isDriveLinked = state.isDriveLinked,
                isSyncing = state.isSyncing,
                statusMessage = state.statusMessage,
                drivePreviews = state.drivePreviews
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    init {
        val account = getLastAccount(appContext)
        driveState.update {
            it.copy(
                isDriveLinked = account != null,
                accountName = account?.displayName,
                accountEmail = account?.email
            )
        }
    }

    fun onDriveLinked(displayName: String?, email: String?) {
        driveState.update {
            it.copy(
                isDriveLinked = true,
                accountName = displayName,
                accountEmail = email,
                statusMessage = appContext.getString(R.string.account_drive_connected_message)
            )
        }
    }

    fun onDriveLinkCancelled() {
        driveState.update {
            it.copy(statusMessage = appContext.getString(R.string.account_drive_sign_in_canceled))
        }
    }

    fun onDriveLinkFailed(reason: String?) {
        val message = if (!reason.isNullOrBlank()) {
            appContext.getString(R.string.account_drive_connect_error, reason)
        } else {
            appContext.getString(R.string.account_drive_connect_error_generic)
        }
        driveState.update { it.copy(statusMessage = message) }
    }

    fun beginAuthorization() {
        driveState.update {
            it.copy(
                isSyncing = true,
                statusMessage = appContext.getString(R.string.account_drive_authorizing)
            )
        }
    }

    fun syncWithToken(token: String) {
        viewModelScope.launch {
            driveState.update {
                it.copy(
                    isSyncing = true,
                    statusMessage = appContext.getString(R.string.account_drive_syncing)
                )
            }
            try {
                val result = driveSyncManager.sync(token)
                preferences.persistDriveToken(token)
                val message = when (result) {
                    is DriveSyncResult.Success ->
                        appContext.getString(R.string.account_drive_sync_success, result.count)

                    DriveSyncResult.Empty ->
                        appContext.getString(R.string.account_drive_sync_empty)
                }
                driveState.update {
                    it.copy(
                        isDriveLinked = true,
                        isSyncing = false,
                        statusMessage = message,
                        drivePreviews = driveSyncManager.buildPreview()
                    )
                }
            } catch (t: Throwable) {
                val message = t.message?.takeIf { it.isNotBlank() }
                    ?: appContext.getString(R.string.account_drive_sync_failed_generic)
                driveState.update {
                    it.copy(isSyncing = false, statusMessage = message)
                }
            }
        }
    }

    fun onSyncError(message: String) {
        driveState.update {
            it.copy(
                isSyncing = false,
                statusMessage = message.ifBlank {
                    appContext.getString(R.string.account_drive_sync_failed_generic)
                }
            )
        }
    }

    fun disconnectDrive() {
        buildGoogleSignInClient(appContext).signOut()
        viewModelScope.launch { preferences.clearDriveToken() }
        driveState.update {
            it.copy(
                isDriveLinked = false,
                accountName = null,
                accountEmail = null,
                isSyncing = false,
                statusMessage = appContext.getString(R.string.account_drive_disconnect_message)
            )
        }
    }

    data class DriveState(
        val isDriveLinked: Boolean = false,
        val accountName: String? = null,
        val accountEmail: String? = null,
        val isSyncing: Boolean = false,
        val statusMessage: String? = null,
        val drivePreviews: List<DriveDocumentPreview> = emptyList()
    )

    companion object {
        fun factory(
            preferences: UserPreferencesRepository,
            driveSyncManager: DriveSyncManager,
            context: Context
        ): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
                        AccountViewModel(preferences, driveSyncManager, appContext) as T
                    } else {
                        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
                    }
                }
            }
        }
    }
}

data class AccountUiState(
    val accountName: String? = null,
    val accountEmail: String? = null,
    val isDriveLinked: Boolean = false,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
    val drivePreviews: List<DriveDocumentPreview> = emptyList()
)
