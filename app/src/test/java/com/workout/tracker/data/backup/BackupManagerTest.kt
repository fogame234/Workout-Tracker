package com.workout.tracker.data.backup

import com.workout.tracker.data.local.entity.ExerciseLogEntity
import com.workout.tracker.data.local.entity.WalkingLogEntity
import com.workout.tracker.data.local.entity.WorkoutDayEntity
import com.workout.tracker.testing.DatabaseTest
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** On any bad input the database must be left completely unchanged. */
@RunWith(RobolectricTestRunner::class)
class BackupManagerTest : DatabaseTest() {

    private val backupManager by lazy {
        BackupManager(db, dayDao, exerciseDao, logDao, walkingDao)
    }

    private data class Snapshot(
        val days: List<WorkoutDayEntity>,
        val exercises: List<Any>,
        val logs: List<ExerciseLogEntity>,
        val walks: List<WalkingLogEntity>,
    )

    private suspend fun snapshot() = Snapshot(
        days = dayDao.getAllSync(),
        exercises = exerciseDao.getAll(),
        logs = logDao.getAll(),
        walks = walkingDao.getAllSync(),
    )

    private suspend fun seedUserData() {
        val exercises = exerciseDao.getAll()
        logDao.insert(
            ExerciseLogEntity(
                exerciseId = exercises.first { it.name == "Bench Press" }.id,
                dateTimestamp = 1_700_000_000_000L,
                weightLbs = 185.5,
                setsCompleted = 4,
                repsPerSet = 8,
                durationSeconds = null,
                distanceMiles = null,
                difficulty = null,
                notes = "felt strong",
            ),
        )
        logDao.insert(
            ExerciseLogEntity(
                exerciseId = exercises.first { it.name == "Plank" }.id,
                dateTimestamp = 1_700_100_000_000L,
                weightLbs = null,
                setsCompleted = 3,
                repsPerSet = null,
                durationSeconds = 75L,
                distanceMiles = null,
                difficulty = "HARD",
                notes = null,
            ),
        )
        walkingDao.insert(
            WalkingLogEntity(
                dateTimestamp = 1_700_200_000_000L,
                distanceMiles = 3.1,
                distanceKm = 4.99,
                durationSeconds = 2_700L,
                notes = null,
            ),
        )
    }

    // ---------------------------------------------------------------- round trip

    @Test
    fun `export then import restores every row exactly`() = runTest {
        seedUserData()
        val original = snapshot()
        val json = backupManager.exportToJson()

        // Scribble over the database so a no-op import would be obvious.
        logDao.clearAll()
        walkingDao.clearAll()
        assertNotEquals(original, snapshot())

        val result = backupManager.importFromJson(json)

        assertTrue("import failed: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(original, snapshot())
    }

    @Test
    fun `round trip preserves null optional fields`() = runTest {
        seedUserData()
        val json = backupManager.exportToJson()
        logDao.clearAll()

        backupManager.importFromJson(json)

        val plank = logDao.getAll().first { it.difficulty == "HARD" }
        assertEquals(null, plank.weightLbs)
        assertEquals(null, plank.repsPerSet)
        assertEquals(null, plank.distanceMiles)
        assertEquals(null, plank.notes)
        assertEquals(75L, plank.durationSeconds)
    }

    @Test
    fun `import reports the number of restored entries`() = runTest {
        seedUserData()
        val json = backupManager.exportToJson()

        val result = backupManager.importFromJson(json)

        // Two exercise logs plus one walking log.
        assertEquals(3, result.getOrNull())
    }

    // ------------------------------------------------- bad input must not destroy data

    @Test
    fun `import of json that is not a backup leaves the database untouched`() = runTest {
        seedUserData()
        val before = snapshot()

        val result = backupManager.importFromJson("""{"hello":"world"}""")

        assertTrue("a non-backup file should be rejected", result.isFailure)
        assertEquals(before, snapshot())
    }

    @Test
    fun `import of malformed json leaves the database untouched`() = runTest {
        seedUserData()
        val before = snapshot()

        val result = backupManager.importFromJson("this is definitely not json {{{")

        assertTrue(result.isFailure)
        assertEquals(before, snapshot())
    }

    @Test
    fun `import of an empty file leaves the database untouched`() = runTest {
        seedUserData()
        val before = snapshot()

        val result = backupManager.importFromJson("")

        assertTrue(result.isFailure)
        assertEquals(before, snapshot())
    }

    @Test
    fun `import of a truncated backup leaves the database untouched`() = runTest {
        seedUserData()
        val before = snapshot()
        val json = backupManager.exportToJson()

        val result = backupManager.importFromJson(json.substring(0, json.length / 2))

        assertTrue(result.isFailure)
        assertEquals(before, snapshot())
    }

    @Test
    fun `import of a backup missing a section leaves the database untouched`() = runTest {
        seedUserData()
        val before = snapshot()
        val json = JSONObject(backupManager.exportToJson())
            .apply { remove("walkingLogs") }
            .toString()

        val result = backupManager.importFromJson(json)

        assertTrue(result.isFailure)
        assertEquals(before, snapshot())
    }

    @Test
    fun `import from a newer format version is rejected`() = runTest {
        seedUserData()
        val before = snapshot()
        val json = JSONObject(backupManager.exportToJson())
            .apply { put("version", 99) }
            .toString()

        val result = backupManager.importFromJson(json)

        assertTrue(result.isFailure)
        assertEquals(before, snapshot())
    }

    @Test
    fun `import with an unknown exercise type is rejected`() = runTest {
        seedUserData()
        val before = snapshot()
        val root = JSONObject(backupManager.exportToJson())
        root.getJSONArray("exercises").getJSONObject(0).put("exerciseType", "INTERPRETIVE_DANCE")

        val result = backupManager.importFromJson(root.toString())

        assertTrue(result.isFailure)
        assertEquals(before, snapshot())
    }

    @Test
    fun `import with a log pointing at a missing exercise is rejected`() = runTest {
        seedUserData()
        val before = snapshot()
        val root = JSONObject(backupManager.exportToJson())
        root.getJSONArray("exerciseLogs").getJSONObject(0).put("exerciseId", 987_654L)

        val result = backupManager.importFromJson(root.toString())

        assertTrue("a dangling foreign key should be rejected", result.isFailure)
        assertEquals(before, snapshot())
    }

    @Test
    fun `import with an exercise pointing at a missing day is rejected`() = runTest {
        seedUserData()
        val before = snapshot()
        val root = JSONObject(backupManager.exportToJson())
        root.getJSONArray("exercises").getJSONObject(0).put("workoutDayId", 987_654L)

        val result = backupManager.importFromJson(root.toString())

        assertTrue(result.isFailure)
        assertEquals(before, snapshot())
    }

    @Test
    fun `import with a wrongly typed field is rejected`() = runTest {
        seedUserData()
        val before = snapshot()
        val root = JSONObject(backupManager.exportToJson())
        root.getJSONArray("exerciseLogs").getJSONObject(0).put("dateTimestamp", "yesterday")

        val result = backupManager.importFromJson(root.toString())

        assertTrue(result.isFailure)
        assertEquals(before, snapshot())
    }

    // ------------------------------------------------------------- parse vs restore

    @Test
    fun `parse alone never touches the database`() = runTest {
        seedUserData()
        val before = snapshot()
        val json = backupManager.exportToJson()

        val bundle = backupManager.parse(json)

        assertEquals(before, snapshot())
        assertEquals(before.days.size, bundle.days.size)
        assertEquals(before.exercises.size, bundle.exercises.size)
        assertEquals(2, bundle.exerciseLogs.size)
        assertEquals(1, bundle.walkingLogs.size)
        assertEquals(3, bundle.entryCount)
    }

    @Test
    fun `parse surfaces the counts used by the confirmation dialog`() = runTest {
        seedUserData()

        val bundle = backupManager.parse(backupManager.exportToJson())

        assertEquals(1, bundle.version)
        assertTrue("exportDate should be populated", bundle.exportDate > 0L)
    }
}
