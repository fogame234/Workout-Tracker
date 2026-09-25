package com.workout.tracker.ui.overview

import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.ExerciseType
import com.workout.tracker.domain.model.WalkingLog
import com.workout.tracker.testing.FakeWorkoutRepository
import com.workout.tracker.testing.MainDispatcherRule
import com.workout.tracker.testing.withDefaultLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class OverviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = System.currentTimeMillis()
    private fun daysAgo(days: Int) = now - days * 24L * 60 * 60 * 1000

    private fun exercise(id: Long, name: String, type: ExerciseType, dayId: Long = 1L) =
        Exercise(id = id, workoutDayId = dayId, name = name, category = "Main", exerciseType = type)

    private fun stateFor(
        exercises: List<Exercise>,
        logs: List<ExerciseLog> = emptyList(),
        walks: List<WalkingLog> = emptyList(),
    ): OverviewUiState = withDefaultLocale(Locale.US) {
        val repo = FakeWorkoutRepository(exercises = exercises)
        repo.setLogs(logs)
        repo.setWalkingLogs(walks)
        OverviewViewModel(repo).uiState.value
    }

    @Test
    fun `the default period is a week`() {
        assertEquals(TimePeriod.WEEK, OverviewUiState().selectedPeriod)
    }

    // ------------------------------------------------------------- tile id contract

    /** Walking tiles have no exercise row, so nothing may assume a tile has an id. */
    @Test
    fun `walking tiles carry no exercise ids`() {
        val state = stateFor(
            exercises = emptyList(),
            walks = listOf(WalkingLog(id = 1, dateTimestamp = daysAgo(1), distanceMiles = 3.0, distanceKm = 4.8, durationSeconds = 2700)),
        )

        assertEquals(2, state.walkingTiles.size)
        assertTrue(
            "walking tiles must not be given exercise ids",
            state.walkingTiles.all { it.exerciseIds.isEmpty() },
        )
    }

    @Test
    fun `walking tiles are present even with no walks recorded`() {
        val state = stateFor(exercises = emptyList())

        assertEquals(2, state.walkingTiles.size)
        assertTrue(state.walkingTiles.all { it.exerciseIds.isEmpty() })
        assertTrue(state.walkingTiles.none { it.hasData })
    }

    @Test
    fun `exercise tiles always carry at least one id`() {
        val state = stateFor(
            exercises = listOf(
                exercise(1, "Bench Press", ExerciseType.WEIGHTED),
                exercise(2, "Plank", ExerciseType.TIMED),
                exercise(3, "Easy Run", ExerciseType.CARDIO),
            ),
        )

        val tiles = state.weightTiles + state.timedTiles + state.cardioTiles
        assertTrue("expected tiles to be built", tiles.isNotEmpty())
        assertTrue(tiles.all { it.exerciseIds.isNotEmpty() })
    }

    @Test
    fun `an exercise appearing on two days produces one tile holding both ids`() {
        val state = stateFor(
            exercises = listOf(
                exercise(1, "Push-ups", ExerciseType.WEIGHTED, dayId = 1),
                exercise(9, "Push-ups", ExerciseType.WEIGHTED, dayId = 3),
            ),
        )

        val tile = state.weightTiles.single()
        assertEquals(setOf(1L, 9L), tile.exerciseIds.toSet())
    }

    // --------------------------------------------------------------------- headlines

    @Test
    fun `a weighted tile headlines the best weight ever logged`() {
        val state = stateFor(
            exercises = listOf(exercise(1, "Bench Press", ExerciseType.WEIGHTED)),
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = 1, dateTimestamp = daysAgo(10), weightLbs = 185.0),
                ExerciseLog(id = 2, exerciseId = 1, dateTimestamp = daysAgo(5), weightLbs = 205.0),
                ExerciseLog(id = 3, exerciseId = 1, dateTimestamp = daysAgo(1), weightLbs = 195.0),
            ),
        )

        assertEquals("205 lbs", state.weightTiles.single().headline)
        assertEquals("Best weight", state.weightTiles.single().subline)
    }

    @Test
    fun `a tile with no logs reports no data`() {
        val state = stateFor(exercises = listOf(exercise(1, "Bench Press", ExerciseType.WEIGHTED)))

        val tile = state.weightTiles.single()
        assertEquals("No data", tile.headline)
        assertEquals(false, tile.hasData)
        assertNull(tile.changeText)
    }

    @Test
    fun `only the key lifts appear in the weighted section`() {
        val state = stateFor(
            exercises = listOf(
                exercise(1, "Bench Press", ExerciseType.WEIGHTED),
                exercise(2, "Face Pulls", ExerciseType.WEIGHTED),
                exercise(3, "Calf Raises", ExerciseType.WEIGHTED),
            ),
        )

        assertEquals(listOf("Bench Press"), state.weightTiles.map { it.name })
    }

    @Test
    fun `a timed tile headlines the longest duration`() {
        val state = stateFor(
            exercises = listOf(exercise(1, "Plank", ExerciseType.TIMED)),
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = 1, dateTimestamp = daysAgo(5), durationSeconds = 75),
                ExerciseLog(id = 2, exerciseId = 1, dateTimestamp = daysAgo(1), durationSeconds = 125),
            ),
        )

        assertEquals("2m 5s", state.timedTiles.single().headline)
    }

    // ------------------------------------------------------------- percentage change

    @Test
    fun `change is measured against the last log before the selected period`() {
        val state = stateFor(
            exercises = listOf(exercise(1, "Bench Press", ExerciseType.WEIGHTED)),
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = 1, dateTimestamp = daysAgo(60), weightLbs = 100.0),
                ExerciseLog(id = 2, exerciseId = 1, dateTimestamp = daysAgo(1), weightLbs = 110.0),
            ),
        )

        assertEquals("+10.0%", state.weightTiles.single().changeText)
    }

    @Test
    fun `a decline is reported as a negative change`() {
        val state = stateFor(
            exercises = listOf(exercise(1, "Bench Press", ExerciseType.WEIGHTED)),
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = 1, dateTimestamp = daysAgo(60), weightLbs = 200.0),
                ExerciseLog(id = 2, exerciseId = 1, dateTimestamp = daysAgo(1), weightLbs = 180.0),
            ),
        )

        assertEquals("-10.0%", state.weightTiles.single().changeText)
    }

    @Test
    fun `no change is shown when there is no history before the period`() {
        val state = stateFor(
            exercises = listOf(exercise(1, "Bench Press", ExerciseType.WEIGHTED)),
            logs = listOf(ExerciseLog(id = 1, exerciseId = 1, dateTimestamp = daysAgo(1), weightLbs = 185.0)),
        )

        assertNull(state.weightTiles.single().changeText)
    }

    /** A faster pace is a smaller number, so the walking pace tile inverts the sign. */
    @Test
    fun `getting faster is reported as a positive pace change`() {
        val state = stateFor(
            exercises = emptyList(),
            walks = listOf(
                WalkingLog(id = 1, dateTimestamp = daysAgo(60), distanceMiles = 1.0, distanceKm = 1.61, durationSeconds = 1200),
                WalkingLog(id = 2, dateTimestamp = daysAgo(1), distanceMiles = 1.0, distanceKm = 1.61, durationSeconds = 1080),
            ),
        )

        val paceTile = state.walkingTiles.first { it.name == "Pace" }
        assertEquals("+10.0%", paceTile.changeText)
    }

    @Test
    fun `the walking distance tile headlines the longest walk`() {
        val state = stateFor(
            exercises = emptyList(),
            walks = listOf(
                WalkingLog(id = 1, dateTimestamp = daysAgo(5), distanceMiles = 2.5, distanceKm = 4.02, durationSeconds = 2400),
                WalkingLog(id = 2, dateTimestamp = daysAgo(1), distanceMiles = 4.25, distanceKm = 6.84, durationSeconds = 3600),
            ),
        )

        val distanceTile = state.walkingTiles.first { it.name == "Distance" }
        assertEquals("4.25 mi", distanceTile.headline)
        assertTrue(distanceTile.subline.contains("2 walks"))
    }

    @Test
    fun `selecting a period recomputes the tiles`() = withDefaultLocale(Locale.US) {
        val repo = FakeWorkoutRepository(exercises = listOf(exercise(1, "Bench Press", ExerciseType.WEIGHTED)))
        repo.setLogs(
            listOf(
                ExerciseLog(id = 1, exerciseId = 1, dateTimestamp = daysAgo(10), weightLbs = 100.0),
                ExerciseLog(id = 2, exerciseId = 1, dateTimestamp = daysAgo(1), weightLbs = 120.0),
            ),
        )
        val vm = OverviewViewModel(repo)

        // Over a month the 10-day-old log is inside the window, so there is no baseline.
        vm.selectPeriod(TimePeriod.MONTH)
        assertNull(vm.uiState.value.weightTiles.single().changeText)

        // Over a week it falls outside and becomes the baseline.
        vm.selectPeriod(TimePeriod.WEEK)
        assertEquals(TimePeriod.WEEK, vm.uiState.value.selectedPeriod)
        assertEquals("+20.0%", vm.uiState.value.weightTiles.single().changeText)
    }
}
