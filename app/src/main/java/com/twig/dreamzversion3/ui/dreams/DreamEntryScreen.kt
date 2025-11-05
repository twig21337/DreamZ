package com.twig.dreamzversion3.ui.dreams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twig.dreamzversion3.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamEntryRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DreamsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.resetEntry()
        }
    }
    DreamEntryScreen(
        entryState = uiState.entry,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onLucidityChange = viewModel::onLucidityChange,
        onRecurringChange = viewModel::onRecurringChange,
        onSave = { viewModel.saveDream(onSaved = onNavigateBack) },
        onCancel = {
            viewModel.resetEntry()
            onNavigateBack()
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamEntryScreen(
    entryState: DreamEntryUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLucidityChange: (Float) -> Unit,
    onRecurringChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.add_dream_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = entryState.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(id = R.string.dream_title_label)) }
            )
            OutlinedTextField(
                value = entryState.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text(text = stringResource(id = R.string.dream_description_label)) },
                supportingText = { Text(text = stringResource(id = R.string.dream_description_support)) },
                minLines = 4
            )
            Column {
                Text(text = stringResource(id = R.string.lucidity_label, entryState.lucidity.toInt()))
                Slider(
                    value = entryState.lucidity,
                    onValueChange = onLucidityChange,
                    valueRange = 0f..10f,
                    steps = 9
                )
            }
            RowWithSwitch(
                checked = entryState.isRecurring,
                onCheckedChange = onRecurringChange
            )
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = entryState.title.isNotBlank() || entryState.description.isNotBlank()
            ) {
                Text(text = stringResource(id = R.string.save_dream_cta))
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = stringResource(id = R.string.cancel_label))
            }
        }
    }
}

@Composable
private fun RowWithSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.recurring_dream_label),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.recurring_dream_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
