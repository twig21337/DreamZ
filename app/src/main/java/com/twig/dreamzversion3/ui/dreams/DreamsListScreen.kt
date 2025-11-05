package com.twig.dreamzversion3.ui.dreams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twig.dreamzversion3.R
import com.twig.dreamzversion3.model.dream.Dream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamsListRoute(
    onAddDream: () -> Unit,
    onDreamSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DreamsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DreamsListScreen(
        dreams = uiState.dreams,
        listMode = uiState.listMode,
        onAddDream = onAddDream,
        onDreamSelected = onDreamSelected,
        onToggleListMode = viewModel::toggleListMode,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamsListScreen(
    dreams: List<Dream>,
    listMode: DreamListMode,
    onAddDream: () -> Unit,
    onDreamSelected: (String) -> Unit,
    onToggleListMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.dreams_tab_label)) },
                actions = {
                    IconButton(onClick = onToggleListMode) {
                        val icon = when (listMode) {
                            DreamListMode.List -> Icons.Outlined.ViewAgenda
                            DreamListMode.Card -> Icons.Outlined.ViewList
                        }
                        val contentDescription = when (listMode) {
                            DreamListMode.List -> stringResource(id = R.string.dream_toggle_to_card_view)
                            DreamListMode.Card -> stringResource(id = R.string.dream_toggle_to_list_view)
                        }
                        Icon(imageVector = icon, contentDescription = contentDescription)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDream) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = stringResource(id = R.string.add_dream_fab))
            }
        }
    ) { innerPadding ->
        if (dreams.isEmpty()) {
            EmptyDreamsState(modifier = Modifier.padding(innerPadding))
        } else {
            when (listMode) {
                DreamListMode.List -> DreamsTitleList(
                    dreams = dreams,
                    contentPadding = innerPadding,
                    onDreamSelected = onDreamSelected
                )
                DreamListMode.Card -> DreamsDetailList(
                    dreams = dreams,
                    contentPadding = innerPadding,
                    onDreamSelected = onDreamSelected
                )
            }
        }
    }
}

@Composable
private fun DreamsTitleList(
    dreams: List<Dream>,
    contentPadding: PaddingValues,
    onDreamSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dreams, key = { it.id }) { dream ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDreamSelected(dream.id) }
            ) {
                Text(
                    text = dream.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DreamsDetailList(
    dreams: List<Dream>,
    contentPadding: PaddingValues,
    onDreamSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dreams, key = { it.id }) { dream ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDreamSelected(dream.id) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dream.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        DreamRecurringIcon(isRecurring = dream.isRecurring)
                    }
                    if (dream.description.isNotBlank()) {
                        Text(text = dream.description)
                    }
                    if (dream.mood.isNotBlank()) {
                        Text(
                            text = stringResource(id = R.string.dream_mood_value, dream.mood),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = stringResource(
                            id = R.string.lucidity_label,
                            dream.lucidity.toInt()
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(
                            id = R.string.dream_intensity_label,
                            dream.intensity.toInt()
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(
                            id = R.string.dream_emotion_label,
                            dream.emotion.toInt()
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DreamRecurringIcon(isRecurring: Boolean) {
    if (isRecurring) {
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = stringResource(id = R.string.recurring_dream_content_description)
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.Hotel,
            contentDescription = stringResource(id = R.string.standard_dream_content_description)
        )
    }
}

@Composable
private fun EmptyDreamsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.empty_dreams_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.empty_dreams_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}
