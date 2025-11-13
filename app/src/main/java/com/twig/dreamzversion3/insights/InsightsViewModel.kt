package com.twig.dreamzversion3.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.twig.dreamzversion3.data.UserPreferencesRepository
import com.twig.dreamzversion3.data.dream.DreamRepositories
import com.twig.dreamzversion3.data.dream.DreamRepository
import com.twig.dreamzversion3.dreamsigns.DreamSignCandidate
import com.twig.dreamzversion3.dreamsigns.buildDreamSignCandidates
import com.twig.dreamzversion3.model.dream.Dream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.math.max

class InsightsViewModel(
    private val repository: DreamRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.dreams,
        preferences.dreamSignBlacklistFlow
    ) { dreams, blacklist ->
        val weeklySummary = buildWeeklySummary(dreams)
        val lucidityTrend = buildLucidityTrend(dreams)
        val emotionTrend = buildEmotionTrend(dreams)
        val topSigns = buildDreamSignCandidates(
            dreams = dreams,
            blacklist = blacklist
        ).take(TOP_DREAM_SIGNS_LIMIT)
        val heatmap = buildDreamSignHeatmap(dreams)
        InsightsUiState(
            hasDreams = dreams.isNotEmpty(),
            weeklySummary = weeklySummary,
            lucidityByWeek = lucidityTrend,
            emotionTrend = emotionTrend,
            topDreamSigns = topSigns,
            heatmap = heatmap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState()
    )

    private fun buildWeeklySummary(dreams: List<Dream>): List<DailySummary> {
        if (dreams.isEmpty()) return emptyList()
        val today = LocalDate.now(zoneId)
        val counts = dreams.groupingBy { it.createdLocalDate() }.eachCount()
        val dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
        return dates.map { date ->
            DailySummary(date = date, count = counts[date] ?: 0)
        }
    }

    private fun buildLucidityTrend(dreams: List<Dream>): List<WeeklyLucidity> {
        if (dreams.isEmpty()) return emptyList()
        val currentWeek = LocalDate.now(zoneId).startOfWeek()
        val weeks = (5 downTo 0).map { currentWeek.minusWeeks(it.toLong()) }
        val grouped = dreams.groupBy { it.createdLocalDate().startOfWeek() }
        return weeks.map { weekStart ->
            val weekDreams = grouped[weekStart].orEmpty()
            val total = weekDreams.size
            val lucid = weekDreams.count { it.isLucid }
            WeeklyLucidity(weekStart = weekStart, totalCount = total, lucidCount = lucid)
        }
    }

    private fun buildEmotionTrend(dreams: List<Dream>): List<WeeklyEmotion> {
        if (dreams.isEmpty()) return emptyList()
        val currentWeek = LocalDate.now(zoneId).startOfWeek()
        val weeks = (5 downTo 0).map { currentWeek.minusWeeks(it.toLong()) }
        val grouped = dreams.groupBy { it.createdLocalDate().startOfWeek() }
        return weeks.map { weekStart ->
            val weekDreams = grouped[weekStart].orEmpty()
            val average = if (weekDreams.isEmpty()) 0f else {
                weekDreams.map { it.emotion }.average().toFloat()
            }
            WeeklyEmotion(weekStart = weekStart, averageEmotion = average)
        }
    }

    private fun buildDreamSignHeatmap(dreams: List<Dream>): DreamSignHeatmap {
        if (dreams.isEmpty()) return DreamSignHeatmap()
        val counts = dreams.groupingBy { dream ->
            val dateTime = Instant.ofEpochMilli(dream.createdAt)
                .atZone(zoneId)
            val day = dateTime.dayOfWeek
            val segment = DaySegment.fromHour(dateTime.hour)
            day to segment
        }.eachCount()
        val mapped = DayOfWeek.values().associate { day ->
            val segmentMap = DaySegment.values().associate { segment ->
                segment to (counts[day to segment] ?: 0)
            }
            day to segmentMap
        }
        val maxCount = counts.values.maxOrNull() ?: 0
        return DreamSignHeatmap(counts = mapped, maxCount = max(maxCount, 0))
    }

    private fun Dream.createdLocalDate(): LocalDate {
        if (createdAt <= 0L) return LocalDate.now(zoneId)
        return Instant.ofEpochMilli(createdAt).atZone(zoneId).toLocalDate()
    }

    private fun LocalDate.startOfWeek(): LocalDate =
        this.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    companion object {
        private const val TOP_DREAM_SIGNS_LIMIT = 5

        fun factory(
            repository: DreamRepository = DreamRepositories.inMemory,
            preferences: UserPreferencesRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                InsightsViewModel(repository, preferences)
            }
        }
    }
}

data class InsightsUiState(
    val hasDreams: Boolean = false,
    val weeklySummary: List<DailySummary> = emptyList(),
    val lucidityByWeek: List<WeeklyLucidity> = emptyList(),
    val emotionTrend: List<WeeklyEmotion> = emptyList(),
    val topDreamSigns: List<DreamSignCandidate> = emptyList(),
    val heatmap: DreamSignHeatmap = DreamSignHeatmap()
)

data class DailySummary(
    val date: LocalDate,
    val count: Int
)

data class WeeklyLucidity(
    val weekStart: LocalDate,
    val totalCount: Int,
    val lucidCount: Int
) {
    val lucidityRatio: Float
        get() = if (totalCount == 0) 0f else lucidCount.toFloat() / totalCount.toFloat()
}

data class WeeklyEmotion(
    val weekStart: LocalDate,
    val averageEmotion: Float
)

data class DreamSignHeatmap(
    val counts: Map<DayOfWeek, Map<DaySegment, Int>> = emptyMap(),
    val maxCount: Int = 0
) {
    fun countFor(day: DayOfWeek, segment: DaySegment): Int {
        return counts[day]?.get(segment) ?: 0
    }
}

enum class DaySegment(val labelResId: Int, val range: IntRange) {
    EARLY_MORNING(com.twig.dreamzversion3.R.string.insights_segment_early_morning, 0..5),
    MORNING(com.twig.dreamzversion3.R.string.insights_segment_morning, 6..11),
    AFTERNOON(com.twig.dreamzversion3.R.string.insights_segment_afternoon, 12..17),
    NIGHT(com.twig.dreamzversion3.R.string.insights_segment_night, 18..23);

    companion object {
        fun fromHour(hour: Int): DaySegment {
            return values().firstOrNull { hour in it.range } ?: NIGHT
        }
    }
}
