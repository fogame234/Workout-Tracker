package com.workout.tracker.data.settings

import android.content.Context
import androidx.core.content.edit
import com.workout.tracker.domain.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsAppSettings @Inject constructor(
    @ApplicationContext context: Context,
) : AppSettings {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val stored = MutableStateFlow(
        prefs.getLong(KEY_LAST_BACKUP_AT, 0L).takeIf { it > 0L },
    )
    override val lastBackupAt: Flow<Long?> = stored.asStateFlow()

    override suspend fun setLastBackupAt(timestamp: Long) {
        prefs.edit { putLong(KEY_LAST_BACKUP_AT, timestamp) }
        stored.value = timestamp
    }

    private companion object {
        const val FILE = "workout_tracker_settings"
        const val KEY_LAST_BACKUP_AT = "last_backup_at"
    }
}
