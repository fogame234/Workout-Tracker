package com.workout.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.workout.tracker.data.local.dao.ExerciseDao
import com.workout.tracker.data.local.dao.ExerciseLogDao
import com.workout.tracker.data.local.dao.WalkingLogDao
import com.workout.tracker.data.local.dao.WorkoutDayDao
import com.workout.tracker.data.local.entity.ExerciseEntity
import com.workout.tracker.data.local.entity.ExerciseLogEntity
import com.workout.tracker.data.local.entity.WalkingLogEntity
import com.workout.tracker.data.local.entity.WorkoutDayEntity

@Database(
    entities = [
        WorkoutDayEntity::class,
        ExerciseEntity::class,
        ExerciseLogEntity::class,
        WalkingLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDayDao(): WorkoutDayDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun walkingLogDao(): WalkingLogDao
}
