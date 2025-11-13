package com.twig.dreamzversion3.insights

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twig.dreamzversion3.R
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.lerp

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
                item(key = "emotion") {
                    SectionCard(title = stringResource(id = R.string.insights_emotion_trend_title)) {
                        EmotionTrendGraph(uiState.emotionTrend)
                    }
                }
                item(key = "top_signs") {
                    SectionCard(title = stringResource(id = R.string.insights_top_dream_signs_title)) {
                        TopDreamSigns(uiState.topDreamSigns)
                    }
                }
                item(key = "heatmap") {
                    SectionCard(title = stringResource(id = R.string.insights_heatmap_title)) {
                        DreamSignHeatmap(uiState.heatmap)
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
private fun EmotionTrendGraph(entries: List<WeeklyEmotion>) {
    if (entries.isEmpty()) {
        Text(
            text = stringResource(id = R.string.insights_emotion_trend_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val pathColor = MaterialTheme.colorScheme.secondary
        val baselineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val maxEmotion = 10f
            val minEmotion = 0f
            val xSpacing = if (entries.size > 1) size.width / (entries.size - 1) else 0f
            drawLine(
                color = baselineColor,
                start = Offset(x = 0f, y = size.height),
                end = Offset(x = size.width, y = size.height),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            var previous: Offset? = null
            entries.forEachIndexed { index, entry ->
                val ratio = ((entry.averageEmotion - minEmotion) / (maxEmotion - minEmotion)).coerceIn(0f, 1f)
                val x = if (entries.size == 1) size.width / 2f else xSpacing * index
                val y = size.height - (ratio * size.height)
                val current = Offset(x, y)
                previous?.let { start ->
                    drawLine(
                        color = pathColor,
                        start = start,
                        end = current,
                        strokeWidth = 6f
                    )
                }
                drawCircle(
                    color = pathColor,
                    radius = 10f,
                    center = current
                )
                previous = current
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            entries.forEach { entry ->
                Text(
                    text = entry.weekStart.format(formatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        signs.forEachIndexed { index, sign ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.insights_ranked_sign_label, index + 1, sign.displayText),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(id = R.string.insights_dream_sign_count, sign.count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DreamSignHeatmap(data: DreamSignHeatmap) {
    if (data.counts.isEmpty()) {
        Text(
            text = stringResource(id = R.string.insights_heatmap_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val maxCount = data.maxCount.coerceAtLeast(1)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(64.dp))
            DaySegment.values().forEach { segment ->
                Text(
                    text = stringResource(id = segment.labelResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        DayOfWeek.values().forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(64.dp)
                )
                DaySegment.values().forEach { segment ->
                    val count = data.countFor(day, segment)
                    val intensity = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                    val color = lerp(
                        Color.Transparent,
                        MaterialTheme.colorScheme.primary,
                        intensity
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .background(color, MaterialTheme.shapes.small)
                            .height(48.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (intensity > 0.5f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
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
