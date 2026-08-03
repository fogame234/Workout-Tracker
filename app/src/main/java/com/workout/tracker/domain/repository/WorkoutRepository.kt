package com.workout.tracker.domain.repository

import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.WalkingLog
import com.workout.tracker.domain.model.WorkoutDay
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {

    fun getAllWorkoutDays(): Flow<List<WorkoutDay>>

    fun getExercisesForDay(dayId: Long): Flow<List<Exercise>>

    suspend fun getAllExercises(): List<Exercise>

    suspend fun getExerciseById(id: Long): Exercise?

    fun getLogsForExercise(exerciseId: Long): Flow<List<ExerciseLog>>

    suspend fun getAllLogsForExercise(exerciseId: Long): List<ExerciseLog>

    suspend fun getLogsForExerciseSince(exerciseId: Long, since: Long): List<ExerciseLog>

    suspend fun getLatestLogForExerciseBefore(exerciseId: Long, before: Long): ExerciseLog?

    suspend fun getLatestLogForExercise(exerciseId: Long): ExerciseLog?

    suspend fun insertLog(log: ExerciseLog): Long

    suspend fun deleteLog(log: ExerciseLog)

    // Walking
    fun getAllWalkingLogs(): Flow<List<WalkingLog>>
    suspend fun getAllWalkingLogsSync(): List<WalkingLog>
    suspend fun getLatestWalkingLog(): WalkingLog?
    suspend fun getLatestWalkingLogBefore(before: Long): WalkingLog?
    suspend fun insertWalkingLog(log: WalkingLog): Long
    suspend fun deleteWalkingLog(log: WalkingLog)
}
