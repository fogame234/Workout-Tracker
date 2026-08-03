package com.workout.tracker.data.repository

import com.workout.tracker.data.local.dao.ExerciseDao
import com.workout.tracker.data.local.dao.ExerciseLogDao
import com.workout.tracker.data.local.dao.WalkingLogDao
import com.workout.tracker.data.local.dao.WorkoutDayDao
import com.workout.tracker.data.local.entity.ExerciseLogEntity
import com.workout.tracker.data.local.entity.WalkingLogEntity
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.WalkingLog
import com.workout.tracker.domain.model.WorkoutDay
import com.workout.tracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val dayDao: WorkoutDayDao,
    private val exerciseDao: ExerciseDao,
    private val logDao: ExerciseLogDao,
    private val walkingLogDao: WalkingLogDao,
) : WorkoutRepository {

    override fun getAllWorkoutDays(): Flow<List<WorkoutDay>> =
        dayDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getExercisesForDay(dayId: Long): Flow<List<Exercise>> =
        exerciseDao.getForDay(dayId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getExerciseById(id: Long): Exercise? =
        exerciseDao.getById(id)?.toDomain()

    override suspend fun getAllExercises(): List<Exercise> =
        exerciseDao.getAll().map { it.toDomain() }

    override fun getLogsForExercise(exerciseId: Long): Flow<List<ExerciseLog>> =
        logDao.getForExercise(exerciseId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAllLogsForExercise(exerciseId: Long): List<ExerciseLog> =
        logDao.getAllForExercise(exerciseId).map { it.toDomain() }

    override suspend fun getLogsForExerciseSince(exerciseId: Long, since: Long): List<ExerciseLog> =
        logDao.getForExerciseSince(exerciseId, since).map { it.toDomain() }

    override suspend fun getLatestLogForExerciseBefore(exerciseId: Long, before: Long): ExerciseLog? =
        logDao.getLatestForExerciseBefore(exerciseId, before)?.toDomain()

    override suspend fun getLatestLogForExercise(exerciseId: Long): ExerciseLog? =
        logDao.getLatestForExercise(exerciseId)?.toDomain()

    override suspend fun insertLog(log: ExerciseLog): Long {
        // Delete any existing entry for this exercise on the same calendar day
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = log.dateTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val dayEnd = cal.timeInMillis

        logDao.deleteForExerciseOnDay(log.exerciseId, dayStart, dayEnd)
        return logDao.insert(ExerciseLogEntity.fromDomain(log))
    }

    override suspend fun deleteLog(log: ExerciseLog) =
        logDao.delete(ExerciseLogEntity.fromDomain(log))

    // Walking

    override fun getAllWalkingLogs(): Flow<List<WalkingLog>> =
        walkingLogDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAllWalkingLogsSync(): List<WalkingLog> =
        walkingLogDao.getAllSync().map { it.toDomain() }

    override suspend fun getLatestWalkingLog(): WalkingLog? =
        walkingLogDao.getLatest()?.toDomain()

    override suspend fun getLatestWalkingLogBefore(before: Long): WalkingLog? =
        walkingLogDao.getLatestBefore(before)?.toDomain()

    override suspend fun insertWalkingLog(log: WalkingLog): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = log.dateTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val dayEnd = cal.timeInMillis

        walkingLogDao.deleteForDay(dayStart, dayEnd)
        return walkingLogDao.insert(WalkingLogEntity.fromDomain(log))
    }

    override suspend fun deleteWalkingLog(log: WalkingLog) =
        walkingLogDao.delete(WalkingLogEntity.fromDomain(log))
}
