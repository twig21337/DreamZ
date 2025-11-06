package com.twig.dreamzversion3.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twig.dreamzversion3.R
import com.twig.dreamzversion3.data.LocalUserPreferencesRepository
import com.twig.dreamzversion3.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute() {
    val preferences = LocalUserPreferencesRepository.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(preferences)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onThemeSelected = viewModel::onThemeSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeSelected: (ThemeMode) -> Unit,
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
