package com.twig.dreamzversion3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twig.dreamzversion3.data.AppDb
import com.twig.dreamzversion3.data.DreamEntry
import com.twig.dreamzversion3.data.DreamRepo
import com.twig.dreamzversion3.ui.DreamViewModel
import com.twig.dreamzversion3.ui.theme.DreamZVersion3Theme // <— ensure this matches your Theme.kt
import androidx.compose.material3.ExperimentalMaterial3Api

import com.twig.dreamzversion3.signs.extractDreamsigns
import com.twig.dreamzversion3.signs.Dreamsign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

// activity result + context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

// Google Sign-In + our helpers
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.twig.dreamzversion3.auth.buildGoogleSignInClient
import com.twig.dreamzversion3.auth.getLastAccount
import com.twig.dreamzversion3.auth.fetchAccessToken
import com.twig.dreamzversion3.drive.ensureDreamZFolder
import com.twig.dreamzversion3.drive.upsertJsonFile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext





class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Build repo from Room
        val db = AppDb.get(this)
        val repo = DreamRepo(db.dreamDao())

        // Simple factory for VM
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return com.twig.dreamzversion3.ui.DreamViewModel(repo) as T
            }
        }

        setContent {
            DreamZVersion3Theme {
                AppScaffold(factory)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(factory: ViewModelProvider.Factory) {
    val vm: DreamViewModel = viewModel(factory = factory)

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("") }
    var lucid by remember { mutableStateOf(false) }

    val entries by vm.entries.collectAsState()

    var activeFilter by remember { mutableStateOf<String?>(null) }

    val texts = remember(entries) { entries.map { (it.title + " " + it.body) } }
    val signs: List<Dreamsign> = remember(texts) { extractDreamsigns(texts, topK = 20) }

    val shownEntries = remember(entries, activeFilter) {
        activeFilter?.let { q ->
            entries.filter { it.title.contains(q, true) || it.body.contains(q, true) }
        } ?: entries
    }

    val context = LocalContext.current
    val activity = context as android.app.Activity
    val scope = rememberCoroutineScope()
    var driveStatus by remember { mutableStateOf<String?>(null) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        driveStatus = if (task.isSuccessful) "Connected as ${task.result.email}"
        else "Sign-in failed: ${task.exception?.message}"
    }



    Scaffold(
        topBar = { TopAppBar(title = { Text("DreamZ", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                vm.addDream(title, body, mood, lucid)
                title = ""; body = ""; mood = ""; lucid = false
            }) { Text("Save Dream") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Use fillMaxWidth — no fillMaxSize on children inside Column
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Give body a fixed height box
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("What happened?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = mood,
                onValueChange = { mood = it },
                label = { Text("Mood (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = lucid, onCheckedChange = { lucid = it })
                Spacer(Modifier.width(8.dp))
                Text("Lucid?")
            }

            // --- Google Drive buttons ---
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val client = buildGoogleSignInClient(context)
                    val acct = getLastAccount(context)
                    if (acct == null) {
                        signInLauncher.launch(client.signInIntent)
                    } else {
                        driveStatus = "Already connected as ${acct.email}"
                    }
                }) {
                    Text("Connect Google Drive")
                }

                Button(
                    enabled = getLastAccount(context) != null,
                    onClick = {
                        scope.launch {
                            try {
                                driveStatus = "Authorizing…"
                                val token = fetchAccessToken(activity)

                                driveStatus = "Ensuring folder…"
                                val folderId = withContext(Dispatchers.IO) {
                                    ensureDreamZFolder(token)
                                }

                                driveStatus = "Uploading…"
                                withContext(Dispatchers.IO) {
                                    entries.forEach { e ->
                                        upsertJsonFile(
                                            token = token,
                                            parentId = folderId,
                                            name = "entries_${e.id}.json",
                                            jsonContent = entryToJson(e)
                                        )
                                    }
                                }

                                driveStatus = "Sync complete ✅"
                            } catch (t: Throwable) {
                                driveStatus = "Sync error: ${t::class.simpleName}${t.message?.let { ": $it" } ?: ""}"
                            }
                        }
                    }
                ) { Text("Sync now") }

            }
            if (!driveStatus.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(driveStatus!!, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
// --- end Google Drive buttons ---


            Text("Dreamsigns", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(signs) { s ->
                    AssistChip(
                        onClick = { activeFilter = s.text },
                        label = { Text("${s.text} (${s.count})") }
                    )
                }
            }

            if (activeFilter != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Filter: $activeFilter")
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { activeFilter = null }) { Text("Clear") }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Recent dreams", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))


            if (activeFilter != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Filter: ${activeFilter}")
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { activeFilter = null }) { Text("Clear") }
                }
            }



            // Only the list uses weight to take remaining space
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(shownEntries) { e ->
                    DreamCard(e, onDelete = { vm.delete(e) })
                    Divider()
                }
            }
        }
    }
}

@Composable
fun DreamCard(e: DreamEntry, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(e.title.ifBlank { "(untitled)" }, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(e.body, maxLines = 4)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!e.mood.isNullOrBlank()) {
                AssistChip(onClick = {}, label = { Text("Mood: ${e.mood}") })
                Spacer(Modifier.width(8.dp))
            }
            if (e.lucid) {
                AssistChip(onClick = {}, label = { Text("Lucid") })
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

private fun esc(s: String?): String =
    if (s == null) "null" else "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun entryToJson(e: com.twig.dreamzversion3.data.DreamEntry): String = """
{
  "id": ${esc(e.id)},
  "title": ${esc(e.title)},
  "body": ${esc(e.body)},
  "mood": ${esc(e.mood)},
  "lucid": ${e.lucid},
  "createdAt": ${e.createdAt},
  "editedAt": ${e.editedAt}
}
""".trimIndent()
