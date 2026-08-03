package com.workout.tracker.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.ExerciseType
import com.workout.tracker.domain.model.WalkingLog
import com.workout.tracker.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

enum class TimePeriod(val label: String, val calendarField: Int, val amount: Int) {
    TODAY("Today", Calendar.DAY_OF_MONTH, 0),
    WEEK("Week", Calendar.WEEK_OF_YEAR, -1),
    MONTH("Month", Calendar.MONTH, -1),
    THREE_MONTHS("3 Months", Calendar.MONTH, -3),
}

data class ExerciseTile(
    val exerciseIds: List<Long>,
    val name: String,
    val exerciseType: ExerciseType,
    val headline: String,
    val subline: String,
    val hasData: Boolean,
    val changeText: String?,
)

data class OverviewUiState(
    val selectedPeriod: TimePeriod = TimePeriod.MONTH,
    val weightTiles: List<ExerciseTile> = emptyList(),
    val timedTiles: List<ExerciseTile> = emptyList(),
    val cardioTiles: List<ExerciseTile> = emptyList(),
    val walkingTiles: List<ExerciseTile> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    init { loadStats(showLoading = true) }

    fun refresh() { loadStats(showLoading = false) }

    fun selectPeriod(period: TimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadStats(showLoading = false)
    }

    private fun loadStats(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true) }

            val allExercises = repository.getAllExercises()
            val period = _uiState.value.selectedPeriod
            val periodStart = periodStartTimestamp(period)

            // Group by name to deduplicate across days
            val grouped = allExercises.groupBy { it.name }

            val weighted = mutableListOf<ExerciseTile>()
            val timed = mutableListOf<ExerciseTile>()
            val cardio = mutableListOf<ExerciseTile>()

            for ((name, exGroup) in grouped) {
                val type = exGroup.first().exerciseType
                val ids = exGroup.map { it.id }

                // Filter weighted to key lifts only
                if (type == ExerciseType.WEIGHTED && name !in OVERVIEW_WEIGHTED) continue

                val allLogs = ids.flatMap { id ->
                    repository.getAllLogsForExercise(id)
                }.sortedBy { it.dateTimestamp }

                val tile = buildTile(name, type, ids, allLogs, periodStart)

                when (type) {
                    ExerciseType.WEIGHTED -> weighted.add(tile)
                    ExerciseType.TIMED -> timed.add(tile)
                    ExerciseType.CARDIO -> cardio.add(tile)
                }
            }

            // Walking tiles
            val walkingLogs = repository.getAllWalkingLogsSync()
            val walkingTiles = buildWalkingTiles(walkingLogs, periodStart)

            _uiState.update {
                it.copy(
                    weightTiles = weighted,
                    timedTiles = timed,
                    cardioTiles = cardio,
                    walkingTiles = walkingTiles,
                    isLoading = false,
                )
            }
        }
    }

    private fun buildTile(
        name: String,
        type: ExerciseType,
        ids: List<Long>,
        allLogs: List<ExerciseLog>,
        periodStart: Long,
    ): ExerciseTile {
        if (allLogs.isEmpty()) {
            return ExerciseTile(
                exerciseIds = ids, name = name, exerciseType = type,
                headline = "No data",
                subline = defaultSubline(type),
                hasData = false, changeText = null,
            )
        }

        val latest = allLogs.last()
        val baseline = allLogs.lastOrNull { it.dateTimestamp < periodStart }

        return when (type) {
            ExerciseType.WEIGHTED -> {
                val maxW = allLogs.maxOf { it.weightLbs ?: 0.0 }
                val latestW = latest.weightLbs ?: 0.0
                val change = baseline?.weightLbs?.let {
                    if (it > 0) (latestW - it) / it * 100 else null
                }
                ExerciseTile(
                    exerciseIds = ids, name = name, exerciseType = type,
                    headline = "%.0f lbs".format(maxW),
                    subline = "Best weight",
                    hasData = true, changeText = fmtChange(change),
                )
            }
            ExerciseType.TIMED -> {
                val maxDur = allLogs.maxOf { it.durationSeconds ?: 0L }
                val latestDur = latest.durationSeconds ?: 0L
                val change = baseline?.durationSeconds?.let {
                    if (it > 0) (latestDur - it).toDouble() / it * 100 else null
                }
                ExerciseTile(
                    exerciseIds = ids, name = name, exerciseType = type,
                    headline = fmtDur(maxDur),
                    subline = "Best time",
                    hasData = true, changeText = fmtChange(change),
                )
            }
            ExerciseType.CARDIO -> {
                val maxDist = allLogs.maxOf { it.distanceMiles ?: 0.0 }
                val bestPace = allLogs
                    .filter { (it.distanceMiles ?: 0.0) > 0 && (it.durationSeconds ?: 0) > 0 }
                    .minOfOrNull { it.durationSeconds!! / 60.0 / it.distanceMiles!! }

                val headline = if (maxDist > 0) "%.2f mi".format(maxDist)
                               else fmtDur(allLogs.maxOf { it.durationSeconds ?: 0L })
                val sub = bestPace?.let { "%.1f min/mi pace".format(it) } ?: "Best distance"

                val latestDist = latest.distanceMiles ?: 0.0
                val change = baseline?.distanceMiles?.let {
                    if (it > 0) (latestDist - it) / it * 100 else null
                }
                ExerciseTile(
                    exerciseIds = ids, name = name, exerciseType = type,
                    headline = headline, subline = sub,
                    hasData = true, changeText = fmtChange(change),
                )
            }
        }
    }

    private fun defaultSubline(type: ExerciseType) = when (type) {
        ExerciseType.WEIGHTED -> "Weight lifted"
        ExerciseType.TIMED -> "Duration"
        ExerciseType.CARDIO -> "Distance / time"
    }

    private fun fmtDur(secs: Long): String {
        val m = secs / 60; val s = secs % 60
        return if (m > 0 && s > 0) "${m}m ${s}s" else if (m > 0) "${m}m" else "${s}s"
    }

    private fun fmtChange(pct: Double?): String? {
        if (pct == null) return null
        val sign = if (pct >= 0) "+" else ""
        return "$sign${"%.1f".format(pct)}%"
    }

    private fun periodStartTimestamp(period: TimePeriod): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        if (period == TimePeriod.TODAY) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } else {
            cal.add(period.calendarField, period.amount)
        }
        return cal.timeInMillis
    }

    private fun buildWalkingTiles(logs: List<WalkingLog>, periodStart: Long): List<ExerciseTile> {
        if (logs.isEmpty()) {
            return listOf(
                ExerciseTile(emptyList(), "Distance", ExerciseType.CARDIO, "No data", "Best distance", false, null),
                ExerciseTile(emptyList(), "Pace", ExerciseType.CARDIO, "No data", "Best pace", false, null),
            )
        }

        val latest = logs.last()
        val baseline = logs.lastOrNull { it.dateTimestamp < periodStart }

        val maxDist = logs.maxOf { it.distanceMiles }
        val latestDist = latest.distanceMiles
        val distChange = baseline?.let {
            if (it.distanceMiles > 0) (latestDist - it.distanceMiles) / it.distanceMiles * 100 else null
        }

        val bestPace = logs.filter { it.distanceMiles > 0 }
            .minOfOrNull { it.durationSeconds / 60.0 / it.distanceMiles }
        val latestPace = if (latest.distanceMiles > 0) latest.durationSeconds / 60.0 / latest.distanceMiles else null
        val baselinePace = baseline?.let { if (it.distanceMiles > 0) it.durationSeconds / 60.0 / it.distanceMiles else null }
        // Negative pace change is improvement (faster)
        val paceChange = if (latestPace != null && baselinePace != null && baselinePace > 0) {
            (latestPace - baselinePace) / baselinePace * 100
        } else null

        return listOf(
            ExerciseTile(
                emptyList(), "Distance", ExerciseType.CARDIO,
                "%.2f mi".format(maxDist),
                "Best distance  •  ${logs.size} walks",
                true, fmtChange(distChange),
            ),
            ExerciseTile(
                emptyList(), "Pace", ExerciseType.CARDIO,
                bestPace?.let { "%.1f min/mi".format(it) } ?: "—",
                "Best pace",
                bestPace != null, paceChange?.let { fmtChange(it * -1) },  // Invert: faster = positive
            ),
        )
    }

    companion object {
        private val OVERVIEW_WEIGHTED = setOf(
            "Bench Press",
            "Pull-ups / Lat Pulldown",
            "Push-ups",
            "Back Squat",
            "Deadlift",
        )
    }
}
