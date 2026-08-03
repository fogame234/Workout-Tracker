package com.workout.tracker.ui.logexercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.domain.model.DifficultyLevel
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.ExerciseType
import com.workout.tracker.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogExerciseUiState(
    val exercise: Exercise? = null,
    val weight: String = "",
    val repsPerSet: String = "",
    val setsCompleted: Int? = null,
    val durationMinutes: String = "",
    val durationSeconds: String = "",
    val distanceMiles: String = "",
    val difficulty: DifficultyLevel? = null,
    val notes: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val recentLogs: List<ExerciseLog> = emptyList(),
)

@HiltViewModel
class LogExerciseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(LogExerciseUiState())
    val uiState: StateFlow<LogExerciseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exercise = repository.getExerciseById(exerciseId) ?: return@launch
            val lastLog = repository.getLatestLogForExercise(exerciseId)

            _uiState.update { state ->
                state.copy(
                    exercise = exercise,
                    weight = lastLog?.weightLbs?.let { "%.1f".format(it) } ?: "",
                    repsPerSet = "",
                    setsCompleted = lastLog?.setsCompleted ?: exercise.defaultSets,
                    durationMinutes = lastLog?.durationSeconds?.let { (it / 60).toString() }
                        ?: exercise.defaultDurationSeconds?.let { (it / 60).toString() } ?: "",
                    durationSeconds = lastLog?.durationSeconds?.let { (it % 60).toString() }
                        ?: exercise.defaultDurationSeconds?.let { (it % 60).toString() } ?: "",
                    distanceMiles = lastLog?.distanceMiles?.let { "%.2f".format(it) } ?: "",
                    difficulty = lastLog?.difficulty?.let {
                        try { DifficultyLevel.valueOf(it) } catch (_: Exception) { null }
                    },
                )
            }
        }

        viewModelScope.launch {
            repository.getLogsForExercise(exerciseId).collect { logs ->
                _uiState.update { it.copy(recentLogs = logs) }
            }
        }
    }

    fun onWeightChange(v: String) = _uiState.update { it.copy(weight = v, errorMessage = null) }
    fun onRepsChange(v: String) = _uiState.update { it.copy(repsPerSet = v, errorMessage = null) }
    fun onSetsChange(v: Int?) = _uiState.update { it.copy(setsCompleted = v, errorMessage = null) }
    fun onDurMinChange(v: String) = _uiState.update { it.copy(durationMinutes = v, errorMessage = null) }
    fun onDurSecChange(v: String) = _uiState.update { it.copy(durationSeconds = v, errorMessage = null) }
    fun onDistanceChange(v: String) = _uiState.update { it.copy(distanceMiles = v, errorMessage = null) }
    fun onDifficultyChange(v: DifficultyLevel) = _uiState.update { it.copy(difficulty = v, errorMessage = null) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }

    fun save() {
        val s = _uiState.value
        val ex = s.exercise ?: return

        val err = validate(ex.exerciseType, s)
        if (err != null) {
            _uiState.update { it.copy(errorMessage = err) }
            return
        }

        val durSecs = when (ex.exerciseType) {
            ExerciseType.TIMED, ExerciseType.CARDIO -> {
                val m = s.durationMinutes.toLongOrNull() ?: 0L
                val sec = s.durationSeconds.toLongOrNull() ?: 0L
                m * 60 + sec
            }
            else -> null
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.insertLog(
                ExerciseLog(
                    exerciseId = exerciseId,
                    weightLbs = s.weight.toDoubleOrNull(),
                    repsPerSet = s.repsPerSet.toIntOrNull(),
                    setsCompleted = s.setsCompleted,
                    durationSeconds = durSecs,
                    distanceMiles = s.distanceMiles.toDoubleOrNull(),
                    difficulty = s.difficulty?.name,
                    notes = s.notes.ifBlank { null },
                ),
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
            _uiState.update { it.copy(isSaved = false) }
        }
    }

    fun deleteLog(log: ExerciseLog) {
        viewModelScope.launch { repository.deleteLog(log) }
    }

    private fun validate(type: ExerciseType, s: LogExerciseUiState): String? = when (type) {
        ExerciseType.WEIGHTED -> when {
            s.weight.isBlank() -> "Enter the weight"
            s.weight.toDoubleOrNull() == null -> "Invalid weight"
            s.setsCompleted == null -> "Select sets completed"
            s.repsPerSet.isBlank() -> "Enter reps per set"
            s.repsPerSet.toIntOrNull() == null -> "Invalid reps"
            else -> null
        }
        ExerciseType.TIMED -> when {
            s.durationMinutes.isBlank() && s.durationSeconds.isBlank() -> "Enter the duration"
            s.difficulty == null -> "Select a difficulty level"
            else -> null
        }
        ExerciseType.CARDIO -> when {
            s.durationMinutes.isBlank() && s.durationSeconds.isBlank() -> "Enter the duration"
            else -> null
        }
    }
}
