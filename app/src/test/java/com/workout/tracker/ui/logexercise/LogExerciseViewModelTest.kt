package com.workout.tracker.ui.logexercise

import androidx.lifecycle.SavedStateHandle
import com.workout.tracker.domain.model.DifficultyLevel
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.ExerciseType
import com.workout.tracker.testing.FakeWorkoutRepository
import com.workout.tracker.testing.MainDispatcherRule
import com.workout.tracker.testing.withDefaultLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class LogExerciseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bench = Exercise(
        id = 1L,
        workoutDayId = 1L,
        name = "Bench Press",
        category = "Main",
        exerciseType = ExerciseType.WEIGHTED,
        defaultSets = 4,
        defaultReps = "6-8",
    )

    private val treadmill = Exercise(
        id = 2L,
        workoutDayId = 1L,
        name = "Incline Treadmill Walk",
        category = "Cardio",
        exerciseType = ExerciseType.CARDIO,
        defaultDurationSeconds = 900,
    )

    private val plank = Exercise(
        id = 3L,
        workoutDayId = 1L,
        name = "Plank",
        category = "Core",
        exerciseType = ExerciseType.TIMED,
        defaultSets = 3,
        defaultDurationSeconds = 45,
    )

    private fun repositoryWith(vararg exercises: Exercise) =
        FakeWorkoutRepository(exercises = exercises.toList())

    private fun viewModel(repo: FakeWorkoutRepository, exerciseId: Long) =
        LogExerciseViewModel(SavedStateHandle(mapOf("exerciseId" to exerciseId)), repo)

    private companion object {
        /** Well before today, so a saved entry never collides with the seeded one. */
        const val PREVIOUS_SESSION = 1_700_000_000_000L
    }

    // -------------------------------------------------- locale-independent round trip

    /** Prefilled values must parse back with toDoubleOrNull, which only accepts '.'. */
    @Test
    fun `weight prefill stays parseable in a comma decimal locale`() {
        val repo = repositoryWith(bench)
        repo.setLogs(listOf(ExerciseLog(id = 1, exerciseId = bench.id, weightLbs = 82.5)))

        val state = withDefaultLocale(Locale.GERMANY) {
            viewModel(repo, bench.id).uiState.value
        }

        assertEquals(82.5, state.weight.toDoubleOrNull()!!, 0.001)
    }

    @Test
    fun `saving a weighted log succeeds in a comma decimal locale`() {
        val repo = repositoryWith(bench)
        repo.setLogs(
            listOf(
                ExerciseLog(
                    id = 1,
                    exerciseId = bench.id,
                    dateTimestamp = PREVIOUS_SESSION,
                    weightLbs = 82.5,
                    setsCompleted = 4,
                ),
            ),
        )

        withDefaultLocale(Locale.GERMANY) {
            val vm = viewModel(repo, bench.id)
            vm.onRepsChange("8")
            vm.save()

            assertNull("save was blocked: ${vm.uiState.value.errorMessage}", vm.uiState.value.errorMessage)
        }

        val saved = repo.logs.maxBy { it.dateTimestamp }
        assertEquals(82.5, saved.weightLbs!!, 0.001)
        assertEquals(8, saved.repsPerSet)
        assertEquals(4, saved.setsCompleted)
    }

    /** Distance is never validated, so a parse failure would silently drop it. */
    @Test
    fun `cardio distance prefill is not silently dropped in a comma decimal locale`() {
        val repo = repositoryWith(treadmill)
        repo.setLogs(
            listOf(
                ExerciseLog(
                    id = 1,
                    exerciseId = treadmill.id,
                    dateTimestamp = PREVIOUS_SESSION,
                    durationSeconds = 900,
                    distanceMiles = 1.25,
                ),
            ),
        )

        withDefaultLocale(Locale.GERMANY) {
            val vm = viewModel(repo, treadmill.id)
            assertEquals(1.25, vm.uiState.value.distanceMiles.toDoubleOrNull()!!, 0.001)
            vm.save()
        }

        val saved = repo.logs.maxBy { it.dateTimestamp }
        assertNotNull("distance was dropped on save", saved.distanceMiles)
        assertEquals(1.25, saved.distanceMiles!!, 0.001)
    }

    @Test
    fun `weight prefill is also parseable in the default locale`() {
        val repo = repositoryWith(bench)
        repo.setLogs(listOf(ExerciseLog(id = 1, exerciseId = bench.id, weightLbs = 182.5)))

        val state = withDefaultLocale(Locale.US) { viewModel(repo, bench.id).uiState.value }

        assertEquals(182.5, state.weight.toDoubleOrNull()!!, 0.001)
    }

    // ------------------------------------------------------------------- prefilling

    @Test
    fun `sets fall back to the programmed default when there is no previous log`() {
        val vm = viewModel(repositoryWith(bench), bench.id)

        assertEquals(4, vm.uiState.value.setsCompleted)
        assertEquals("", vm.uiState.value.weight)
    }

    @Test
    fun `reps are never prefilled so the previous session is not logged by accident`() {
        val repo = repositoryWith(bench)
        repo.setLogs(listOf(ExerciseLog(id = 1, exerciseId = bench.id, weightLbs = 185.0, repsPerSet = 8)))

        assertEquals("", viewModel(repo, bench.id).uiState.value.repsPerSet)
    }

    @Test
    fun `duration is split into minutes and seconds`() {
        val vm = viewModel(repositoryWith(treadmill), treadmill.id)

        assertEquals("15", vm.uiState.value.durationMinutes)
        assertEquals("0", vm.uiState.value.durationSeconds)
    }

    @Test
    fun `an unrecognised stored difficulty is ignored rather than crashing`() {
        val repo = repositoryWith(plank)
        repo.setLogs(listOf(ExerciseLog(id = 1, exerciseId = plank.id, durationSeconds = 60, difficulty = "IMPOSSIBLE")))

        assertNull(viewModel(repo, plank.id).uiState.value.difficulty)
    }

    // ------------------------------------------------------------------- validation

    @Test
    fun `a weighted log requires a weight`() {
        val vm = viewModel(repositoryWith(bench), bench.id)
        vm.onRepsChange("8")

        vm.save()

        assertEquals("Enter the weight", vm.uiState.value.errorMessage)
        assertTrue(repositoryWith(bench).logs.isEmpty())
    }

    @Test
    fun `a weighted log rejects a non numeric weight`() {
        val repo = repositoryWith(bench)
        val vm = viewModel(repo, bench.id)
        vm.onWeightChange("heavy")
        vm.onRepsChange("8")

        vm.save()

        assertEquals("Invalid weight", vm.uiState.value.errorMessage)
        assertTrue(repo.logs.isEmpty())
    }

    @Test
    fun `a weighted log requires reps`() {
        val repo = repositoryWith(bench)
        val vm = viewModel(repo, bench.id)
        vm.onWeightChange("185")

        vm.save()

        assertEquals("Enter reps per set", vm.uiState.value.errorMessage)
        assertTrue(repo.logs.isEmpty())
    }

    @Test
    fun `a weighted log requires sets`() {
        val repo = repositoryWith(bench.copy(defaultSets = null))
        val vm = viewModel(repo, bench.id)
        vm.onWeightChange("185")
        vm.onRepsChange("8")

        vm.save()

        assertEquals("Select sets completed", vm.uiState.value.errorMessage)
        assertTrue(repo.logs.isEmpty())
    }

    @Test
    fun `a timed log requires a difficulty`() {
        val repo = repositoryWith(plank)
        val vm = viewModel(repo, plank.id)

        vm.save()

        assertEquals("Select a difficulty level", vm.uiState.value.errorMessage)
        assertTrue(repo.logs.isEmpty())
    }

    @Test
    fun `a timed log saves once a difficulty is chosen`() {
        val repo = repositoryWith(plank)
        val vm = viewModel(repo, plank.id)
        vm.onDifficultyChange(DifficultyLevel.HARD)

        vm.save()

        assertNull(vm.uiState.value.errorMessage)
        assertEquals("HARD", repo.logs.single().difficulty)
        assertEquals(45L, repo.logs.single().durationSeconds)
    }

    @Test
    fun `a cardio log requires a duration`() {
        val repo = repositoryWith(treadmill.copy(defaultDurationSeconds = null))
        val vm = viewModel(repo, treadmill.id)

        vm.save()

        assertEquals("Enter the duration", vm.uiState.value.errorMessage)
        assertTrue(repo.logs.isEmpty())
    }

    @Test
    fun `editing a field clears the previous error`() {
        val repo = repositoryWith(bench)
        val vm = viewModel(repo, bench.id)
        vm.save()
        assertNotNull(vm.uiState.value.errorMessage)

        vm.onWeightChange("185")

        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `duration is stored as total seconds`() {
        val repo = repositoryWith(plank)
        val vm = viewModel(repo, plank.id)
        vm.onDurMinChange("2")
        vm.onDurSecChange("30")
        vm.onDifficultyChange(DifficultyLevel.MEDIUM)

        vm.save()

        assertEquals(150L, repo.logs.single().durationSeconds)
    }
}
