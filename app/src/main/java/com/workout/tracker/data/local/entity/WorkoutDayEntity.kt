package com.workout.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.workout.tracker.domain.model.WorkoutDay

@Entity(tableName = "workout_days")
data class WorkoutDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dayNumber: Int,
    val title: String,
    val subtitle: String,
) {
    fun toDomain(): WorkoutDay = WorkoutDay(
        id = id,
        dayNumber = dayNumber,
        title = title,
        subtitle = subtitle,
    )
}
