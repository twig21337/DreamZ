package com.twig.dreamzversion3

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.ShieldMoon
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.twig.dreamzversion3.data.DreamEntry
import com.twig.dreamzversion3.data.DreamRepo
import com.twig.dreamzversion3.data.UserPreferencesRepository
import com.twig.dreamzversion3.data.userPreferencesDataStore
import com.twig.dreamzversion3.signs.Dreamsign
import com.twig.dreamzversion3.ui.DreamUiState
import com.twig.dreamzversion3.ui.DreamViewModel
import com.twig.dreamzversion3.ui.SyncState
import com.twig.dreamzversion3.ui.theme.DreamZVersion3Theme
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
            onQuickAdd = vm::saveCurrentDraft,
            onClearDraft = vm::clearDraft,
            onDelete = vm::delete,
            onToggleTheme = vm::toggleDarkTheme,
            onSyncRequested = vm::enqueueSync
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamHome(
    uiState: DreamUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onLucidChange: (Boolean) -> Unit,
    onQuickAdd: () -> Unit,
    onClearDraft: () -> Unit,
    onDelete: (DreamEntry) -> Unit,
    onToggleTheme: () -> Unit,
    onSyncRequested: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as Activity

    var driveStatus by remember { mutableStateOf<String?>(null) }
    var activeFilter by remember { mutableStateOf<String?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }

    val shownEntries = remember(uiState.entries, activeFilter) {
        activeFilter?.let { query ->
            uiState.entries.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.body.contains(query, ignoreCase = true)
            }
        } ?: uiState.entries
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DreamZ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            DraftEditor(
                title = uiState.draft.title,
                body = uiState.draft.body,
                mood = uiState.draft.mood,
                lucid = uiState.draft.lucid,
                onTitleChange = onTitleChange,
                onBodyChange = onBodyChange,
                onMoodChange = onMoodChange,
                onLucidChange = onLucidChange,
                onClear = onClearDraft
            )

            Spacer(Modifier.height(16.dp))

            DriveControls(
                driveStatus = driveStatus,
                syncState = syncState,
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
                canSync = getLastAccount(context) != null
            )

            syncState.errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            DreamsignsRow(
                signs = uiState.dreamsigns,
                activeFilter = activeFilter,
                onFilterSelected = { filter ->
                    activeFilter = if (activeFilter == filter) null else filter
                }
            )

            Spacer(Modifier.height(16.dp))

            DreamList(
                entries = shownEntries,
                onDelete = onDelete,
                modifier = Modifier.weight(1f, fill = true)
            )
        }
    }
}

@Composable
private fun DraftEditor(
    title: String,
    body: String,
    mood: String,
    lucid: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onLucidChange: (Boolean) -> Unit,
    onClear: () -> Unit
) {
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
    }
}

@Composable
private fun DriveControls(
    driveStatus: String?,
    syncState: SyncState,
    onConnect: () -> Unit,
    onSync: () -> Unit,
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
        driveStatus?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
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
                    selected = activeFilter == sign.text,
                    onClick = { onFilterSelected(sign.text) },
                    label = { Text("${sign.text} (${sign.count})") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }
        activeFilter?.let { filter ->
            Spacer(Modifier.height(8.dp))
            Text("Filtering by \"$filter\"", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DreamList(
    entries: List<DreamEntry>,
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
        LazyColumn(modifier = modifier) {
            items(entries, key = { it.id }) { entry ->
                DreamCard(entry, onDelete = { onDelete(entry) })
                Spacer(Modifier.height(12.dp))
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
            Text(entry.body, style = MaterialTheme.typography.bodyMedium, maxLines = 6)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!entry.mood.isNullOrBlank()) {
                    AssistChip(onClick = {}, label = { Text(entry.mood!!) }, leadingIcon = {
                        Icon(Icons.Rounded.Mood, contentDescription = null)
                    })
                    Spacer(Modifier.width(8.dp))
                }
                if (entry.lucid) {
                    AssistChip(onClick = {}, label = { Text("Lucid") }, leadingIcon = {
                        Icon(Icons.Rounded.ShieldMoon, contentDescription = null)
                    })
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}
