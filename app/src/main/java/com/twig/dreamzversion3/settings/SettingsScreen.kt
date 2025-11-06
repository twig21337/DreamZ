package com.twig.dreamzversion3.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twig.dreamzversion3.R
import com.twig.dreamzversion3.data.LocalUserPreferencesRepository
import com.twig.dreamzversion3.data.ThemeMode
import com.twig.dreamzversion3.data.dream.DreamRepositories
import com.twig.dreamzversion3.drive.DriveSyncManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute() {
    val preferences = LocalUserPreferencesRepository.current
    val driveSyncManager = remember { DriveSyncManager(DreamRepositories.inMemory) }
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(preferences, driveSyncManager)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onThemeSelected = viewModel::onThemeSelected,
        onDriveTokenChange = viewModel::onDriveTokenChange,
        onConnectDrive = viewModel::connectDrive,
        onDisconnectDrive = viewModel::disconnectDrive,
        onSyncNow = viewModel::syncNow
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeSelected: (ThemeMode) -> Unit,
    onDriveTokenChange: (String) -> Unit,
    onConnectDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.settings_title)) })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "theme_header") {
                Text(
                    text = stringResource(id = R.string.settings_theme_header),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = R.string.settings_theme_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(uiState.availableThemeModes, key = { it.name }) { mode ->
                ThemeOptionRow(
                    themeMode = mode,
                    selected = uiState.themeMode == mode,
                    onThemeSelected = onThemeSelected
                )
            }
            item(key = "drive_header") {
                Text(
                    text = stringResource(id = R.string.settings_drive_header),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = R.string.settings_drive_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item(key = "drive_token") {
                OutlinedTextField(
                    value = uiState.driveTokenInput,
                    onValueChange = onDriveTokenChange,
                    label = { Text(text = stringResource(id = R.string.settings_drive_token_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = stringResource(id = R.string.settings_drive_token_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item(key = "drive_actions") {
                DriveActionsRow(
                    isConnected = uiState.isDriveLinked,
                    isSyncing = uiState.isSyncing,
                    onConnect = onConnectDrive,
                    onDisconnect = onDisconnectDrive,
                    onSync = onSyncNow
                )
            }
            item(key = "drive_status") {
                if (uiState.syncMessage != null) {
                    Text(
                        text = uiState.syncMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (uiState.drivePreviews.isNotEmpty()) {
                item(key = "drive_preview_header") {
                    Text(
                        text = stringResource(id = R.string.settings_drive_preview_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.drivePreviews, key = { it.fileName }) { preview ->
                    ListItem(
                        headlineContent = { Text(text = preview.fileName) },
                        supportingContent = {
                            Text(
                                text = preview.summary.ifBlank { stringResource(id = R.string.settings_drive_preview_placeholder) },
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
private fun ThemeOptionRow(
    themeMode: ThemeMode,
    selected: Boolean,
    onThemeSelected: (ThemeMode) -> Unit
) {
    ListItem(
        headlineContent = { Text(text = themeModeLabel(themeMode)) },
        supportingContent = {
            Text(
                text = themeModeDescription(themeMode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            RadioButton(selected = selected, onClick = { onThemeSelected(themeMode) })
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DriveActionsRow(
    isConnected: Boolean,
    isSyncing: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RowActions(
            primaryLabel = if (isConnected) R.string.settings_drive_sync_now else R.string.settings_drive_connect,
            primaryEnabled = !isSyncing,
            secondaryLabel = if (isConnected) R.string.settings_drive_disconnect else null,
            onPrimary = { if (isConnected) onSync() else onConnect() },
            onSecondary = if (isConnected) onDisconnect else null
        )
        if (isSyncing) {
            Text(
                text = stringResource(id = R.string.settings_drive_syncing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RowActions(
    primaryLabel: Int,
    primaryEnabled: Boolean,
    secondaryLabel: Int?,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onPrimary, enabled = primaryEnabled) {
            Text(text = stringResource(id = primaryLabel))
        }
        if (secondaryLabel != null && onSecondary != null) {
            OutlinedButton(onClick = onSecondary, enabled = primaryEnabled) {
                Text(text = stringResource(id = secondaryLabel))
            }
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(id = R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(id = R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(id = R.string.settings_theme_dark)
}

@Composable
private fun themeModeDescription(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(id = R.string.settings_theme_system_description)
    ThemeMode.LIGHT -> stringResource(id = R.string.settings_theme_light_description)
    ThemeMode.DARK -> stringResource(id = R.string.settings_theme_dark_description)
}
