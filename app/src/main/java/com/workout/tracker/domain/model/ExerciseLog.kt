package com.workout.tracker.domain.model

data class ExerciseLog(
    val id: Long = 0L,
    val exerciseId: Long,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val weightLbs: Double? = null,
    val setsCompleted: Int? = null,
    val repsPerSet: Int? = null,
    val durationSeconds: Long? = null,
    val distanceMiles: Double? = null,
    val difficulty: String? = null,
    val notes: String? = null,
)
