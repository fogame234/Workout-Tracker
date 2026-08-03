package com.workout.tracker.data.backup

import com.workout.tracker.data.local.dao.ExerciseDao
import com.workout.tracker.data.local.dao.ExerciseLogDao
import com.workout.tracker.data.local.dao.WalkingLogDao
import com.workout.tracker.data.local.dao.WorkoutDayDao
import com.workout.tracker.data.local.entity.ExerciseEntity
import com.workout.tracker.data.local.entity.ExerciseLogEntity
import com.workout.tracker.data.local.entity.WalkingLogEntity
import com.workout.tracker.data.local.entity.WorkoutDayEntity
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val dayDao: WorkoutDayDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val walkingLogDao: WalkingLogDao,
) {

    suspend fun exportToJson(): String {
        val root = JSONObject()
        root.put("version", 1)
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

    suspend fun importFromJson(json: String): Result<Int> = try {
        val root = JSONObject(json)
        val version = root.optInt("version", 1)

        // Clear all tables (exercise_logs and walking_logs first due to foreign keys)
        exerciseLogDao.clearAll()
        walkingLogDao.clearAll()
        exerciseDao.clearAll()
        dayDao.clearAll()

        // Import workout days
        val days = root.getJSONArray("workoutDays")
        val dayEntities = (0 until days.length()).map { i ->
            val d = days.getJSONObject(i)
            WorkoutDayEntity(
                id = d.getLong("id"),
                dayNumber = d.getInt("dayNumber"),
                title = d.getString("title"),
                subtitle = d.getString("subtitle"),
            )
        }
        dayDao.insertAll(dayEntities)

        // Import exercises
        val exercises = root.getJSONArray("exercises")
        val exerciseEntities = (0 until exercises.length()).map { i ->
            val e = exercises.getJSONObject(i)
            ExerciseEntity(
                id = e.getLong("id"),
                workoutDayId = e.getLong("workoutDayId"),
                name = e.getString("name"),
                category = e.getString("category"),
                exerciseType = e.getString("exerciseType"),
                defaultSets = e.optNullInt("defaultSets"),
                defaultReps = e.optNullString("defaultReps"),
                defaultDurationSeconds = e.optNullInt("defaultDurationSeconds"),
                orderIndex = e.getInt("orderIndex"),
            )
        }
        exerciseDao.insertAll(exerciseEntities)

        // Import exercise logs
        val logs = root.getJSONArray("exerciseLogs")
        var count = 0
        for (i in 0 until logs.length()) {
            val l = logs.getJSONObject(i)
            exerciseLogDao.insert(
                ExerciseLogEntity(
                    id = l.getLong("id"),
                    exerciseId = l.getLong("exerciseId"),
                    dateTimestamp = l.getLong("dateTimestamp"),
                    weightLbs = l.optNullDouble("weightLbs"),
                    setsCompleted = l.optNullInt("setsCompleted"),
                    repsPerSet = l.optNullInt("repsPerSet"),
                    durationSeconds = l.optNullLong("durationSeconds"),
                    distanceMiles = l.optNullDouble("distanceMiles"),
                    difficulty = l.optNullString("difficulty"),
                    notes = l.optNullString("notes"),
                ),
            )
            count++
        }

        // Import walking logs
        val walks = root.getJSONArray("walkingLogs")
        val walkEntities = (0 until walks.length()).map { i ->
            val w = walks.getJSONObject(i)
            WalkingLogEntity(
                id = w.getLong("id"),
                dateTimestamp = w.getLong("dateTimestamp"),
                distanceMiles = w.getDouble("distanceMiles"),
                distanceKm = w.getDouble("distanceKm"),
                durationSeconds = w.getLong("durationSeconds"),
                notes = w.optNullString("notes"),
            )
        }
        walkingLogDao.insertAll(walkEntities)
        count += walkEntities.size

        Result.success(count)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Extension helpers for nullable JSON fields
    private fun JSONObject.optNullInt(key: String): Int? =
        if (isNull(key)) null else optInt(key)

    private fun JSONObject.optNullLong(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private fun JSONObject.optNullDouble(key: String): Double? =
        if (isNull(key)) null else optDouble(key)

    private fun JSONObject.optNullString(key: String): String? =
        if (isNull(key)) null else optString(key)
}
