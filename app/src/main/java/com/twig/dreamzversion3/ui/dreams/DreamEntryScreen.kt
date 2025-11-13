package com.twig.dreamzversion3.ui.dreams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twig.dreamzversion3.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamEntryRoute(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    dreamId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: DreamsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSuccessCheck by remember { mutableStateOf(false) }
    LaunchedEffect(dreamId) {
        showDeleteDialog = false
        showSuccessCheck = false
        if (dreamId != null) {
            viewModel.startEditing(dreamId)
        } else {
            viewModel.resetEntry()
        }
    }
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.resetEntry()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DreamEditorEvent.DreamSaved -> {
                    showSuccessCheck = true
                    onShowMessage(context.getString(R.string.dream_saved_snackbar, event.title))
                    delay(900)
                    onNavigateBack()
                }
                DreamEditorEvent.DreamDeleted -> {
                    onShowMessage(context.getString(R.string.dream_deleted_snackbar))
                    delay(600)
                    onNavigateBack()
                }
            }
        }
    }

    LaunchedEffect(showSuccessCheck) {
        if (showSuccessCheck) {
            delay(1200)
            showSuccessCheck = false
        }
    }

    DreamEntryScreen(
        entryState = uiState.entry,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onMoodChange = viewModel::onMoodChange,
        onTagsInputChange = viewModel::onTagsInputChange,
        onLucidChange = viewModel::onLucidChange,
        onIntensityChange = viewModel::onIntensityChange,
        onEmotionChange = viewModel::onEmotionChange,
        onRecurringChange = viewModel::onRecurringChange,
        onSave = {
            val saved = viewModel.saveDream()
            if (!saved) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.dream_save_empty_error)
                    )
                }
            }
        },
        onCancel = {
            viewModel.resetEntry()
            onNavigateBack()
        },
        onDeleteClick = { showDeleteDialog = true },
        onConfirmDelete = {
            if (!viewModel.deleteCurrentDream()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.dream_delete_error)
                    )
                }
            }
        },
        onDismissDelete = { showDeleteDialog = false },
        showDeleteDialog = showDeleteDialog,
        showDeleteButton = uiState.entry.isEditing,
        snackbarHostState = snackbarHostState,
        showSuccessCheck = showSuccessCheck,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamEntryScreen(
    entryState: DreamEntryUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onTagsInputChange: (String) -> Unit,
    onLucidChange: (Boolean) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onEmotionChange: (Float) -> Unit,
    onRecurringChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    showDeleteDialog: Boolean,
    showDeleteButton: Boolean,
    snackbarHostState: SnackbarHostState,
    showSuccessCheck: Boolean,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val title = if (entryState.isEditing) {
                        stringResource(id = R.string.edit_dream_title)
                    } else {
                        stringResource(id = R.string.add_dream_title)
                    }
                    Text(text = title)
                },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                OutlinedTextField(
                    value = entryState.tagsInput,
                    onValueChange = onTagsInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(id = R.string.dream_tags_label)) },
                    supportingText = { Text(text = stringResource(id = R.string.dream_tags_support)) }
                )
                OutlinedTextField(
                    value = entryState.mood,
                    onValueChange = onMoodChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(id = R.string.dream_mood_label)) }
                )
                LucidDreamCheckbox(
                    checked = entryState.isLucid,
                    onCheckedChange = onLucidChange
                )
                EntrySlider(
                    label = stringResource(id = R.string.dream_intensity_label),
                    value = entryState.intensity,
                    onValueChange = onIntensityChange
                )
                EntrySlider(
                    label = stringResource(id = R.string.dream_emotion_label),
                    value = entryState.emotion,
                    onValueChange = onEmotionChange
                )
                RowWithSwitch(
                    checked = entryState.isRecurring,
                    onCheckedChange = onRecurringChange
                )
                if (showDeleteButton) {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                        Text(
                            text = stringResource(id = R.string.delete_dream_cta),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
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
            AnimatedVisibility(
                visible = showSuccessCheck,
                enter = scaleIn(animationSpec = tween(300)) ,
                exit = scaleOut(animationSpec = tween(200)),
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(96.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(text = stringResource(id = R.string.delete_dream_title)) },
            text = { Text(text = stringResource(id = R.string.delete_dream_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDismissDelete()
                    onConfirmDelete()
                }) {
                    Text(text = stringResource(id = R.string.delete_dream_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text(text = stringResource(id = R.string.cancel_label))
                }
            }
        )
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

@Composable
private fun LucidDreamCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(id = R.string.lucid_dream_label),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.lucid_dream_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EntrySlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(id = R.string.dream_rating_value, value.toInt()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..10f,
            steps = 9
        )
    }
}
