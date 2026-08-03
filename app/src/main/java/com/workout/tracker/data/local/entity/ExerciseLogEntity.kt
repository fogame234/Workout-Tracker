package com.workout.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.workout.tracker.domain.model.ExerciseLog

@Entity(
    tableName = "exercise_logs",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["exerciseId"])],
)
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "exerciseId")
    val exerciseId: Long,
    val dateTimestamp: Long,
    val weightLbs: Double?,
    val setsCompleted: Int?,
    val repsPerSet: Int?,
    val durationSeconds: Long?,
    val distanceMiles: Double?,
    val difficulty: String?,
    val notes: String?,
) {
    fun toDomain(): ExerciseLog = ExerciseLog(
        id = id,
        exerciseId = exerciseId,
        dateTimestamp = dateTimestamp,
        weightLbs = weightLbs,
        setsCompleted = setsCompleted,
        repsPerSet = repsPerSet,
        durationSeconds = durationSeconds,
        distanceMiles = distanceMiles,
        difficulty = difficulty,
        notes = notes,
    )

    companion object {
        fun fromDomain(log: ExerciseLog): ExerciseLogEntity = ExerciseLogEntity(
            id = log.id,
            exerciseId = log.exerciseId,
            dateTimestamp = log.dateTimestamp,
            weightLbs = log.weightLbs,
            setsCompleted = log.setsCompleted,
            repsPerSet = log.repsPerSet,
            durationSeconds = log.durationSeconds,
            distanceMiles = log.distanceMiles,
            difficulty = log.difficulty,
            notes = log.notes,
        )
    }
}
