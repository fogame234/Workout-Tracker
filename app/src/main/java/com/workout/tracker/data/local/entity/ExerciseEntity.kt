package com.workout.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseType

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["workoutDayId"])],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "workoutDayId")
    val workoutDayId: Long,
    val name: String,
    val category: String,
    val exerciseType: String,
    val defaultSets: Int?,
    val defaultReps: String?,
    val defaultDurationSeconds: Int?,
    val orderIndex: Int,
) {
    fun toDomain(): Exercise = Exercise(
        id = id,
        workoutDayId = workoutDayId,
        name = name,
        category = category,
        exerciseType = ExerciseType.valueOf(exerciseType),
        defaultSets = defaultSets,
        defaultReps = defaultReps,
        defaultDurationSeconds = defaultDurationSeconds,
        orderIndex = orderIndex,
    )
}
