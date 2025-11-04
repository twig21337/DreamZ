package com.twig.dreamzversion3

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillParentMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShieldMoon
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.twig.dreamzversion3.auth.buildGoogleSignInClient
import com.twig.dreamzversion3.auth.fetchAccessToken
import com.twig.dreamzversion3.auth.getLastAccount
import com.twig.dreamzversion3.data.AppDb
import com.twig.dreamzversion3.data.BackupFrequency
import com.twig.dreamzversion3.data.DreamEntry
import com.twig.dreamzversion3.data.DreamLayoutMode
import com.twig.dreamzversion3.data.DreamRepo
import com.twig.dreamzversion3.data.UserPreferencesRepository
import com.twig.dreamzversion3.data.userPreferencesDataStore
import com.twig.dreamzversion3.signs.Dreamsign
import com.twig.dreamzversion3.ui.DateRange
import com.twig.dreamzversion3.ui.DreamUiState
import com.twig.dreamzversion3.ui.DreamViewModel
import com.twig.dreamzversion3.ui.SyncState
import com.twig.dreamzversion3.ui.theme.DreamZVersion3Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDb.get(this)
        val repo = DreamRepo(db.dreamDao())
        val preferences = UserPreferencesRepository(applicationContext.userPreferencesDataStore)
        val workManager = WorkManager.getInstance(applicationContext)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DreamViewModel(repo, preferences, workManager) as T
            }
        }

        setContent {
            DreamApp(factory)
        }
    }
}

@Composable
fun DreamApp(factory: ViewModelProvider.Factory) {
    val vm: DreamViewModel = viewModel(factory = factory)
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    DreamZVersion3Theme(darkTheme = uiState.isDarkTheme) {
        DreamHome(
            uiState = uiState,
            onTitleChange = vm::updateTitle,
            onBodyChange = vm::updateBody,
            onMoodChange = vm::updateMood,
            onLucidChange = vm::updateLucid,
            onTagToggle = vm::toggleDraftTag,
            onAddCustomTag = vm::addCustomTag,
            onIntensityChange = vm::updateIntensityRating,
            onEmotionChange = vm::updateEmotionRating,
            onLucidityRatingChange = vm::updateLucidityRating,
            onQuickAdd = vm::saveCurrentDraft,
            onClearDraft = vm::clearDraft,
            onDelete = vm::delete,
            onToggleTheme = vm::toggleDarkTheme,
            onSearchChange = vm::updateSearchQuery,
            onFilterTag = vm::selectTag,
            onClearTag = vm::clearTagFilter,
            onDateFilter = vm::setDateFilter,
            onClearDate = vm::clearDateFilter,
            onClearFilters = vm::clearFilters,
            onLayoutModeChange = vm::setLayoutMode,
            onBackupFrequencyChange = vm::scheduleBackup,
            onDreamsignSelected = vm::updateSearchQuery,
            onSyncRequested = vm::enqueueSync
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DreamHome(
    uiState: DreamUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onLucidChange: (Boolean) -> Unit,
    onTagToggle: (String) -> Unit,
    onAddCustomTag: (String) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onEmotionChange: (Float) -> Unit,
    onLucidityRatingChange: (Float) -> Unit,
    onQuickAdd: () -> Unit,
    onClearDraft: () -> Unit,
    onDelete: (DreamEntry) -> Unit,
    onToggleTheme: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterTag: (String) -> Unit,
    onClearTag: () -> Unit,
    onDateFilter: (Long?) -> Unit,
    onClearDate: () -> Unit,
    onClearFilters: () -> Unit,
    onLayoutModeChange: (DreamLayoutMode) -> Unit,
    onBackupFrequencyChange: (BackupFrequency) -> Unit,
    onDreamsignSelected: (String) -> Unit,
    onSyncRequested: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as Activity

    var driveStatus by remember { mutableStateOf<String?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.selectedDateRange?.start)
    LaunchedEffect(uiState.selectedDateRange) {
        if (uiState.selectedDateRange != null) {
            datePickerState.selectedDateMillis = uiState.selectedDateRange.start
        } else {
            datePickerState.selectedDateMillis = null
        }
    }

    val shownEntries = uiState.entries

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        driveStatus = if (task.isSuccessful) {
            "Connected as ${task.result.email}"
        } else {
            "Sign-in failed: ${task.exception?.localizedMessage ?: "Unknown error"}"
        }
    }

    val syncState = uiState.syncState

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val selectedDateLabel = uiState.selectedDateRange?.let { dateFormatter.format(Date(it.start)) }

    val listState = rememberLazyListState()
    val scrollbarStyle = ScrollbarStyle(
        minimalHeight = 24.dp,
        thickness = 6.dp,
        shape = RoundedCornerShape(3.dp),
        hoverDurationMillis = 250,
        unhoverColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        hoveredColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dream Journal Pro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        syncState.message?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = if (uiState.isDarkTheme) "Switch to light" else "Switch to dark"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Quick add") }
                )
                DropdownMenu(expanded = fabExpanded, onDismissRequest = { fabExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Add entry") },
                        leadingIcon = { Icon(Icons.Rounded.NoteAdd, contentDescription = null) },
                        onClick = {
                            fabExpanded = false
                            if (uiState.draft.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Nothing to add yet") }
                            } else {
                                onQuickAdd()
                                scope.launch { snackbarHostState.showSnackbar("Draft saved to journal") }
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Record dream (soon)") },
                        leadingIcon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
                        onClick = {
                            fabExpanded = false
                            scope.launch { snackbarHostState.showSnackbar("Voice recording is coming soon") }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                scrollbarStyle = scrollbarStyle
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DraftEditor(
                            title = uiState.draft.title,
                            body = uiState.draft.body,
                            mood = uiState.draft.mood,
                            lucid = uiState.draft.lucid,
                            availableTags = uiState.availableTags,
                            selectedTags = uiState.draft.tags,
                            intensityRating = uiState.draft.intensityRating,
                            emotionRating = uiState.draft.emotionRating,
                            lucidityRating = uiState.draft.lucidityRating,
                            onTitleChange = onTitleChange,
                            onBodyChange = onBodyChange,
                            onMoodChange = onMoodChange,
                            onLucidChange = onLucidChange,
                            onTagToggle = onTagToggle,
                            onAddCustomTag = onAddCustomTag,
                            onIntensityChange = onIntensityChange,
                            onEmotionChange = onEmotionChange,
                            onLucidityRatingChange = onLucidityRatingChange,
                            onClear = onClearDraft
                        )

                        DriveControls(
                            driveStatus = driveStatus,
                            syncState = syncState,
                            backupFrequency = uiState.backupFrequency,
                            onConnect = {
                                val client = buildGoogleSignInClient(context)
                                val account = getLastAccount(context)
                                if (account == null) {
                                    signInLauncher.launch(client.signInIntent)
                                } else {
                                    driveStatus = "Already connected as ${account.email}"
                                }
                            },
                            onSync = {
                                scope.launch {
                                    try {
                                        driveStatus = "Authorizing…"
                                        val token = fetchAccessToken(activity)
                                        driveStatus = "Sync queued"
                                        onSyncRequested(token)
                                    } catch (t: Throwable) {
                                        driveStatus = "Sync error: ${t.localizedMessage ?: t::class.simpleName}"
                                    }
                                }
                            },
                            onFrequencyChange = onBackupFrequencyChange,
                            canSync = getLastAccount(context) != null
                        )

                        driveStatus?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        }

                        syncState.errorMessage?.let { error ->
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchChange,
                            label = { Text("Search dreams") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        )

                        if (uiState.availableTags.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Filter by tag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.availableTags.forEach { tag ->
                                        FilterChip(
                                            selected = uiState.activeTag?.equals(tag, ignoreCase = true) == true,
                                            onClick = { onFilterTag(tag) },
                                            label = { Text(tag) }
                                        )
                                    }
                                }
                                if (uiState.activeTag != null) {
                                    AssistChip(onClick = onClearTag, label = { Text("Clear tag filter") })
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Browse by date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(12.dp))
                            AssistChip(
                                onClick = { showDatePicker = true },
                                label = { Text(selectedDateLabel ?: "Pick a day") },
                                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) }
                            )
                            if (uiState.selectedDateRange != null) {
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = onClearDate) { Text("Clear") }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Layout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            FilterChip(
                                selected = uiState.layoutMode == DreamLayoutMode.LIST,
                                onClick = { onLayoutModeChange(DreamLayoutMode.LIST) },
                                label = { Text("List") },
                                leadingIcon = { Icon(Icons.Rounded.ViewList, contentDescription = null) }
                            )
                            FilterChip(
                                selected = uiState.layoutMode == DreamLayoutMode.CARDS,
                                onClick = { onLayoutModeChange(DreamLayoutMode.CARDS) },
                                label = { Text("Cards") },
                                leadingIcon = { Icon(Icons.Rounded.ViewModule, contentDescription = null) }
                            )
                            Spacer(Modifier.weight(1f))
                            if (uiState.searchQuery.isNotBlank() || uiState.activeTag != null || uiState.selectedDateRange != null) {
                                TextButton(onClick = onClearFilters) { Text("Clear filters") }
                            }
                        }
                    }
                }

                if (uiState.dreamsigns.isNotEmpty()) {
                    item {
                        DreamsignsRow(
                            signs = uiState.dreamsigns,
                            activeFilter = uiState.searchQuery,
                            onFilterSelected = { sign ->
                                if (uiState.searchQuery.equals(sign, ignoreCase = true)) {
                                    onSearchChange("")
                                } else {
                                    onDreamsignSelected(sign)
                                }
                            }
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.fillParentMaxHeight()) {
                        DreamEntries(
                            entries = shownEntries,
                            layoutMode = uiState.layoutMode,
                            onDelete = onDelete,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDatePicker = false
                                onDateFilter(datePickerState.selectedDateMillis)
                            }
                        ) { Text("Apply") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DraftEditor(
    title: String,
    body: String,
    mood: String,
    lucid: Boolean,
    availableTags: List<String>,
    selectedTags: List<String>,
    intensityRating: Int,
    emotionRating: Int,
    lucidityRating: Int,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onLucidChange: (Boolean) -> Unit,
    onTagToggle: (String) -> Unit,
    onAddCustomTag: (String) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onEmotionChange: (Float) -> Unit,
    onLucidityRatingChange: (Float) -> Unit,
    onClear: () -> Unit
) {
    var customTag by remember { mutableStateOf("") }

    Column {
        Text("Draft", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = body,
            onValueChange = onBodyChange,
            label = { Text("What happened?") },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = MaterialTheme.shapes.large,
            maxLines = 8
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = mood,
            onValueChange = onMoodChange,
            label = { Text("Mood (optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Rounded.ShieldMoon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Lucid dream")
            Spacer(Modifier.width(8.dp))
            Switch(checked = lucid, onCheckedChange = onLucidChange)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Clear") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            availableTags.forEach { tag ->
                FilterChip(
                    selected = selectedTags.containsIgnoreCase(tag),
                    onClick = { onTagToggle(tag) },
                    label = { Text(tag) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = customTag,
                onValueChange = { customTag = it },
                label = { Text("Add custom tag") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    onAddCustomTag(customTag)
                    customTag = ""
                },
                enabled = customTag.isNotBlank()
            ) { Text("Add") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Ratings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        RatingSlider(
            label = "Intensity",
            icon = Icons.Rounded.Flare,
            value = intensityRating,
            onValueChange = onIntensityChange
        )
        Spacer(Modifier.height(8.dp))
        RatingSlider(
            label = "Emotion",
            icon = Icons.Rounded.Favorite,
            value = emotionRating,
            onValueChange = onEmotionChange
        )
        Spacer(Modifier.height(8.dp))
        RatingSlider(
            label = "Lucidity",
            icon = Icons.Rounded.Star,
            value = lucidityRating,
            onValueChange = onLucidityRatingChange
        )
    }
}

@Composable
private fun RatingSlider(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text("${value}/10", fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = 0f..10f,
            steps = 9
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriveControls(
    driveStatus: String?,
    syncState: SyncState,
    backupFrequency: BackupFrequency,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onFrequencyChange: (BackupFrequency) -> Unit,
    canSync: Boolean
) {
    Column {
        Text("Google Drive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onConnect,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.CloudDone, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Connect")
            }
            Button(
                onClick = onSync,
                enabled = canSync && !syncState.inProgress && !syncState.isQueued,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Rounded.CloudSync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        syncState.inProgress -> "Syncing…"
                        syncState.isQueued -> "Queued"
                        else -> "Sync now"
                    }
                )
                if (syncState.inProgress) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        BackupFrequencySelector(frequency = backupFrequency, onFrequencyChange = onFrequencyChange)
        driveStatus?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupFrequencySelector(
    frequency: BackupFrequency,
    onFrequencyChange: (BackupFrequency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = frequencyLabel(frequency),
            onValueChange = {},
            readOnly = true,
            label = { Text("Automatic backup") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            BackupFrequency.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(frequencyLabel(option)) },
                    onClick = {
                        expanded = false
                        onFrequencyChange(option)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun DreamsignsRow(
    signs: List<Dreamsign>,
    activeFilter: String?,
    onFilterSelected: (String) -> Unit
) {
    if (signs.isEmpty()) return
    Column {
        Text("Dreamsigns", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(signs) { sign ->
                FilterChip(
                    selected = activeFilter.equals(sign.text, ignoreCase = true),
                    onClick = { onFilterSelected(sign.text) },
                    label = { Text("${sign.text} (${sign.count})") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }
        activeFilter?.takeIf { it.isNotBlank() }?.let { filter ->
            Spacer(Modifier.height(8.dp))
            Text("Filtering by \"$filter\"", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DreamEntries(
    entries: List<DreamEntry>,
    layoutMode: DreamLayoutMode,
    onDelete: (DreamEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text("No dreams yet", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        when (layoutMode) {
            DreamLayoutMode.LIST -> {
                LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(entries, key = { it.id }) { entry ->
                        DreamListRow(entry, onDelete = { onDelete(entry) })
                    }
                }
            }
            DreamLayoutMode.CARDS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 260.dp),
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        DreamCard(entry, onDelete = { onDelete(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DreamListRow(entry: DreamEntry, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(entry.title.ifBlank { "(untitled)" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(entry.body, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            EntryMeta(entry)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = onDelete, label = { Text("Delete") }, leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) })
            }
        }
    }
}

@Composable
private fun DreamCard(entry: DreamEntry, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(entry.title.ifBlank { "(untitled)" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(entry.body, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            EntryMeta(entry)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryMeta(entry: DreamEntry) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!entry.mood.isNullOrBlank()) {
            AssistChip(onClick = {}, label = { Text(entry.mood!!) }, leadingIcon = { Icon(Icons.Rounded.Mood, contentDescription = null) })
        }
        if (entry.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                entry.tags.forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RatingPill(icon = Icons.Rounded.Flare, label = "Intensity", value = entry.intensityRating)
            RatingPill(icon = Icons.Rounded.Favorite, label = "Emotion", value = entry.emotionRating)
            RatingPill(icon = Icons.Rounded.Star, label = "Lucidity", value = entry.lucidityRating)
        }
        Text("Recorded ${dateFormatter.format(Date(entry.createdAt))}", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
        if (entry.lucid) {
            Text("Lucid", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RatingPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("$label ${value}/10", style = MaterialTheme.typography.bodySmall)
    }
}

private fun frequencyLabel(frequency: BackupFrequency): String = when (frequency) {
    BackupFrequency.OFF -> "Off"
    BackupFrequency.WEEKLY -> "Weekly"
    BackupFrequency.MONTHLY -> "Monthly"
}

private fun List<String>.containsIgnoreCase(value: String): Boolean = any { it.equals(value, ignoreCase = true) }
