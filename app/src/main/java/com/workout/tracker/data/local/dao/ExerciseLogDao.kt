package com.workout.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.workout.tracker.data.local.entity.ExerciseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseLogDao {

    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId ORDER BY dateTimestamp ASC")
    fun getForExercise(exerciseId: Long): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_logs WHERE exerciseId IN (:exerciseIds) ORDER BY dateTimestamp ASC")
    fun getForExercises(exerciseIds: List<Long>): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId ORDER BY dateTimestamp ASC")
    suspend fun getAllForExercise(exerciseId: Long): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId AND dateTimestamp >= :since ORDER BY dateTimestamp ASC")
    suspend fun getForExerciseSince(exerciseId: Long, since: Long): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId AND dateTimestamp < :before ORDER BY dateTimestamp DESC LIMIT 1")
    suspend fun getLatestForExerciseBefore(exerciseId: Long, before: Long): ExerciseLogEntity?

    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId ORDER BY dateTimestamp DESC LIMIT 1")
    suspend fun getLatestForExercise(exerciseId: Long): ExerciseLogEntity?

    @Query("SELECT * FROM exercise_logs ORDER BY dateTimestamp ASC")
    suspend fun getAll(): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_logs ORDER BY dateTimestamp ASC")
    fun getAllFlow(): Flow<List<ExerciseLogEntity>>

    @Query("DELETE FROM exercise_logs")
    suspend fun clearAll()

    @Query("DELETE FROM exercise_logs WHERE exerciseId = :exerciseId AND dateTimestamp >= :dayStart AND dateTimestamp < :dayEnd")
    suspend fun deleteForExerciseOnDay(exerciseId: Long, dayStart: Long, dayEnd: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ExerciseLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<ExerciseLogEntity>)

    @Delete
    suspend fun delete(log: ExerciseLogEntity)
}
