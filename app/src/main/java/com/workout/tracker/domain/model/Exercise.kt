package com.workout.tracker.domain.model

data class Exercise(
    val id: Long = 0L,
    val workoutDayId: Long,
    val name: String,
    val category: String,
    val exerciseType: ExerciseType,
    val defaultSets: Int? = null,
    val defaultReps: String? = null,
    val defaultDurationSeconds: Int? = null,
    val orderIndex: Int = 0,
)
