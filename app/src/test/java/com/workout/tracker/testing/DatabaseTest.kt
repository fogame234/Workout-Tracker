package com.workout.tracker.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.workout.tracker.data.local.DatabaseSeeder
import com.workout.tracker.data.local.WorkoutDatabase
import org.junit.After
import org.junit.Before

/** In-memory Room database with the production seeder, for tests that need real SQL. */
abstract class DatabaseTest {

    protected lateinit var db: WorkoutDatabase

    protected val dayDao get() = db.workoutDayDao()
    protected val exerciseDao get() = db.exerciseDao()
    protected val logDao get() = db.exerciseLogDao()
    protected val walkingDao get() = db.walkingLogDao()

    @Before
    fun createDatabase() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .addCallback(DatabaseSeeder())
            .build()
    }

    @After
    fun closeDatabase() {
        db.close()
    }
}
