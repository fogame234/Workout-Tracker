package com.workout.tracker.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.workout.tracker.domain.model.ExerciseType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Seeds the database on first creation with the 4-day workout plan.
 * Uses raw SQL inside the callback because Room DAOs are not available
 * until the database is fully opened.
 */
class DatabaseSeeder : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.beginTransaction()
        try {
            seedDay1(db)
            seedDay2(db)
            seedDay3(db)
            seedDay4(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun seedDay1(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO workout_days (id, dayNumber, title, subtitle) VALUES (1, 1, 'Upper Body Strength', 'Upper Body + Easy Cardio')")
        insertExercise(db, 1, "Bench Press",              "Main",   W, 4, "6-8",    null,  0)
        insertExercise(db, 1, "Bent-Over Barbell Row",     "Main",   W, 4, "8",      null,  1)
        insertExercise(db, 1, "Standing Overhead Press",   "Main",   W, 3, "8-10",   null,  2)
        insertExercise(db, 1, "Pull-ups / Lat Pulldown",   "Main",   W, 3, "8-10",   null,  3)
        insertExercise(db, 1, "Push-ups",                  "Main",   W, 3, "10-15",  null,  4)
        insertExercise(db, 1, "Plank",                     "Core",   T, 3, null,     45,    5)
        insertExercise(db, 1, "Incline Treadmill Walk",    "Cardio", C, null, null,   900,   6)
    }

    private fun seedDay2(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO workout_days (id, dayNumber, title, subtitle) VALUES (2, 2, 'Lower Body Strength', 'Lower Body + Easy Run')")
        insertExercise(db, 2, "Back Squat",         "Main",   W, 4, "6-8",    null,  0)
        insertExercise(db, 2, "Romanian Deadlift",   "Main",   W, 3, "8-10",   null,  1)
        insertExercise(db, 2, "Walking Lunges",      "Main",   W, 3, "10/leg", null,  2)
        insertExercise(db, 2, "Step-ups",            "Main",   W, 3, "10/leg", null,  3)
        insertExercise(db, 2, "Calf Raises",         "Main",   W, 3, "15-20",  null,  4)
        insertExercise(db, 2, "Dead Bugs",           "Core",   W, 3, "10/side",null,  5)
        insertExercise(db, 2, "Easy Run",            "Cardio", C, null, null,   null,  6)
    }

    private fun seedDay3(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO workout_days (id, dayNumber, title, subtitle) VALUES (3, 3, 'Upper Body + Conditioning', 'Upper Body + Military Conditioning')")
        insertExercise(db, 3, "Incline DB Bench Press",  "Main",   W, 3, "8-12",     null,  0)
        insertExercise(db, 3, "Seated Cable Row",        "Main",   W, 3, "10-12",    null,  1)
        insertExercise(db, 3, "Push-ups",                "Main",   W, 3, "to failure",null, 2)
        insertExercise(db, 3, "Face Pulls",              "Main",   W, 3, "15",       null,  3)
        insertExercise(db, 3, "Farmer Carries",          "Main",   T, 3, null,       30,    4)
        insertExercise(db, 3, "Hanging Knee Raises",     "Core",   W, 3, "10-15",    null,  5)
        insertExercise(db, 3, "200m Intervals",          "Cardio", C, null, null,     null,  6)
    }

    private fun seedDay4(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO workout_days (id, dayNumber, title, subtitle) VALUES (4, 4, 'Lower Body + Conditioning', 'Lower Body + Conditioning Circuit')")
        insertExercise(db, 4, "Deadlift",              "Main",         W, 4, "5",       null, 0)
        insertExercise(db, 4, "Bulgarian Split Squat",  "Main",         W, 3, "8-10/leg",null, 1)
        insertExercise(db, 4, "Hip Thrust",             "Main",         W, 3, "10-12",   null, 2)
        insertExercise(db, 4, "Step-ups",               "Main",         W, 3, "12/leg",  null, 3)
        insertExercise(db, 4, "Plank",                  "Core",         T, 3, null,      50,   4)
        insertExercise(db, 4, "Conditioning Circuit",   "Conditioning", T, 4, null,      null, 5)
    }

    private fun insertExercise(
        db: SupportSQLiteDatabase,
        dayId: Long,
        name: String,
        category: String,
        type: String,
        sets: Int?,
        reps: String?,
        durationSec: Int?,
        order: Int,
    ) {
        db.execSQL(
            """INSERT INTO exercises (workoutDayId, name, category, exerciseType, defaultSets, defaultReps, defaultDurationSeconds, orderIndex)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
            arrayOf(dayId, name, category, type, sets, reps, durationSec, order),
        )
    }

    companion object {
        private val W = ExerciseType.WEIGHTED.name
        private val T = ExerciseType.TIMED.name
        private val C = ExerciseType.CARDIO.name
    }
}
