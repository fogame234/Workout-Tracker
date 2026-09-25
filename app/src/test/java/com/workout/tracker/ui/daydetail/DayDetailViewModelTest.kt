package com.workout.tracker.ui.daydetail

import androidx.lifecycle.SavedStateHandle
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.ExerciseType
import com.workout.tracker.domain.model.WorkoutDay
import com.workout.tracker.testing.FakeWorkoutRepository
import com.workout.tracker.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DayDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dayOne = WorkoutDay(id = 1L, dayNumber = 1, title = "Upper Body Strength", subtitle = "Upper + Cardio")
    private val dayTwo = WorkoutDay(id = 2L, dayNumber = 2, title = "Lower Body Strength", subtitle = "Lower + Run")

    private val bench = Exercise(
        id = 10L, workoutDayId = 1L, name = "Bench Press",
        category = "Main", exerciseType = ExerciseType.WEIGHTED,
    )
    private val plank = Exercise(
        id = 11L, workoutDayId = 1L, name = "Plank",
        category = "Core", exerciseType = ExerciseType.TIMED,
    )
    private val squat = Exercise(
        id = 20L, workoutDayId = 2L, name = "Back Squat",
        category = "Main", exerciseType = ExerciseType.WEIGHTED,
    )

    private fun repository() = FakeWorkoutRepository(
        days = listOf(dayOne, dayTwo),
        exercises = listOf(bench, plank, squat),
    )

    private fun viewModel(repo: FakeWorkoutRepository, dayId: Long = 1L) =
        DayDetailViewModel(SavedStateHandle(mapOf("dayId" to dayId)), repo)

    // --------------------------------------------------------------- first paint

    /** Cards must appear off the first query, with the last-log line filled in after. */
    @Test
    fun `exercises are shown even when no logs exist yet`() = runTest {
        val state = viewModel(repository()).uiState.value

        assertEquals(listOf("Bench Press", "Plank"), state.exercises.map { it.exercise.name })
        assertTrue(state.exercises.all { it.lastLog == null })
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `only exercises belonging to the requested day are shown`() = runTest {
        val state = viewModel(repository(), dayId = 2L).uiState.value

        assertEquals(listOf("Back Squat"), state.exercises.map { it.exercise.name })
    }

    @Test
    fun `the day header is resolved from the day id`() = runTest {
        assertEquals("Upper Body Strength", viewModel(repository()).uiState.value.day?.title)
        assertEquals(2, viewModel(repository(), dayId = 2L).uiState.value.day?.dayNumber)
    }

    @Test
    fun `an unknown day id leaves the header empty rather than crashing`() = runTest {
        val state = viewModel(repository(), dayId = 99L).uiState.value

        assertNull(state.day)
        assertTrue(state.exercises.isEmpty())
    }

    // ------------------------------------------------------------- last log line

    @Test
    fun `the most recent log is attached to each exercise`() = runTest {
        val repo = repository()
        repo.setLogs(
            listOf(
                ExerciseLog(id = 1, exerciseId = bench.id, dateTimestamp = 1_000L, weightLbs = 165.0),
                ExerciseLog(id = 2, exerciseId = bench.id, dateTimestamp = 3_000L, weightLbs = 185.0),
                ExerciseLog(id = 3, exerciseId = bench.id, dateTimestamp = 2_000L, weightLbs = 175.0),
            ),
        )

        val benchRow = viewModel(repo).uiState.value.exercises.first { it.exercise.id == bench.id }

        assertEquals(185.0, benchRow.lastLog!!.weightLbs!!, 0.001)
    }

    @Test
    fun `an exercise with no logs keeps a null last log`() = runTest {
        val repo = repository()
        repo.setLogs(listOf(ExerciseLog(id = 1, exerciseId = bench.id, dateTimestamp = 1_000L, weightLbs = 165.0)))

        val rows = viewModel(repo).uiState.value.exercises.associateBy { it.exercise.name }

        assertEquals(165.0, rows.getValue("Bench Press").lastLog!!.weightLbs!!, 0.001)
        assertNull(rows.getValue("Plank").lastLog)
    }

    @Test
    fun `logs belonging to another day do not leak in`() = runTest {
        val repo = repository()
        repo.setLogs(listOf(ExerciseLog(id = 1, exerciseId = squat.id, dateTimestamp = 1_000L, weightLbs = 225.0)))

        val state = viewModel(repo).uiState.value

        assertTrue(state.exercises.all { it.lastLog == null })
    }

    /** The summary has to update in place, not just when the screen is recreated. */
    @Test
    fun `writing a log updates the summary without recreating the screen`() = runTest {
        val repo = repository()
        val vm = viewModel(repo)

        assertNull(vm.uiState.value.exercises.first { it.exercise.id == bench.id }.lastLog)

        repo.insertLog(ExerciseLog(exerciseId = bench.id, dateTimestamp = 5_000L, weightLbs = 205.0))

        val updated = vm.uiState.value.exercises.first { it.exercise.id == bench.id }
        assertEquals(205.0, updated.lastLog!!.weightLbs!!, 0.001)
    }

    @Test
    fun `deleting the only log clears the summary`() = runTest {
        val repo = repository()
        val vm = viewModel(repo)
        repo.insertLog(ExerciseLog(exerciseId = bench.id, dateTimestamp = 5_000L, weightLbs = 205.0))
        val saved = repo.logs.single()

        repo.deleteLog(saved)

        assertNull(vm.uiState.value.exercises.first { it.exercise.id == bench.id }.lastLog)
    }
}
