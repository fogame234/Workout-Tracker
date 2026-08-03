package com.workout.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.workout.tracker.data.local.entity.WalkingLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkingLogDao {

    @Query("SELECT * FROM walking_logs ORDER BY dateTimestamp ASC")
    fun getAll(): Flow<List<WalkingLogEntity>>

    @Query("SELECT * FROM walking_logs ORDER BY dateTimestamp ASC")
    suspend fun getAllSync(): List<WalkingLogEntity>

    @Query("SELECT * FROM walking_logs ORDER BY dateTimestamp DESC LIMIT 1")
    suspend fun getLatest(): WalkingLogEntity?

    @Query("SELECT * FROM walking_logs WHERE dateTimestamp < :before ORDER BY dateTimestamp DESC LIMIT 1")
    suspend fun getLatestBefore(before: Long): WalkingLogEntity?

    @Query("DELETE FROM walking_logs")
    suspend fun clearAll()

    @Query("DELETE FROM walking_logs WHERE dateTimestamp >= :dayStart AND dateTimestamp < :dayEnd")
    suspend fun deleteForDay(dayStart: Long, dayEnd: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WalkingLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<WalkingLogEntity>)

    @Delete
    suspend fun delete(log: WalkingLogEntity)
}
