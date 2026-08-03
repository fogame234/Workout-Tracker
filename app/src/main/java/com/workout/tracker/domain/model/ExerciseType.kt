package com.workout.tracker.domain.model

enum class ExerciseType {
    /** Weight + Sets + Reps (bench press, squat, etc.) */
    WEIGHTED,
    /** Duration + Sets + Optional Weight (plank, farmer carries) */
    TIMED,
    /** Distance + Duration (running, walking) */
    CARDIO,
}
