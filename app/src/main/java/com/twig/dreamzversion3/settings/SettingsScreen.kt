package com.twig.dreamzversion3.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.twig.dreamzversion3.data.ThemeMode
import com.twig.dreamzversion3.R

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeSelected: (ThemeMode) -> Unit,
    // drive callbacks kept but unused so your nav call still compiles
    onDriveTokenChanged: (String) -> Unit = {},
    onConnectDrive: () -> Unit = {},
    onDisconnectDrive: () -> Unit = {},
    onSyncNow: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = stringResource(id = R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.settings_theme_header),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(id = R.string.settings_theme_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        uiState.availableThemeModes.forEach { mode ->
            ThemeModeRow(
                mode = mode,
                selected = mode == uiState.themeMode,
                onSelected = { onThemeSelected(mode) }
            )
        }
    }
}

@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
    selected: Boolean,
    onSelected: () -> Unit
) {
    val (titleRes, descriptionRes) = when (mode) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system to R.string.settings_theme_system_description
        ThemeMode.LIGHT -> R.string.settings_theme_light to R.string.settings_theme_light_description
        ThemeMode.DARK -> R.string.settings_theme_dark to R.string.settings_theme_dark_description
    }

    val backgroundColor: Color = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .selectable(
                selected = selected,
                onClick = onSelected,
                role = Role.RadioButton
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(id = descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
