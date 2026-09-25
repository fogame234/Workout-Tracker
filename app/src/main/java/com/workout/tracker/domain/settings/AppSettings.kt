package com.workout.tracker.domain.settings

import kotlinx.coroutines.flow.Flow

/** Small app preferences. Deliberately outside the database so they stay out of backups. */
interface AppSettings {

    /** When the last export was written, or null if there has never been one. */
    val lastBackupAt: Flow<Long?>

    suspend fun setLastBackupAt(timestamp: Long)
}
