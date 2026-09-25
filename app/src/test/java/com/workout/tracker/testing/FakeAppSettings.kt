package com.workout.tracker.testing

import com.workout.tracker.domain.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAppSettings(initialLastBackupAt: Long? = null) : AppSettings {

    private val stored = MutableStateFlow(initialLastBackupAt)
    override val lastBackupAt: Flow<Long?> = stored.asStateFlow()

    val recorded: Long? get() = stored.value

    override suspend fun setLastBackupAt(timestamp: Long) {
        stored.value = timestamp
    }
}
