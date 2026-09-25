package com.workout.tracker.data.repository

import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.WalkingLog
import com.workout.tracker.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class WorkoutRepositoryImplTest : DatabaseTest() {

    private val repository by lazy {
        WorkoutRepositoryImpl(dayDao, exerciseDao, logDao, walkingDao)
    }

    private suspend fun benchPressId() = exerciseDao.getAll().first { it.name == "Bench Press" }.id
    private suspend fun squatId() = exerciseDao.getAll().first { it.name == "Back Squat" }.id

    /** Local-time timestamp, since the same-day rule is evaluated in the device time zone. */
    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getDefault()).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    // ---------------------------------------------------------- same-day override

    @Test
    fun `logging the same exercise twice in one day keeps only the later entry`() = runTest {
        val id = benchPressId()

        repository.insertLog(ExerciseLog(exerciseId = id, dateTimestamp = at(2026, 3, 10, 7, 0), weightLbs = 165.0))
        repository.insertLog(ExerciseLog(exerciseId = id, dateTimestamp = at(2026, 3, 10, 18, 30), weightLbs = 185.0))

        val logs = repository.getAllLogsForExercise(id)
        assertEquals(1, logs.size)
        assertEquals(185.0, logs.single().weightLbs!!, 0.001)
    }

    @Test
    fun `logging on different days keeps both entries`() = runTest {
        val id = benchPressId()

        repository.insertLog(ExerciseLog(exerciseId = id, dateTimestamp = at(2026, 3, 10, 7, 0), weightLbs = 165.0))
        repository.insertLog(ExerciseLog(exerciseId = id, dateTimestamp = at(2026, 3, 11, 7, 0), weightLbs = 175.0))

        assertEquals(2, repository.getAllLogsForExercise(id).size)
    }

    @Test
    fun `entries either side of midnight are treated as different days`() = runTest {
        val id = benchPressId()

        repository.insertLog(ExerciseLog(exerciseId = id, dateTimestamp = at(2026, 3, 10, 23, 59), weightLbs = 165.0))
        repository.insertLog(ExerciseLog(exerciseId = id, dateTimestamp = at(2026, 3, 11, 0, 1), weightLbs = 175.0))

        assertEquals(2, repository.getAllLogsForExercise(id).size)
    }

    @Test
    fun `the override only affects the exercise being logged`() = runTest {
        val bench = benchPressId()
        val squat = squatId()
        val sameDay = at(2026, 3, 10, 9, 0)

        repository.insertLog(ExerciseLog(exerciseId = bench, dateTimestamp = sameDay, weightLbs = 165.0))
        repository.insertLog(ExerciseLog(exerciseId = squat, dateTimestamp = sameDay, weightLbs = 225.0))
        repository.insertLog(ExerciseLog(exerciseId = bench, dateTimestamp = sameDay, weightLbs = 185.0))

        assertEquals(1, repository.getAllLogsForExercise(bench).size)
        assertEquals(1, repository.getAllLogsForExercise(squat).size)
        assertEquals(225.0, repository.getAllLogsForExercise(squat).single().weightLbs!!, 0.001)
    }

    @Test
    fun `walking logs also override within the same day`() = runTest {
        repository.insertWalkingLog(
            WalkingLog(dateTimestamp = at(2026, 3, 10, 8, 0), distanceMiles = 2.0, distanceKm = 3.22, durationSeconds = 1800),
        )
        repository.insertWalkingLog(
            WalkingLog(dateTimestamp = at(2026, 3, 10, 19, 0), distanceMiles = 3.5, distanceKm = 5.63, durationSeconds = 3000),
        )

        val walks = repository.getAllWalkingLogsSync()
        assertEquals(1, walks.size)
        assertEquals(3.5, walks.single().distanceMiles, 0.001)
    }

    // ------------------------------------------------- bulk log flow used by Day Detail

    @Test
    fun `getLogsForExercises returns logs across every requested exercise`() = runTest {
        val bench = benchPressId()
        val squat = squatId()
        repository.insertLog(ExerciseLog(exerciseId = bench, dateTimestamp = at(2026, 3, 10, 9, 0), weightLbs = 165.0))
        repository.insertLog(ExerciseLog(exerciseId = squat, dateTimestamp = at(2026, 3, 10, 9, 0), weightLbs = 225.0))

        val logs = repository.getLogsForExercises(listOf(bench, squat)).first()

        assertEquals(2, logs.size)
        assertEquals(setOf(bench, squat), logs.map { it.exerciseId }.toSet())
    }

    @Test
    fun `getLogsForExercises ignores exercises that were not requested`() = runTest {
        val bench = benchPressId()
        val squat = squatId()
        repository.insertLog(ExerciseLog(exerciseId = bench, dateTimestamp = at(2026, 3, 10, 9, 0), weightLbs = 165.0))
        repository.insertLog(ExerciseLog(exerciseId = squat, dateTimestamp = at(2026, 3, 10, 9, 0), weightLbs = 225.0))

        val logs = repository.getLogsForExercises(listOf(bench)).first()

        assertEquals(listOf(bench), logs.map { it.exerciseId })
    }

    /** SQLite allows an empty IN list where most engines do not; pin that down. */
    @Test
    fun `getLogsForExercises with no ids returns empty instead of failing`() = runTest {
        val logs = repository.getLogsForExercises(emptyList()).first()

        assertTrue(logs.isEmpty())
    }

    @Test
    fun `the day detail flow re-emits when a log is written`() = runTest {
        val bench = benchPressId()

        assertTrue(repository.getLogsForExercises(listOf(bench)).first().isEmpty())

        repository.insertLog(ExerciseLog(exerciseId = bench, dateTimestamp = at(2026, 3, 10, 9, 0), weightLbs = 165.0))

        assertEquals(1, repository.getLogsForExercises(listOf(bench)).first().size)
    }

    // ------------------------------------------------------------------- deletion

    @Test
    fun `deleting a log removes it`() = runTest {
        val id = benchPressId()
        repository.insertLog(ExerciseLog(exerciseId = id, dateTimestamp = at(2026, 3, 10, 9, 0), weightLbs = 165.0))
        val log = repository.getAllLogsForExercise(id).single()

        repository.deleteLog(log)

        assertTrue(repository.getAllLogsForExercise(id).isEmpty())
    }

    @Test
    fun `getLatestLogForExercise returns the most recent entry`() = runTest {
        val id = benchPressId()
        repository.insertLog(ExerciseLog(exerciseId = id, dateTimestamp = at(2026, 3, 9, 9, 0), weightLbs = 165.0))
        repository.insertLog(ExerciseLog(exerciseId = id, dateTimestamp = at(2026, 3, 11, 9, 0), weightLbs = 185.0))
        repository.insertLog(ExerciseLog(exerciseId = id, dateTimestamp = at(2026, 3, 10, 9, 0), weightLbs = 175.0))

        assertEquals(185.0, repository.getLatestLogForExercise(id)!!.weightLbs!!, 0.001)
    }
}
