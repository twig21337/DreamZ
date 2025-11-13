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
        val topSigns = buildDreamSignCandidates(
            dreams = dreams,
            blacklist = blacklist
        ).take(TOP_DREAM_SIGNS_LIMIT)
        InsightsUiState(
            hasDreams = dreams.isNotEmpty(),
            weeklySummary = weeklySummary,
            lucidityByWeek = lucidityTrend,
            topDreamSigns = topSigns
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
    val topDreamSigns: List<DreamSignCandidate> = emptyList()
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

