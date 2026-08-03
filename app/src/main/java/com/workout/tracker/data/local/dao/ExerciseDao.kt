package com.workout.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.workout.tracker.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE workoutDayId = :dayId ORDER BY orderIndex ASC")
    fun getForDay(dayId: Long): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY workoutDayId ASC, orderIndex ASC")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("DELETE FROM exercises")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)
}
