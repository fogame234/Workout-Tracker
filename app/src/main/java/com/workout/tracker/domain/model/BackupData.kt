package com.workout.tracker.domain.model

data class BackupData(
    val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val workoutDays: List<WorkoutDay>,
    val exercises: List<Exercise>,
    val exerciseLogs: List<ExerciseLog>,
    val walkingLogs: List<WalkingLog>,
)
