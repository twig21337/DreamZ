package com.twig.dreamzversion3.dreamsigns

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twig.dreamzversion3.R
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import com.twig.dreamzversion3.data.LocalUserPreferencesRepository
import com.twig.dreamzversion3.data.dream.DreamRepositories

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DreamSignsRoute(
    onManageIgnoredWords: () -> Unit,
    viewModel: DreamSignsViewModel = viewModel(
        factory = DreamSignsViewModel.factory(
            repository = DreamRepositories.inMemory,
            preferences = LocalUserPreferencesRepository.current
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DreamSignsScreen(
        uiState = uiState,
        onPromoteSign = viewModel::promoteSign,
        onRemovePromotedSign = viewModel::removePromotedSign,
        onBlacklistSign = viewModel::addToBlacklist,
        onManageIgnoredWords = onManageIgnoredWords
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DreamSignsScreen(
    uiState: DreamSignsUiState,
    onPromoteSign: (String) -> Unit,
    onRemovePromotedSign: (String) -> Unit,
    onBlacklistSign: (String) -> Unit,
    onManageIgnoredWords: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.dream_signs_title)) })
        }
    ) { innerPadding ->
        if (!uiState.hasDreams) {
            DreamSignsEmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "manage_blacklist") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(id = R.string.dream_signs_blacklist_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onManageIgnoredWords,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Block,
                                contentDescription = stringResource(id = R.string.dream_signs_manage_ignored_words_cd)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(id = R.string.dream_signs_manage_ignored_words_button))
                        }
                    }
                }
                item(key = "promoted_header") {
                    Text(
                        text = stringResource(id = R.string.dream_signs_promoted_section_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (uiState.promotedSigns.isEmpty()) {
                    item(key = "promoted_empty") {
                        Text(
                            text = stringResource(id = R.string.dream_signs_promoted_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(uiState.promotedSigns, key = { it.key }) { sign ->
                        PromotedDreamSignCard(
                            sign = sign,
                            onRemove = onRemovePromotedSign,
                            maxCount = uiState.maxCount
                        )
                    }
                }

                item(key = "candidates_header") {
                    Text(
                        text = stringResource(id = R.string.dream_signs_candidates_section_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (uiState.candidateSigns.isEmpty()) {
                    item(key = "no_candidates") {
                        Text(
                            text = stringResource(id = R.string.dream_signs_no_candidates),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(uiState.candidateSigns, key = { it.key }) { sign ->
                        DreamSignCandidateCard(
                            sign = sign,
                            onPromote = onPromoteSign,
                            onBlacklist = onBlacklistSign,
                            maxCount = uiState.maxCount
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DreamSignIgnoredWordsRoute(
    onNavigateBack: () -> Unit,
    viewModel: DreamSignsViewModel = viewModel(
        factory = DreamSignsViewModel.factory(
            repository = DreamRepositories.inMemory,
            preferences = LocalUserPreferencesRepository.current
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DreamSignIgnoredWordsScreen(
        ignoredWords = uiState.blacklistedSigns,
        onAddIgnoredWord = viewModel::addToBlacklist,
        onRemoveIgnoredWord = viewModel::removeFromBlacklist,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DreamSignIgnoredWordsScreen(
    ignoredWords: List<String>,
    onAddIgnoredWord: (String) -> Unit,
    onRemoveIgnoredWord: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var blacklistInput by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.dream_signs_blacklist_section_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.dream_signs_blacklist_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = blacklistInput,
                onValueChange = { blacklistInput = it },
                label = { Text(text = stringResource(id = R.string.dream_signs_blacklist_input_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val value = blacklistInput.trim()
                            if (value.isNotEmpty()) {
                                onAddIgnoredWord(value)
                                blacklistInput = ""
                            }
                        },
                        enabled = blacklistInput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(id = R.string.dream_signs_add_blacklist_cd)
                        )
                    }
                }
            )
            if (ignoredWords.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.dream_signs_blacklist_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ignoredWords.forEach { term ->
                        AssistChip(
                            onClick = { onRemoveIgnoredWord(term) },
                            label = { Text(text = term) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(
                                        id = R.string.dream_signs_remove_blacklist_cd,
                                        term
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DreamSignsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.dream_signs_empty_state_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(id = R.string.dream_signs_empty_state_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PromotedDreamSignCard(
    sign: DreamSignCandidate,
    onRemove: (String) -> Unit,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = sign.displayText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(text = stringResource(id = R.string.dream_signs_promoted_chip)) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                DreamSignFrequency(count = sign.count, maxCount = maxCount)
                DreamSignContextText(sign = sign)
                Text(
                    text = stringResource(id = sign.sourceDescriptionRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onRemove(sign.key) }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(
                        id = R.string.dream_signs_remove_promoted_cd,
                        sign.displayText
                    )
                )
            }
        }
    }
}

@Composable
private fun DreamSignCandidateCard(
    sign: DreamSignCandidate,
    onPromote: (String) -> Unit,
    onBlacklist: (String) -> Unit,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = sign.displayText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                OutlinedButton(
                    onClick = { onPromote(sign.key) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = stringResource(id = R.string.dream_signs_promote_button))
                }
                IconButton(onClick = { onBlacklist(sign.key) }) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = stringResource(
                            id = R.string.dream_signs_blacklist_candidate_cd,
                            sign.displayText
                        )
                    )
                }
            }
            DreamSignFrequency(count = sign.count, maxCount = maxCount)
            DreamSignContextText(sign = sign)
            Text(
                text = stringResource(id = sign.sourceDescriptionRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DreamSignFrequency(
    count: Int,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(text = stringResource(id = R.string.dream_signs_frequency_chip, count)) },
            colors = AssistChipDefaults.assistChipColors(
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
        if (maxCount > 0) {
            LinearProgressIndicator(
                progress = (count.coerceAtMost(maxCount)).toFloat() / maxCount.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
        Text(
            text = stringResource(id = R.string.dream_signs_frequency_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DreamSignContextText(sign: DreamSignCandidate, modifier: Modifier = Modifier) {
    val uniqueTitles = sign.dreamTitles.distinct()
    val displayedTitles = uniqueTitles.take(3)
    val titlesText = displayedTitles.joinToString(", ")
    val baseText = when {
        uniqueTitles.isEmpty() -> stringResource(id = R.string.dream_signs_seen_in_none)
        uniqueTitles.size == 1 -> stringResource(
            id = R.string.dream_signs_seen_in_single,
            uniqueTitles.size,
            titlesText
        )
        else -> stringResource(
            id = R.string.dream_signs_seen_in,
            uniqueTitles.size,
            titlesText
        )
    }
    val text = if (uniqueTitles.size > displayedTitles.size) "$baseText…" else baseText
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

private fun DreamSignCandidate.sourceDescriptionRes(): Int {
    return when {
        sources.contains(DreamSignSource.Description) && sources.contains(DreamSignSource.Tag) ->
            R.string.dream_signs_source_description_both
        sources.contains(DreamSignSource.Tag) -> R.string.dream_signs_source_description_tags
        sources.contains(DreamSignSource.Description) -> R.string.dream_signs_source_description_dreams
        else -> R.string.dream_signs_source_description_manual
    }
}
