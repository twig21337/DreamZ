package com.twig.dreamzversion3.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twig.dreamzversion3.R
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InsightsRoute(
    viewModel: InsightsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun InsightsScreen(
    uiState: InsightsUiState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.insights_title)) })
        }
    ) { innerPadding ->
        if (!uiState.hasDreams) {
            InsightsEmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "weekly_summary") {
                    SectionCard(title = stringResource(id = R.string.insights_weekly_summary_title)) {
                        WeeklyDreamSummary(uiState.weeklySummary)
                    }
                }
                item(key = "lucidity") {
                    SectionCard(title = stringResource(id = R.string.insights_lucidity_frequency_title)) {
                        LucidityFrequencyGraph(uiState.lucidityByWeek)
                    }
                }
                item(key = "top_signs") {
                    SectionCard(title = stringResource(id = R.string.insights_top_dream_signs_title)) {
                        TopDreamSigns(uiState.topDreamSigns)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun WeeklyDreamSummary(entries: List<DailySummary>) {
    if (entries.isEmpty()) {
        Text(
            text = stringResource(id = R.string.insights_weekly_summary_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    if (entries.all { it.count == 0 }) {
        Text(
            text = stringResource(id = R.string.insights_weekly_summary_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val maxCount = entries.maxOfOrNull { it.count } ?: 0
    val formatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        entries.forEach { entry ->
            val label = entry.date.format(formatter)
            val barHeight = entry.count.toBarHeight(maxCount, 96.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(barHeight)
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LucidityFrequencyGraph(entries: List<WeeklyLucidity>) {
    if (entries.isEmpty()) {
        Text(
            text = stringResource(id = R.string.insights_lucidity_frequency_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        entries.forEach { entry ->
            val percentage = (entry.lucidityRatio * 100).roundToInt()
            val barHeight = entry.lucidityRatio.toBarHeight(1f, 112.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.insights_percentage_value, percentage),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(barHeight)
                        .background(MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.weekStart.format(formatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TopDreamSigns(signs: List<com.twig.dreamzversion3.dreamsigns.DreamSignCandidate>) {
    if (signs.isEmpty()) {
        Text(
            text = stringResource(id = R.string.insights_top_dream_signs_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        signs.forEachIndexed { index, sign ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.insights_ranked_sign_label, index + 1, sign.displayText),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.insights_dream_sign_count, sign.count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InsightsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.insights_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.insights_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun Int.toBarHeight(maxValue: Int, maxHeight: Dp): Dp {
    if (maxValue <= 0) return 4.dp
    val fraction = (this.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
    return (maxHeight * fraction).coerceAtLeast(8.dp)
}

private fun Float.toBarHeight(maxValue: Float, maxHeight: Dp): Dp {
    if (maxValue <= 0f) return 4.dp
    val fraction = (this / maxValue).coerceIn(0f, 1f)
    return (maxHeight * fraction).coerceAtLeast(8.dp)
}
