package com.workout.tracker.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.data.guide.ExerciseGuides
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseGuide
import com.workout.tracker.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseInfoUiState(
    val exercise: Exercise? = null,
    val guide: ExerciseGuide? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ExerciseInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
    private val guides: ExerciseGuides,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(ExerciseInfoUiState())
    val uiState: StateFlow<ExerciseInfoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exercise = repository.getExerciseById(exerciseId)
            _uiState.update {
                it.copy(
                    exercise = exercise,
                    guide = exercise?.let { e -> guides.forName(e.name) },
                    isLoading = false,
                )
            }
        }
    }
}
