package com.workout.tracker.ui.overview

import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.ExerciseType
import com.workout.tracker.domain.model.WalkingLog
import com.workout.tracker.testing.FakeWorkoutRepository
import com.workout.tracker.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SessionSummaryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Midday today, so shifting by whole days never straddles a boundary. */
    private val noonToday: Long = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun daysAgo(days: Int): Long = Calendar.getInstance(TimeZone.getDefault()).apply {
        timeInMillis = noonToday
        add(Calendar.DAY_OF_MONTH, -days)
    }.timeInMillis

    private val bench = Exercise(
        id = 1L, workoutDayId = 1L, name = "Bench Press",
        category = "Main", exerciseType = ExerciseType.WEIGHTED,
    )
    private val plank = Exercise(
        id = 2L, workoutDayId = 1L, name = "Plank",
        category = "Core", exerciseType = ExerciseType.TIMED,
    )
    private val intervals = Exercise(
        id = 3L, workoutDayId = 3L, name = "200m Intervals",
        category = "Cardio", exerciseType = ExerciseType.CARDIO,
    )

    private suspend fun summaryFor(
        logs: List<ExerciseLog> = emptyList(),
        walks: List<WalkingLog> = emptyList(),
    ): SessionSummary? {
        val repo = FakeWorkoutRepository(exercises = listOf(bench, plank, intervals))
        repo.setLogs(logs)
        repo.setWalkingLogs(walks)
        return SessionSummaryViewModel(repo).uiState.first { !it.isLoading }.summary
    }

    // ------------------------------------------------------------------ today

    @Test
    fun `shows what was logged today`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = bench.id, dateTimestamp = noonToday, weightLbs = 185.0, repsPerSet = 8, setsCompleted = 4),
            ),
        )

        requireNotNull(summary)
        assertTrue(summary.isToday)
        assertEquals(0, summary.daysAgo)
        assertEquals(listOf("Bench Press"), summary.entries.map { it.name })
    }

    @Test
    fun `today wins even when older sessions exist`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = bench.id, dateTimestamp = daysAgo(3), weightLbs = 175.0),
                ExerciseLog(id = 2, exerciseId = plank.id, dateTimestamp = noonToday, durationSeconds = 75),
            ),
        )

        requireNotNull(summary)
        assertTrue(summary.isToday)
        assertEquals(listOf("Plank"), summary.entries.map { it.name })
    }

    @Test
    fun `entries are ordered by the time they were logged`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = plank.id, dateTimestamp = noonToday + 3_600_000, durationSeconds = 75),
                ExerciseLog(id = 2, exerciseId = bench.id, dateTimestamp = noonToday, weightLbs = 185.0),
            ),
        )

        assertEquals(listOf("Bench Press", "Plank"), summary!!.entries.map { it.name })
    }

    // ----------------------------------------------------------- rest-day fallback

    /** On a four-day program most days have nothing logged, so the card falls back. */
    @Test
    fun `falls back to the most recent session on a rest day`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = bench.id, dateTimestamp = daysAgo(9), weightLbs = 165.0),
                ExerciseLog(id = 2, exerciseId = plank.id, dateTimestamp = daysAgo(4), durationSeconds = 90),
            ),
        )

        requireNotNull(summary)
        assertEquals(false, summary.isToday)
        assertEquals(4, summary.daysAgo)
        assertEquals(listOf("Plank"), summary.entries.map { it.name })
    }

    @Test
    fun `the fallback groups everything from that one day`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = bench.id, dateTimestamp = daysAgo(2), weightLbs = 185.0),
                ExerciseLog(id = 2, exerciseId = plank.id, dateTimestamp = daysAgo(2) + 600_000, durationSeconds = 75),
                ExerciseLog(id = 3, exerciseId = bench.id, dateTimestamp = daysAgo(5), weightLbs = 175.0),
            ),
        )

        assertEquals(listOf("Bench Press", "Plank"), summary!!.entries.map { it.name })
        assertEquals(2, summary.daysAgo)
    }

    @Test
    fun `nothing logged ever produces no summary`() = runTest {
        assertNull(summaryFor())
    }

    // ---------------------------------------------------------------- walking

    @Test
    fun `walking appears alongside exercises`() = runTest {
        val summary = summaryFor(
            logs = listOf(ExerciseLog(id = 1, exerciseId = bench.id, dateTimestamp = noonToday, weightLbs = 185.0)),
            walks = listOf(WalkingLog(id = 1, dateTimestamp = noonToday + 600_000, distanceMiles = 3.1, distanceKm = 4.99, durationSeconds = 2_700)),
        )

        assertEquals(listOf("Bench Press", "Walk"), summary!!.entries.map { it.name })
    }

    @Test
    fun `a walk on its own still forms a session`() = runTest {
        val summary = summaryFor(
            walks = listOf(WalkingLog(id = 1, dateTimestamp = noonToday, distanceMiles = 2.0, distanceKm = 3.22, durationSeconds = 1_800)),
        )

        requireNotNull(summary)
        assertTrue(summary.isToday)
        assertEquals(listOf("Walk"), summary.entries.map { it.name })
    }

    /** Walking has no exercise row, so the card routes it to the walking screen instead. */
    @Test
    fun `walking entries carry no exercise id and exercises do`() = runTest {
        val summary = summaryFor(
            logs = listOf(ExerciseLog(id = 1, exerciseId = bench.id, dateTimestamp = noonToday, weightLbs = 185.0)),
            walks = listOf(WalkingLog(id = 1, dateTimestamp = noonToday, distanceMiles = 2.0, distanceKm = 3.22, durationSeconds = 1_800)),
        )

        val byName = summary!!.entries.associateBy { it.name }
        assertEquals(bench.id, byName.getValue("Bench Press").exerciseId)
        assertNull(byName.getValue("Walk").exerciseId)
    }

    // -------------------------------------------------------------- formatting

    @Test
    fun `a weighted entry reads as weight by reps by sets`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = bench.id, dateTimestamp = noonToday, weightLbs = 185.0, repsPerSet = 8, setsCompleted = 4),
            ),
        )

        assertEquals("185 lbs  ×  8  ×  4", summary!!.entries.single().detail)
    }

    @Test
    fun `a bodyweight entry reads as BW`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = bench.id, dateTimestamp = noonToday, repsPerSet = 15, setsCompleted = 3),
            ),
        )

        assertEquals("BW  ×  15  ×  3", summary!!.entries.single().detail)
    }

    @Test
    fun `a timed entry reads as duration and difficulty`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = plank.id, dateTimestamp = noonToday, durationSeconds = 75, difficulty = "HARD"),
            ),
        )

        assertEquals("1m 15s  ·  Fully challenging", summary!!.entries.single().detail)
    }

    @Test
    fun `an unrecognised difficulty is dropped rather than shown raw`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = plank.id, dateTimestamp = noonToday, durationSeconds = 120, difficulty = "IMPOSSIBLE"),
            ),
        )

        assertEquals("2m", summary!!.entries.single().detail)
    }

    @Test
    fun `a cardio entry reads as distance and duration`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = intervals.id, dateTimestamp = noonToday, distanceMiles = 1.5, durationSeconds = 600),
            ),
        )

        assertEquals("1.50 mi  ·  10m", summary!!.entries.single().detail)
    }

    @Test
    fun `a cardio entry with no distance still shows its duration`() = runTest {
        val summary = summaryFor(
            logs = listOf(
                ExerciseLog(id = 1, exerciseId = intervals.id, dateTimestamp = noonToday, durationSeconds = 905),
            ),
        )

        assertEquals("15m 5s", summary!!.entries.single().detail)
    }

    @Test
    fun `a walk reads as distance and duration`() = runTest {
        val summary = summaryFor(
            walks = listOf(WalkingLog(id = 1, dateTimestamp = noonToday, distanceMiles = 3.1, distanceKm = 4.99, durationSeconds = 2_700)),
        )

        assertEquals("3.10 mi  ·  45m", summary!!.entries.single().detail)
    }

    // ------------------------------------------------------------------ reactivity

    @Test
    fun `logging a workout updates the card without recreating it`() = runTest {
        val repo = FakeWorkoutRepository(exercises = listOf(bench, plank, intervals))
        val vm = SessionSummaryViewModel(repo)
        assertNull(vm.uiState.first { !it.isLoading }.summary)

        repo.insertLog(ExerciseLog(exerciseId = bench.id, dateTimestamp = noonToday, weightLbs = 185.0))

        val summary = vm.uiState.value.summary
        requireNotNull(summary)
        assertTrue(summary.isToday)
        assertEquals(listOf("Bench Press"), summary.entries.map { it.name })
    }
}
