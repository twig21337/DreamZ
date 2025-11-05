package com.twig.dreamzversion3.dreamsigns

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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twig.dreamzversion3.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamSignsRoute(viewModel: DreamSignsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DreamSignsScreen(
        uiState = uiState,
        onPromoteSign = viewModel::promoteSign,
        onRemovePromotedSign = viewModel::removePromotedSign
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamSignsScreen(
    uiState: DreamSignsUiState,
    onPromoteSign: (String) -> Unit,
    onRemovePromotedSign: (String) -> Unit,
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
                if (uiState.promotedSigns.isNotEmpty()) {
                    item(key = "promoted_header") {
                        Text(
                            text = stringResource(id = R.string.dream_signs_promoted_section_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(uiState.promotedSigns, key = { it.key }) { sign ->
                        PromotedDreamSignCard(sign = sign, onRemove = onRemovePromotedSign)
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
                        DreamSignCandidateCard(sign = sign, onPromote = onPromoteSign)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = sign.displayText, style = MaterialTheme.typography.titleMedium)
                if (sign.count > 0) {
                    Text(
                        text = stringResource(id = R.string.dream_signs_frequency_count, sign.count),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = sign.displayText, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(id = R.string.dream_signs_frequency_count, sign.count),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(id = sign.sourceDescriptionRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = { onPromote(sign.key) }) {
                Text(text = stringResource(id = R.string.dream_signs_promote_button))
            }
        }
    }
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
