package com.twig.dreamzversion3.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.twig.dreamzversion3.data.ThemeMode

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
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

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
    val label = when (mode) {
        ThemeMode.SYSTEM -> "Follow system"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelected
        )
        Text(
            text = label,
            modifier = Modifier
                .padding(start = 8.dp)
        )
    }
}
