package com.songloft.tv.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songloft.tv.data.api.StatsHourlyPoint
import com.songloft.tv.data.api.StatsHistoryRecord
import com.songloft.tv.data.api.StatsSummary
import com.songloft.tv.data.api.StatsTrendPoint
import com.songloft.tv.data.repository.StatsRange
import com.songloft.tv.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val range: StatsRange = StatsRange.ALL,
    val summary: StatsSummary? = null,
    val trendsDays: Int = 7,
    val trends: List<StatsTrendPoint> = emptyList(),
    val hourly: List<StatsHourlyPoint> = emptyList(),
    val history: List<StatsHistoryRecord> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun refresh() {
        loadAll()
    }

    fun selectRange(range: StatsRange) {
        if (_uiState.value.range == range) return
        _uiState.value = _uiState.value.copy(range = range, summary = null, error = null)
        viewModelScope.launch {
            val result = statsRepository.getSummary(range)
            _uiState.value = _uiState.value.copy(
                summary = result.getOrNull(),
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun selectTrendDays(days: Int) {
        if (_uiState.value.trendsDays == days) return
        _uiState.value = _uiState.value.copy(trendsDays = days, trends = emptyList())
        viewModelScope.launch {
            val result = statsRepository.getTrends(days)
            _uiState.value = _uiState.value.copy(trends = result.getOrNull() ?: emptyList())
        }
    }

    fun refreshSummary() {
        val range = _uiState.value.range
        viewModelScope.launch {
            val result = statsRepository.getSummary(range)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(summary = result.getOrNull(), error = null)
            }
        }
    }

    fun refreshHourly() {
        viewModelScope.launch {
            val result = statsRepository.getHourly()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(hourly = result.getOrNull() ?: emptyList())
            }
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            val result = statsRepository.getHistory(HISTORY_LIMIT, 0)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(history = result.getOrNull()?.records ?: emptyList())
            }
        }
    }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val summaryDeferred = async { statsRepository.getSummary(StatsRange.ALL) }
            val trendsDeferred = async { statsRepository.getTrends(7) }
            val hourlyDeferred = async { statsRepository.getHourly() }
            val historyDeferred = async { statsRepository.getHistory(HISTORY_LIMIT, 0) }

            val summary = summaryDeferred.await()
            val trends = trendsDeferred.await().getOrNull() ?: emptyList()
            val hourly = hourlyDeferred.await().getOrNull() ?: emptyList()
            val history = historyDeferred.await().getOrNull()

            _uiState.value = StatsUiState(
                range = StatsRange.ALL,
                summary = summary.getOrNull(),
                trends = trends,
                hourly = hourly,
                history = history?.records ?: emptyList(),
                isLoading = false,
                error = summary.exceptionOrNull()?.message
            )
        }
    }

    companion object {
        const val HISTORY_LIMIT = 3
    }
}
