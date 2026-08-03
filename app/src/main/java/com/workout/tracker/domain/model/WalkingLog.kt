package com.workout.tracker.domain.model

data class WalkingLog(
    val id: Long = 0L,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val distanceMiles: Double,
    val distanceKm: Double,
    val durationSeconds: Long,
    val notes: String? = null,
)
