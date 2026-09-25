package com.workout.tracker.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.domain.model.DifficultyLevel
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.ExerciseType
import com.workout.tracker.domain.model.WalkingLog
import com.workout.tracker.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.roundToInt

/** One logged activity in the session card. */
data class SessionEntry(
    val name: String,
    val detail: String,
    /** Null for walking, which is not backed by an exercise row. */
    val exerciseId: Long?,
)

data class SessionSummary(
    /** Midnight of the day being shown, in the device time zone. */
    val dateTimestamp: Long,
    val isToday: Boolean,
    val daysAgo: Int,
    val entries: List<SessionEntry>,
)

data class SessionSummaryUiState(
    /** Null once loaded means nothing has ever been logged. */
    val summary: SessionSummary? = null,
    val isLoading: Boolean = true,
)

/** Today's logged activity, falling back to the most recent session. */
@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    repository: WorkoutRepository,
) : ViewModel() {

    val uiState: StateFlow<SessionSummaryUiState> = combine(
        repository.getAllExercisesFlow(),
        repository.getAllLogsFlow(),
        repository.getAllWalkingLogs(),
    ) { exercises, logs, walks ->
        buildState(exercises, logs, walks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionSummaryUiState())

    private fun buildState(
        exercises: List<Exercise>,
        logs: List<ExerciseLog>,
        walks: List<WalkingLog>,
    ): SessionSummaryUiState {
        val exercisesById = exercises.associateBy { it.id }

        val dated = buildList {
            logs.forEach { log ->
                val exercise = exercisesById[log.exerciseId] ?: return@forEach
                add(
                    log.dateTimestamp to SessionEntry(
                        name = exercise.name,
                        detail = detailFor(log, exercise.exerciseType),
                        exerciseId = exercise.id,
                    ),
                )
            }
            walks.forEach { walk ->
                add(walk.dateTimestamp to SessionEntry("Walk", detailFor(walk), exerciseId = null))
            }
        }

        if (dated.isEmpty()) return SessionSummaryUiState(summary = null, isLoading = false)

        val today = startOfDay(System.currentTimeMillis())
        val byDay = dated.groupBy { startOfDay(it.first) }

        // Fall back to the latest session so the card is useful on rest days.
        val day = if (byDay.containsKey(today)) today else byDay.keys.max()

        return SessionSummaryUiState(
            summary = SessionSummary(
                dateTimestamp = day,
                isToday = day == today,
                daysAgo = ((today - day).toDouble() / DAY_MILLIS).roundToInt(),
                entries = byDay.getValue(day).sortedBy { it.first }.map { it.second },
            ),
            isLoading = false,
        )
    }

    private fun detailFor(log: ExerciseLog, type: ExerciseType): String = when (type) {
        ExerciseType.WEIGHTED -> listOfNotNull(
            log.weightLbs?.let { "%.0f lbs".format(it) } ?: "BW",
            log.repsPerSet?.toString(),
            log.setsCompleted?.toString(),
        ).joinToString("  ×  ")

        ExerciseType.TIMED -> listOfNotNull(
            log.durationSeconds?.let(::formatDuration),
            log.difficulty?.let { stored ->
                runCatching { DifficultyLevel.valueOf(stored).displayName }.getOrNull()
            },
        ).joinToString("  ·  ").ifBlank { "Logged" }

        ExerciseType.CARDIO -> listOfNotNull(
            log.distanceMiles?.takeIf { it > 0 }?.let { "%.2f mi".format(it) },
            log.durationSeconds?.let(::formatDuration),
        ).joinToString("  ·  ").ifBlank { "Logged" }
    }

    private fun detailFor(walk: WalkingLog): String = listOfNotNull(
        "%.2f mi".format(walk.distanceMiles),
        formatDuration(walk.durationSeconds),
    ).joinToString("  ·  ")

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainder = seconds % 60
        return when {
            minutes > 0 && remainder > 0 -> "${minutes}m ${remainder}s"
            minutes > 0 -> "${minutes}m"
            else -> "${remainder}s"
        }
    }

    private fun startOfDay(timestamp: Long): Long =
        Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private companion object {
        const val DAY_MILLIS = 24.0 * 60 * 60 * 1000
    }
}
