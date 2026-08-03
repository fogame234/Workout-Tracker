package com.workout.tracker.ui.daydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.WorkoutDay
import com.workout.tracker.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            repository.getExercisesForDay(dayId).collect { exercises ->
                val withLogs = exercises.map { exercise ->
                    ExerciseWithLastLog(
                        exercise = exercise,
                        lastLog = repository.getLatestLogForExercise(exercise.id),
                    )
                }
                _uiState.update {
                    it.copy(exercises = withLogs, isLoading = false)
                }
            }
        }
        viewModelScope.launch {
            repository.getAllWorkoutDays().collect { days ->
                val day = days.find { it.id == dayId }
                _uiState.update { it.copy(day = day) }
            }
        }
    }
}
