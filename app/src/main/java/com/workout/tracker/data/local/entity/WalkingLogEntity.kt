package com.workout.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.workout.tracker.domain.model.WalkingLog

@Entity(tableName = "walking_logs")
data class WalkingLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateTimestamp: Long,
    val distanceMiles: Double,
    val distanceKm: Double,
    val durationSeconds: Long,
    val notes: String?,
) {
    fun toDomain(): WalkingLog = WalkingLog(
        id = id,
        dateTimestamp = dateTimestamp,
        distanceMiles = distanceMiles,
        distanceKm = distanceKm,
        durationSeconds = durationSeconds,
        notes = notes,
    )

    companion object {
        fun fromDomain(log: WalkingLog): WalkingLogEntity = WalkingLogEntity(
            id = log.id,
            dateTimestamp = log.dateTimestamp,
            distanceMiles = log.distanceMiles,
            distanceKm = log.distanceKm,
            durationSeconds = log.durationSeconds,
            notes = log.notes,
        )
    }
}
