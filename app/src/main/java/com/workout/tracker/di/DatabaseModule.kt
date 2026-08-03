package com.workout.tracker.di

import android.content.Context
import androidx.room.Room
import com.workout.tracker.data.local.DatabaseSeeder
import com.workout.tracker.data.local.WorkoutDatabase
import com.workout.tracker.data.local.dao.ExerciseDao
import com.workout.tracker.data.local.dao.ExerciseLogDao
import com.workout.tracker.data.local.dao.WalkingLogDao
import com.workout.tracker.data.local.dao.WorkoutDayDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutDatabase =
        Room.databaseBuilder(
            context,
            WorkoutDatabase::class.java,
            "workout_tracker.db",
        )
            .addCallback(DatabaseSeeder())
            .build()

    @Provides
    fun provideWorkoutDayDao(db: WorkoutDatabase): WorkoutDayDao = db.workoutDayDao()

    @Provides
    fun provideExerciseDao(db: WorkoutDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideExerciseLogDao(db: WorkoutDatabase): ExerciseLogDao = db.exerciseLogDao()

    @Provides
    fun provideWalkingLogDao(db: WorkoutDatabase): WalkingLogDao = db.walkingLogDao()
}
