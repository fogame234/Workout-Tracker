package com.workout.tracker.testing

import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.WalkingLog
import com.workout.tracker.domain.model.WorkoutDay
import com.workout.tracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory repository for ViewModel tests, including the same-day override rule. */
class FakeWorkoutRepository(
    days: List<WorkoutDay> = emptyList(),
    exercises: List<Exercise> = emptyList(),
) : WorkoutRepository {

    private val daysFlow = MutableStateFlow(days)
    private val exercisesFlow = MutableStateFlow(exercises)
    private val logsFlow = MutableStateFlow<List<ExerciseLog>>(emptyList())
    private val walkingFlow = MutableStateFlow<List<WalkingLog>>(emptyList())

    // Start high so generated ids can never collide with ids a test seeds by hand.
    private var nextLogId = 1_000L
    private var nextWalkId = 1_000L

    fun setExercises(exercises: List<Exercise>) { exercisesFlow.value = exercises }

    fun setLogs(logs: List<ExerciseLog>) { logsFlow.value = logs }

    fun setWalkingLogs(logs: List<WalkingLog>) { walkingFlow.value = logs }

    val logs: List<ExerciseLog> get() = logsFlow.value
    val walkingLogs: List<WalkingLog> get() = walkingFlow.value

    override fun getAllWorkoutDays(): Flow<List<WorkoutDay>> = daysFlow

    override fun getExercisesForDay(dayId: Long): Flow<List<Exercise>> =
        exercisesFlow.map { all -> all.filter { it.workoutDayId == dayId } }

    override suspend fun getAllExercises(): List<Exercise> = exercisesFlow.value

    override fun getAllExercisesFlow(): Flow<List<Exercise>> = exercisesFlow

    override suspend fun getExerciseById(id: Long): Exercise? =
        exercisesFlow.value.firstOrNull { it.id == id }

    override fun getLogsForExercise(exerciseId: Long): Flow<List<ExerciseLog>> =
        logsFlow.map { all -> all.filter { it.exerciseId == exerciseId }.sortedBy { it.dateTimestamp } }

    override fun getLogsForExercises(exerciseIds: List<Long>): Flow<List<ExerciseLog>> =
        logsFlow.map { all -> all.filter { it.exerciseId in exerciseIds }.sortedBy { it.dateTimestamp } }

    override fun getAllLogsFlow(): Flow<List<ExerciseLog>> =
        logsFlow.map { all -> all.sortedBy { it.dateTimestamp } }

    override suspend fun getAllLogsForExercise(exerciseId: Long): List<ExerciseLog> =
        logsFlow.value.filter { it.exerciseId == exerciseId }.sortedBy { it.dateTimestamp }

    override suspend fun getLogsForExerciseSince(exerciseId: Long, since: Long): List<ExerciseLog> =
        getAllLogsForExercise(exerciseId).filter { it.dateTimestamp >= since }

    override suspend fun getLatestLogForExerciseBefore(exerciseId: Long, before: Long): ExerciseLog? =
        getAllLogsForExercise(exerciseId).lastOrNull { it.dateTimestamp < before }

    override suspend fun getLatestLogForExercise(exerciseId: Long): ExerciseLog? =
        getAllLogsForExercise(exerciseId).lastOrNull()

    override suspend fun insertLog(log: ExerciseLog): Long {
        val id = nextLogId++
        val dayBucket = log.dateTimestamp / DAY_MILLIS
        logsFlow.value = logsFlow.value
            .filterNot { it.exerciseId == log.exerciseId && it.dateTimestamp / DAY_MILLIS == dayBucket }
            .plus(log.copy(id = id))
        return id
    }

    override suspend fun deleteLog(log: ExerciseLog) {
        logsFlow.value = logsFlow.value.filterNot { it.id == log.id }
    }

    override fun getAllWalkingLogs(): Flow<List<WalkingLog>> =
        walkingFlow.map { all -> all.sortedBy { it.dateTimestamp } }

    override suspend fun getAllWalkingLogsSync(): List<WalkingLog> =
        walkingFlow.value.sortedBy { it.dateTimestamp }

    override suspend fun getLatestWalkingLog(): WalkingLog? = getAllWalkingLogsSync().lastOrNull()

    override suspend fun getLatestWalkingLogBefore(before: Long): WalkingLog? =
        getAllWalkingLogsSync().lastOrNull { it.dateTimestamp < before }

    override suspend fun insertWalkingLog(log: WalkingLog): Long {
        val id = nextWalkId++
        val dayBucket = log.dateTimestamp / DAY_MILLIS
        walkingFlow.value = walkingFlow.value
            .filterNot { it.dateTimestamp / DAY_MILLIS == dayBucket }
            .plus(log.copy(id = id))
        return id
    }

    override suspend fun deleteWalkingLog(log: WalkingLog) {
        walkingFlow.value = walkingFlow.value.filterNot { it.id == log.id }
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
