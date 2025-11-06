package com.twig.dreamzversion3.account

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.twig.dreamzversion3.R
import com.twig.dreamzversion3.auth.buildGoogleSignInClient
import com.twig.dreamzversion3.auth.fetchAccessToken
import com.twig.dreamzversion3.common.findActivity
import com.twig.dreamzversion3.data.LocalUserPreferencesRepository
import com.twig.dreamzversion3.data.dream.DreamRepositories
import com.twig.dreamzversion3.drive.DriveSyncManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountRoute() {
    val context = LocalContext.current
    val preferences = LocalUserPreferencesRepository.current
    val driveSyncManager = remember { DriveSyncManager(DreamRepositories.inMemory) }
    val viewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.factory(preferences, driveSyncManager, context)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val googleSignInClient = remember(context) { buildGoogleSignInClient(context) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            viewModel.onDriveLinkCancelled()
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.onDriveLinked(account?.displayName, account?.email)
        } catch (apiException: ApiException) {
            if (apiException.statusCode == CommonStatusCodes.CANCELED) {
                viewModel.onDriveLinkCancelled()
            } else {
                viewModel.onDriveLinkFailed(apiException.localizedMessage)
            }
        }
    }

    AccountScreen(
        uiState = uiState,
        onConnectDrive = { signInLauncher.launch(googleSignInClient.signInIntent) },
        onDisconnectDrive = viewModel::disconnectDrive,
        onSyncNow = {
            val activity = context.findActivity()
            if (activity == null) {
                viewModel.onSyncError(context.getString(R.string.account_drive_authorization_failed))
                return@AccountScreen
            }
            coroutineScope.launch {
                viewModel.beginAuthorization()
                try {
                    val token = fetchAccessToken(activity)
                    viewModel.syncWithToken(token)
                } catch (recoverable: UserRecoverableAuthException) {
                    viewModel.onSyncError(context.getString(R.string.account_drive_authorization_failed))
                } catch (t: Throwable) {
                    val message = t.localizedMessage
                        ?: context.getString(R.string.account_drive_sync_failed_generic)
                    viewModel.onSyncError(message)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    uiState: AccountUiState,
    onConnectDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(text = stringResource(id = R.string.account_title)) }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "drive_header") {
                Text(
                    text = stringResource(id = R.string.account_drive_header),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = R.string.account_drive_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item(key = "drive_connection") {
                DriveConnectionSection(
                    uiState = uiState,
                    onConnectDrive = onConnectDrive,
                    onDisconnectDrive = onDisconnectDrive,
                    onSyncNow = onSyncNow
                )
            }
            if (uiState.statusMessage != null) {
                item(key = "drive_status") {
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.isSyncing) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            if (uiState.drivePreviews.isNotEmpty()) {
                item(key = "drive_preview_header") {
                    Text(
                        text = stringResource(id = R.string.account_drive_preview_header),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                items(uiState.drivePreviews, key = { it.fileName }) { preview ->
                    ListItem(
                        headlineContent = { Text(text = preview.fileName) },
                        supportingContent = {
                            Text(
                                text = preview.summary.ifBlank {
                                    stringResource(id = R.string.account_drive_preview_placeholder)
                                },
                                maxLines = 2,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun DriveConnectionSection(
    uiState: AccountUiState,
    onConnectDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onSyncNow: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val statusText = when {
            uiState.isDriveLinked && uiState.accountName != null && uiState.accountEmail != null ->
                stringResource(
                    id = R.string.account_drive_connected_as_name_email,
                    uiState.accountName,
                    uiState.accountEmail
                )

            uiState.isDriveLinked && uiState.accountEmail != null ->
                stringResource(id = R.string.account_drive_connected_as_email, uiState.accountEmail)

            uiState.isDriveLinked && uiState.accountName != null ->
                stringResource(id = R.string.account_drive_connected_as_email, uiState.accountName)

            uiState.isDriveLinked ->
                stringResource(id = R.string.account_drive_connected_generic)

            else -> stringResource(id = R.string.account_drive_not_connected)
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        DriveActionButtons(
            isLinked = uiState.isDriveLinked,
            isSyncing = uiState.isSyncing,
            onConnectDrive = onConnectDrive,
            onDisconnectDrive = onDisconnectDrive,
            onSyncNow = onSyncNow
        )
    }
}

@Composable
private fun DriveActionButtons(
    isLinked: Boolean,
    isSyncing: Boolean,
    onConnectDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onSyncNow: () -> Unit
) {
    val primaryLabel = if (isLinked) {
        stringResource(id = R.string.account_drive_sync_now)
    } else {
        stringResource(id = R.string.account_drive_connect)
    }
    val secondaryLabel = stringResource(id = R.string.account_drive_disconnect)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { if (isLinked) onSyncNow() else onConnectDrive() },
            enabled = !isSyncing
        ) {
            Text(text = primaryLabel)
        }
        if (isLinked) {
            OutlinedButton(onClick = onDisconnectDrive, enabled = !isSyncing) {
                Text(text = secondaryLabel)
            }
        }
    }
}
