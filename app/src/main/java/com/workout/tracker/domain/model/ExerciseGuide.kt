package com.workout.tracker.domain.model

/** Reference info for an exercise. Matched by name, so one guide covers every day it appears on. */
data class ExerciseGuide(
    val summary: String,
    val steps: List<String>,
    val tips: List<String> = emptyList(),
    val musclesWorked: String,
)
