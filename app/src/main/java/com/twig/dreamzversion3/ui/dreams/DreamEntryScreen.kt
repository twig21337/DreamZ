package com.twig.dreamzversion3.ui.dreams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twig.dreamzversion3.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamEntryRoute(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onNavigateToDream: (String) -> Unit,
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
            viewModel.startNewEntry()
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

    val dreams = uiState.dreams
    val entry = uiState.entry
    val currentIndex = entry.dreamId?.let { id -> dreams.indexOfFirst { it.id == id } } ?: -1
    val previousDreamId = if (currentIndex > 0) dreams[currentIndex - 1].id else null
    val nextDreamId = if (currentIndex != -1 && currentIndex < dreams.lastIndex) dreams[currentIndex + 1].id else null

    DreamEntryScreen(
        entryState = uiState.entry,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onMoodChange = viewModel::onMoodChange,
        onTagInputChange = viewModel::onTagInputChanged,
        onTagInputCommit = viewModel::commitTagInput,
        onTagRemove = viewModel::removeTag,
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
            viewModel.cancelEditing()
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
        onNavigateToPrevious = previousDreamId?.let { id -> { onNavigateToDream(id) } },
        onNavigateToNext = nextDreamId?.let { id -> { onNavigateToDream(id) } },
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
    onTagInputChange: (String) -> Unit,
    onTagInputCommit: () -> Unit,
    onTagRemove: (String) -> Unit,
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
    onNavigateToPrevious: (() -> Unit)?,
    onNavigateToNext: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val titleFocusRequester = remember { FocusRequester() }
    val highlightTerms = remember(entryState.tags, entryState.highlightedDreamSigns) {
        (entryState.tags + entryState.highlightedDreamSigns)
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer
    val highlightStyle = remember(tertiaryContainer, onTertiaryContainer) {
        SpanStyle(
            background = tertiaryContainer,
            color = onTertiaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
    val descriptionTransformation = remember(highlightTerms, highlightStyle) {
        DreamSignHighlightTransformation(highlightTerms, highlightStyle)
    }

    LaunchedEffect(entryState.dreamId, entryState.isEditing) {
        if (!entryState.isEditing) {
            titleFocusRequester.requestFocus()
        }
    }

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
                .dreamSwipeGesture(
                    onSwipeLeft = onNavigateToNext,
                    onSwipeRight = onNavigateToPrevious
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = entryState.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .bringIntoViewOnFocus(),
                    label = { Text(text = stringResource(id = R.string.dream_title_label)) }
                )
                OutlinedTextField(
                    value = entryState.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                    label = { Text(text = stringResource(id = R.string.dream_description_label)) },
                    supportingText = { Text(text = stringResource(id = R.string.dream_description_support)) },
                    minLines = 4,
                    visualTransformation = descriptionTransformation
                )
                TagEditorSection(
                    tags = entryState.tags,
                    input = entryState.tagInput,
                    onInputChange = onTagInputChange,
                    onCommitInput = onTagInputCommit,
                    onRemoveTag = onTagRemove
                )
                OutlinedTextField(
                    value = entryState.mood,
                    onValueChange = onMoodChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
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
                enter = scaleIn(animationSpec = tween(300)),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditorSection(
    tags: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onCommitInput: () -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.dream_tags_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                TagChip(
                    text = tag,
                    onRemove = { onRemoveTag(tag) }
                )
            }
            TagInputChip(
                value = input,
                onValueChange = onInputChange,
                onCommit = onCommitInput
            )
        }
        Text(
            text = stringResource(id = R.string.dream_tags_support),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagChip(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        onClick = onRemove,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(id = R.string.dream_tag_remove, text),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun TagInputChip(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue.replace('\n', ' '))
        },
        modifier = modifier
            .widthIn(min = 120.dp, max = 240.dp)
            .bringIntoViewOnFocus()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    onCommit()
                    true
                } else {
                    false
                }
            }
            .onFocusChanged { focusState ->
                if (!focusState.isFocused) {
                    onCommit()
                }
            },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onCommit() }),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.dream_tags_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
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

private fun Modifier.bringIntoViewOnFocus(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    this.then(
        Modifier
            .bringIntoViewRequester(requester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch { requester.bringIntoView() }
                }
            }
    )
}

private fun Modifier.dreamSwipeGesture(
    onSwipeLeft: (() -> Unit)?,
    onSwipeRight: (() -> Unit)?,
    threshold: Dp = 96.dp
): Modifier = composed {
    val leftHandler by rememberUpdatedState(onSwipeLeft)
    val rightHandler by rememberUpdatedState(onSwipeRight)
    val density = LocalDensity.current
    val thresholdPx = remember(threshold, density) { with(density) { threshold.toPx() } }
    if (leftHandler == null && rightHandler == null) {
        this
    } else {
        this.then(
            Modifier.pointerInput(leftHandler, rightHandler, thresholdPx) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    onDragCancel = { totalDrag = 0f },
                    onDragEnd = {
                        val drag = totalDrag
                        if (abs(drag) >= thresholdPx) {
                            if (drag < 0 && leftHandler != null) {
                                leftHandler?.invoke()
                            } else if (drag > 0 && rightHandler != null) {
                                rightHandler?.invoke()
                            }
                        }
                        totalDrag = 0f
                    }
                )
            }
        )
    }
}

private class DreamSignHighlightTransformation(
    private val terms: Set<String>,
    private val style: SpanStyle
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (terms.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val lower = original.lowercase()
        val builder = AnnotatedString.Builder(text)
        terms.forEach { rawTerm ->
            val normalized = rawTerm.trim().lowercase()
            if (normalized.isEmpty()) return@forEach
            var start = lower.indexOf(normalized)
            while (start >= 0) {
                val end = start + normalized.length
                val beforeValid = start == 0 || !lower[start - 1].isLetterOrDigit()
                val afterValid = end >= lower.length || !lower[end].isLetterOrDigit()
                if (beforeValid && afterValid) {
                    builder.addStyle(style, start, end)
                }
                start = lower.indexOf(normalized, start + normalized.length)
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
