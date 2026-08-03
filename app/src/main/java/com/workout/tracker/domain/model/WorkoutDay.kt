package com.workout.tracker.domain.model

data class WorkoutDay(
    val id: Long = 0L,
    val dayNumber: Int,
    val title: String,
    val subtitle: String,
)
