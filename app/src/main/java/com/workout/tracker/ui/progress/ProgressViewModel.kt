package com.workout.tracker.ui.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.ExerciseType
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

enum class Metric(val label: String) {
    WEIGHT("Weight (lbs)"),
    VOLUME("Total Volume"),
    REPS("Reps / Set"),
    DURATION("Duration (min)"),
    DISTANCE("Distance (mi)"),
    PACE("Pace (min/mi)"),
}

data class ProgressUiState(
    val exercise: Exercise? = null,
    val entries: List<ExerciseLog> = emptyList(),
    val metrics: List<Metric> = emptyList(),
    val selectedMetric: Metric? = null,
    val chartData: List<ChartDataPoint> = emptyList(),
    val changeText: String = "",
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])
    private val dateFmt = SimpleDateFormat("M/d", Locale.getDefault())

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        // Resolve the metric before collecting logs, or the first chart renders empty.
        viewModelScope.launch {
            val ex = repository.getExerciseById(exerciseId) ?: return@launch
            val metrics = metricsFor(ex.exerciseType)
            _uiState.update { it.copy(exercise = ex, metrics = metrics, selectedMetric = metrics.firstOrNull()) }

            repository.getLogsForExercise(exerciseId).collect { logs ->
                _uiState.update { s ->
                    val data = chart(logs, s.selectedMetric)
                    s.copy(entries = logs, chartData = data, changeText = change(data))
                }
            }
        }
    }

    fun selectMetric(m: Metric) {
        _uiState.update { s ->
            val data = chart(s.entries, m)
            s.copy(selectedMetric = m, chartData = data, changeText = change(data))
        }
    }

    private fun chart(logs: List<ExerciseLog>, metric: Metric?): List<ChartDataPoint> {
        if (metric == null) return emptyList()
        return logs.mapNotNull { l ->
            val v = when (metric) {
                Metric.WEIGHT -> l.weightLbs?.toFloat()
                Metric.VOLUME -> {
                    val w = l.weightLbs ?: return@mapNotNull null
                    val r = l.repsPerSet ?: return@mapNotNull null
                    val s = l.setsCompleted ?: return@mapNotNull null
                    (w * r * s).toFloat()
                }
                Metric.REPS -> l.repsPerSet?.toFloat()
                Metric.DURATION -> l.durationSeconds?.let { it / 60f }
                Metric.DISTANCE -> l.distanceMiles?.toFloat()
                Metric.PACE -> {
                    val s = l.durationSeconds ?: return@mapNotNull null
                    val d = l.distanceMiles ?: return@mapNotNull null
                    if (d <= 0) return@mapNotNull null
                    (s / 60.0 / d).toFloat()
                }
            }
            v?.let { ChartDataPoint(dateFmt.format(Date(l.dateTimestamp)), it) }
        }
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

    private fun metricsFor(type: ExerciseType) = when (type) {
        ExerciseType.WEIGHTED -> listOf(Metric.WEIGHT, Metric.VOLUME, Metric.REPS)
        ExerciseType.TIMED -> listOf(Metric.DURATION)
        ExerciseType.CARDIO -> listOf(Metric.DISTANCE, Metric.DURATION, Metric.PACE)
    }
}
