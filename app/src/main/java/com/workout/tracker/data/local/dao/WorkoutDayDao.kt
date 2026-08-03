package com.workout.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.workout.tracker.data.local.entity.WorkoutDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDayDao {

    @Query("SELECT * FROM workout_days ORDER BY dayNumber ASC")
    fun getAll(): Flow<List<WorkoutDayEntity>>

    @Query("SELECT * FROM workout_days ORDER BY dayNumber ASC")
    suspend fun getAllSync(): List<WorkoutDayEntity>

    @Query("DELETE FROM workout_days")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<WorkoutDayEntity>): List<Long>
}
