package com.workout.tracker.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.data.guide.ExerciseGuides
import com.workout.tracker.domain.model.ExerciseType
import com.workout.tracker.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One row in the library. Deliberately carries no guide text — that is only shown on the detail page. */
data class ExerciseLibraryItem(
    val exerciseId: Long,
    val name: String,
    val category: String,
    val hasGuide: Boolean,
)

data class ExerciseLibrarySection(
    val type: ExerciseType,
    val title: String,
    val items: List<ExerciseLibraryItem>,
)

data class ExerciseLibraryUiState(
    val sections: List<ExerciseLibrarySection> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    repository: WorkoutRepository,
    guides: ExerciseGuides,
) : ViewModel() {

    val uiState: StateFlow<ExerciseLibraryUiState> = repository.getAllExercisesFlow()
        .map { exercises ->
            // Push-ups, Step-ups and Plank appear on two days; list each once.
            val unique = exercises
                .filterNot { it.name in HIDDEN_FROM_LIBRARY }
                .groupBy { it.name }
                .map { (_, sameName) -> sameName.minBy { it.id } }

            val sections = SECTION_ORDER.mapNotNull { (type, title) ->
                val items = unique
                    .filter { it.exerciseType == type }
                    .sortedBy { it.name }
                    .map {
                        ExerciseLibraryItem(
                            exerciseId = it.id,
                            name = it.name,
                            category = it.category,
                            hasGuide = guides.hasGuide(it.name),
                        )
                    }
                if (items.isEmpty()) null else ExerciseLibrarySection(type, title, items)
            }

            ExerciseLibraryUiState(sections = sections, isLoading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryUiState())

    private companion object {
        /** Sections shown in the library, in order. Titles match the Overview screen. */
        val SECTION_ORDER = listOf(
            ExerciseType.WEIGHTED to "Weight exercises",
            ExerciseType.TIMED to "Timed exercises",
            ExerciseType.CARDIO to "Cardio",
        )

        /** Hidden from the library. Delete a name to show it again; guides are kept either way. */
        val HIDDEN_FROM_LIBRARY = setOf(
            "Incline Treadmill Walk",
            "Easy Run",
        )
    }
}
