package com.workout.tracker.data.backup

import androidx.room.withTransaction
import com.workout.tracker.data.local.WorkoutDatabase
import com.workout.tracker.data.local.dao.ExerciseDao
import com.workout.tracker.data.local.dao.ExerciseLogDao
import com.workout.tracker.data.local.dao.WalkingLogDao
import com.workout.tracker.data.local.dao.WorkoutDayDao
import com.workout.tracker.data.local.entity.ExerciseEntity
import com.workout.tracker.data.local.entity.ExerciseLogEntity
import com.workout.tracker.data.local.entity.WalkingLogEntity
import com.workout.tracker.data.local.entity.WorkoutDayEntity
import com.workout.tracker.domain.model.ExerciseType
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Validated contents of a backup file, parsed before anything is written. */
data class BackupBundle(
    val version: Int,
    val exportDate: Long,
    val days: List<WorkoutDayEntity>,
    val exercises: List<ExerciseEntity>,
    val exerciseLogs: List<ExerciseLogEntity>,
    val walkingLogs: List<WalkingLogEntity>,
) {
    val entryCount: Int get() = exerciseLogs.size + walkingLogs.size
}

@Singleton
class BackupManager @Inject constructor(
    private val db: WorkoutDatabase,
    private val dayDao: WorkoutDayDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val walkingLogDao: WalkingLogDao,
) {

    suspend fun exportToJson(): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportDate", System.currentTimeMillis())

        // Workout days
        val days = JSONArray()
        dayDao.getAllSync().forEach { d ->
            days.put(JSONObject().apply {
                put("id", d.id)
                put("dayNumber", d.dayNumber)
                put("title", d.title)
                put("subtitle", d.subtitle)
            })
        }
        root.put("workoutDays", days)

        // Exercises
        val exercises = JSONArray()
        exerciseDao.getAll().forEach { e ->
            exercises.put(JSONObject().apply {
                put("id", e.id)
                put("workoutDayId", e.workoutDayId)
                put("name", e.name)
                put("category", e.category)
                put("exerciseType", e.exerciseType)
                put("defaultSets", e.defaultSets ?: JSONObject.NULL)
                put("defaultReps", e.defaultReps ?: JSONObject.NULL)
                put("defaultDurationSeconds", e.defaultDurationSeconds ?: JSONObject.NULL)
                put("orderIndex", e.orderIndex)
            })
        }
        root.put("exercises", exercises)

        // Exercise logs
        val logs = JSONArray()
        exerciseLogDao.getAll().forEach { l ->
            logs.put(JSONObject().apply {
                put("id", l.id)
                put("exerciseId", l.exerciseId)
                put("dateTimestamp", l.dateTimestamp)
                put("weightLbs", l.weightLbs ?: JSONObject.NULL)
                put("setsCompleted", l.setsCompleted ?: JSONObject.NULL)
                put("repsPerSet", l.repsPerSet ?: JSONObject.NULL)
                put("durationSeconds", l.durationSeconds ?: JSONObject.NULL)
                put("distanceMiles", l.distanceMiles ?: JSONObject.NULL)
                put("difficulty", l.difficulty ?: JSONObject.NULL)
                put("notes", l.notes ?: JSONObject.NULL)
            })
        }
        root.put("exerciseLogs", logs)

        // Walking logs
        val walks = JSONArray()
        walkingLogDao.getAllSync().forEach { w ->
            walks.put(JSONObject().apply {
                put("id", w.id)
                put("dateTimestamp", w.dateTimestamp)
                put("distanceMiles", w.distanceMiles)
                put("distanceKm", w.distanceKm)
                put("durationSeconds", w.durationSeconds)
                put("notes", w.notes ?: JSONObject.NULL)
            })
        }
        root.put("walkingLogs", walks)

        return root.toString(2)
    }

    /** Parses and validates [json]. Never touches the database. */
    fun parse(json: String): BackupBundle {
        val root = JSONObject(json)

        val version = root.optInt("version", 1)
        require(version in 1..BACKUP_VERSION) {
            "This backup was made by a newer version of the app (format $version)."
        }

        val days = root.getJSONArray("workoutDays").map { d ->
            WorkoutDayEntity(
                id = d.getLong("id"),
                dayNumber = d.getInt("dayNumber"),
                title = d.getString("title"),
                subtitle = d.getString("subtitle"),
            )
        }

        val exercises = root.getJSONArray("exercises").map { e ->
            val type = e.getString("exerciseType")
            require(ExerciseType.entries.any { it.name == type }) {
                "Unknown exercise type: $type"
            }
            ExerciseEntity(
                id = e.getLong("id"),
                workoutDayId = e.getLong("workoutDayId"),
                name = e.getString("name"),
                category = e.getString("category"),
                exerciseType = type,
                defaultSets = e.nullableInt("defaultSets"),
                defaultReps = e.nullableString("defaultReps"),
                defaultDurationSeconds = e.nullableInt("defaultDurationSeconds"),
                orderIndex = e.getInt("orderIndex"),
            )
        }

        val exerciseLogs = root.getJSONArray("exerciseLogs").map { l ->
            ExerciseLogEntity(
                id = l.getLong("id"),
                exerciseId = l.getLong("exerciseId"),
                dateTimestamp = l.getLong("dateTimestamp"),
                weightLbs = l.nullableDouble("weightLbs"),
                setsCompleted = l.nullableInt("setsCompleted"),
                repsPerSet = l.nullableInt("repsPerSet"),
                durationSeconds = l.nullableLong("durationSeconds"),
                distanceMiles = l.nullableDouble("distanceMiles"),
                difficulty = l.nullableString("difficulty"),
                notes = l.nullableString("notes"),
            )
        }

        val walkingLogs = root.getJSONArray("walkingLogs").map { w ->
            WalkingLogEntity(
                id = w.getLong("id"),
                dateTimestamp = w.getLong("dateTimestamp"),
                distanceMiles = w.getDouble("distanceMiles"),
                distanceKm = w.getDouble("distanceKm"),
                durationSeconds = w.getLong("durationSeconds"),
                notes = w.nullableString("notes"),
            )
        }

        // Checked here so restore cannot fail part-way through.
        val dayIds = days.mapTo(HashSet()) { it.id }
        exercises.firstOrNull { it.workoutDayId !in dayIds }?.let {
            throw IllegalArgumentException("Exercise ${it.name} refers to a missing workout day.")
        }
        val exerciseIds = exercises.mapTo(HashSet()) { it.id }
        exerciseLogs.firstOrNull { it.exerciseId !in exerciseIds }?.let {
            throw IllegalArgumentException("A logged entry refers to a missing exercise.")
        }

        return BackupBundle(
            version = version,
            exportDate = root.optLong("exportDate", 0L),
            days = days,
            exercises = exercises,
            exerciseLogs = exerciseLogs,
            walkingLogs = walkingLogs,
        )
    }

    /** Replaces all stored data with [bundle] in one transaction. */
    suspend fun restore(bundle: BackupBundle): Int = db.withTransaction {
        // Children first, to respect the foreign keys.
        exerciseLogDao.clearAll()
        walkingLogDao.clearAll()
        exerciseDao.clearAll()
        dayDao.clearAll()

        dayDao.insertAll(bundle.days)
        exerciseDao.insertAll(bundle.exercises)
        exerciseLogDao.insertAll(bundle.exerciseLogs)
        walkingLogDao.insertAll(bundle.walkingLogs)

        bundle.entryCount
    }

    suspend fun importFromJson(json: String): Result<Int> = runCatching { restore(parse(json)) }

    private inline fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    // Extension helpers for nullable JSON fields
    private fun JSONObject.nullableInt(key: String): Int? = if (isNull(key)) null else getInt(key)

    private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)

    private fun JSONObject.nullableDouble(key: String): Double? =
        if (isNull(key)) null else getDouble(key)

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else getString(key)

    companion object {
        /** Highest backup format this build can read. Bump when the shape changes. */
        const val BACKUP_VERSION = 1
    }
}
