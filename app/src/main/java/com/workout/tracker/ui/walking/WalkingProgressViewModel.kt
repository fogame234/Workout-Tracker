package com.workout.tracker.ui.walking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.domain.model.WalkingLog
import com.workout.tracker.domain.repository.WorkoutRepository
import com.workout.tracker.ui.components.ChartDataPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class WalkingMetric(val label: String) {
    DISTANCE_MI("Distance (mi)"),
    DISTANCE_KM("Distance (km)"),
    DURATION("Duration (min)"),
    PACE("Pace (min/mi)"),
}

data class WalkingProgressUiState(
    val logs: List<WalkingLog> = emptyList(),
    val metrics: List<WalkingMetric> = WalkingMetric.entries,
    val selectedMetric: WalkingMetric = WalkingMetric.DISTANCE_MI,
    val chartData: List<ChartDataPoint> = emptyList(),
    val changeText: String = "",
)

@HiltViewModel
class WalkingProgressViewModel @Inject constructor(
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val dateFmt = SimpleDateFormat("M/d", Locale.getDefault())

    private val _uiState = MutableStateFlow(WalkingProgressUiState())
    val uiState: StateFlow<WalkingProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllWalkingLogs().collect { logs ->
                _uiState.update { s ->
                    val data = chart(logs, s.selectedMetric)
                    s.copy(logs = logs, chartData = data, changeText = change(data))
                }
            }
        }
    }

    fun selectMetric(m: WalkingMetric) {
        _uiState.update { s ->
            val data = chart(s.logs, m)
            s.copy(selectedMetric = m, chartData = data, changeText = change(data))
        }
    }

    private fun chart(logs: List<WalkingLog>, metric: WalkingMetric): List<ChartDataPoint> =
        logs.mapNotNull { l ->
            val v = when (metric) {
                WalkingMetric.DISTANCE_MI -> l.distanceMiles.toFloat()
                WalkingMetric.DISTANCE_KM -> l.distanceKm.toFloat()
                WalkingMetric.DURATION -> l.durationSeconds / 60f
                WalkingMetric.PACE -> {
                    if (l.distanceMiles <= 0) return@mapNotNull null
                    (l.durationSeconds / 60.0 / l.distanceMiles).toFloat()
                }
            }
            ChartDataPoint(dateFmt.format(Date(l.dateTimestamp)), v)
        }

    private fun change(data: List<ChartDataPoint>): String {
        if (data.size < 2) return ""
        val first = data.first().value
        val last = data.last().value
        val diff = last - first
        val pct = if (first != 0f) diff / first * 100 else 0f
        val sign = if (diff >= 0) "+" else ""
        return "$sign${"%.1f".format(diff)}  ($sign${"%.1f".format(pct)}%)"
    }
}
