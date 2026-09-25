package com.workout.tracker.ui.exercises

import com.workout.tracker.data.guide.ExerciseGuides
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseType
import com.workout.tracker.testing.FakeWorkoutRepository
import com.workout.tracker.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExerciseLibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun exercise(
        id: Long,
        name: String,
        type: ExerciseType,
        dayId: Long = 1L,
        category: String = "Main",
    ) = Exercise(
        id = id,
        workoutDayId = dayId,
        name = name,
        category = category,
        exerciseType = type,
    )

    private suspend fun stateFor(exercises: List<Exercise>) =
        ExerciseLibraryViewModel(FakeWorkoutRepository(exercises = exercises), ExerciseGuides())
            .uiState.first { !it.isLoading }

    @Test
    fun `exercises are split into sections by type`() = runTest {
        val state = stateFor(
            listOf(
                exercise(1, "Bench Press", ExerciseType.WEIGHTED),
                exercise(2, "Plank", ExerciseType.TIMED, category = "Core"),
                exercise(3, "200m Intervals", ExerciseType.CARDIO, category = "Cardio"),
            ),
        )

        assertEquals(
            listOf("Weight exercises", "Timed exercises", "Cardio"),
            state.sections.map { it.title },
        )
        assertEquals(listOf("Bench Press"), state.sections[0].items.map { it.name })
        assertEquals(listOf("Plank"), state.sections[1].items.map { it.name })
        assertEquals(listOf("200m Intervals"), state.sections[2].items.map { it.name })
    }

    /** 200m Intervals is not hidden, so the Cardio section survives with one entry. */
    @Test
    fun `exercises hidden from the library are not listed`() = runTest {
        val state = stateFor(
            listOf(
                exercise(1, "200m Intervals", ExerciseType.CARDIO, category = "Cardio"),
                exercise(2, "Easy Run", ExerciseType.CARDIO, category = "Cardio"),
                exercise(3, "Incline Treadmill Walk", ExerciseType.CARDIO, category = "Cardio"),
            ),
        )

        assertEquals(listOf("Cardio"), state.sections.map { it.title })
        assertEquals(
            listOf("200m Intervals"),
            state.sections.flatMap { section -> section.items.map { it.name } },
        )
    }

    @Test
    fun `a section is dropped when every exercise in it is hidden`() = runTest {
        val state = stateFor(
            listOf(
                exercise(1, "Bench Press", ExerciseType.WEIGHTED),
                exercise(2, "Easy Run", ExerciseType.CARDIO, category = "Cardio"),
                exercise(3, "Incline Treadmill Walk", ExerciseType.CARDIO, category = "Cardio"),
            ),
        )

        assertEquals(listOf("Weight exercises"), state.sections.map { it.title })
    }

    /** The hidden exercises keep their guides so they can be restored later. */
    @Test
    fun `hidden exercises still have guide content`() {
        val guides = ExerciseGuides()

        assertTrue(guides.hasGuide("Incline Treadmill Walk"))
        assertTrue(guides.hasGuide("Easy Run"))
        assertTrue(guides.hasGuide("200m Intervals"))
    }

    /** Push-ups, Step-ups and Plank each appear on two workout days. */
    @Test
    fun `an exercise appearing on several days is listed once`() = runTest {
        val state = stateFor(
            listOf(
                exercise(1, "Push-ups", ExerciseType.WEIGHTED, dayId = 1),
                exercise(9, "Push-ups", ExerciseType.WEIGHTED, dayId = 3),
            ),
        )

        val items = state.sections.single().items
        assertEquals(1, items.size)
        assertEquals("Push-ups", items.single().name)
    }

    @Test
    fun `a deduplicated exercise links to its lowest id`() = runTest {
        val state = stateFor(
            listOf(
                exercise(9, "Push-ups", ExerciseType.WEIGHTED, dayId = 3),
                exercise(1, "Push-ups", ExerciseType.WEIGHTED, dayId = 1),
            ),
        )

        assertEquals(1L, state.sections.single().items.single().exerciseId)
    }

    @Test
    fun `items are sorted alphabetically within a section`() = runTest {
        val state = stateFor(
            listOf(
                exercise(1, "Push-ups", ExerciseType.WEIGHTED),
                exercise(2, "Bench Press", ExerciseType.WEIGHTED),
                exercise(3, "Deadlift", ExerciseType.WEIGHTED),
            ),
        )

        assertEquals(
            listOf("Bench Press", "Deadlift", "Push-ups"),
            state.sections.single().items.map { it.name },
        )
    }

    @Test
    fun `sections with no exercises are omitted`() = runTest {
        val state = stateFor(listOf(exercise(1, "Bench Press", ExerciseType.WEIGHTED)))

        assertEquals(1, state.sections.size)
        assertEquals("Weight exercises", state.sections.single().title)
    }

    @Test
    fun `an empty library produces no sections`() = runTest {
        val state = stateFor(emptyList())

        assertTrue(state.sections.isEmpty())
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `exercises are flagged by whether a guide exists`() = runTest {
        val state = stateFor(
            listOf(
                exercise(1, "Bench Press", ExerciseType.WEIGHTED),
                exercise(2, "Kettlebell Juggling", ExerciseType.WEIGHTED),
            ),
        )

        val byName = state.sections.single().items.associateBy { it.name }
        assertEquals(true, byName.getValue("Bench Press").hasGuide)
        assertEquals(false, byName.getValue("Kettlebell Juggling").hasGuide)
    }

    @Test
    fun `rows carry the category for the subtitle`() = runTest {
        val state = stateFor(listOf(exercise(1, "Plank", ExerciseType.TIMED, category = "Core")))

        assertEquals("Core", state.sections.single().items.single().category)
    }
}
