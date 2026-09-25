package com.workout.tracker.ui.daydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.WorkoutDay
import com.workout.tracker.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseWithLastLog(
    val exercise: Exercise,
    val lastLog: ExerciseLog? = null,
)

data class DayDetailUiState(
    val day: WorkoutDay? = null,
    val exercises: List<ExerciseWithLastLog> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val dayId: Long = checkNotNull(savedStateHandle["dayId"])

    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            exercisesWithLatestLogs().collect { exercises ->
                _uiState.update { it.copy(exercises = exercises, isLoading = false) }
            }
        }
        viewModelScope.launch {
            repository.getAllWorkoutDays().collect { days ->
                val day = days.find { it.id == dayId }
                _uiState.update { it.copy(day = day) }
            }
        }
    }

    /** Exercises for the day with their latest log, flowing so it updates on write. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun exercisesWithLatestLogs(): Flow<List<ExerciseWithLastLog>> =
        repository.getExercisesForDay(dayId)
            .flatMapLatest { exercises ->
                repository.getLogsForExercises(exercises.map { it.id })
                    .map { logs ->
                        val latestByExercise = logs
                            .groupBy { it.exerciseId }
                            .mapValues { (_, forExercise) ->
                                forExercise.maxByOrNull { it.dateTimestamp }
                            }
                        exercises.map { ExerciseWithLastLog(it, latestByExercise[it.id]) }
                    }
                    // Show the cards on the first query rather than waiting for the logs.
                    .onStart { emit(exercises.map { ExerciseWithLastLog(it) }) }
            }
}
