package com.workout.tracker.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.data.backup.BackupBundle
import com.workout.tracker.data.backup.BackupManager
import com.workout.tracker.domain.settings.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.roundToInt

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val exportJson: String? = null,
    /** A parsed backup waiting for the user to confirm the (destructive) restore. */
    val pendingImport: BackupBundle? = null,
    val lastBackupLabel: String = "",
    val lastBackupIsStale: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val backupDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    init {
        viewModelScope.launch {
            appSettings.lastBackupAt.collect { at ->
                _uiState.update {
                    it.copy(
                        lastBackupLabel = describeLastBackup(at),
                        lastBackupIsStale = at == null || daysSince(at) >= STALE_AFTER_DAYS,
                    )
                }
            }
        }
    }

    fun prepareExport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val json = backupManager.exportToJson()
                _uiState.update { it.copy(isExporting = false, exportJson = json) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, message = "Export failed: ${e.message}") }
            }
        }
    }

    fun writeExportTo(uri: Uri) {
        val json = _uiState.value.exportJson ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = context.contentResolver.openOutputStream(uri)
                        ?: error("Could not open the selected file")
                    stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
            }
            if (result.isSuccess) appSettings.setLastBackupAt(System.currentTimeMillis())
            _uiState.update {
                it.copy(
                    exportJson = null,
                    message = if (result.isSuccess) "Backup exported successfully" else "Failed to save file",
                )
            }
        }
    }

    /** The user dismissed the save dialog — not an error, so say nothing. */
    fun cancelExport() = _uiState.update { it.copy(exportJson = null) }

    /** Reads and validates the file. Nothing is written until [confirmImport]. */
    fun onImportFileSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Could not open the selected file")
                    backupManager.parse(json)
                }
            }
            result.onSuccess { bundle ->
                _uiState.update { it.copy(isImporting = false, pendingImport = bundle) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isImporting = false, message = "Import failed: ${e.message}")
                }
            }
        }
    }

    fun confirmImport() {
        val bundle = _uiState.value.pendingImport ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, pendingImport = null) }
            runCatching { backupManager.restore(bundle) }
                .onSuccess { count ->
                    _uiState.update { it.copy(isImporting = false, message = "Restored $count entries") }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isImporting = false, message = "Import failed: ${e.message}")
                    }
                }
        }
    }

    fun cancelImport() = _uiState.update { it.copy(pendingImport = null) }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun describeLastBackup(timestamp: Long?): String {
        if (timestamp == null) return "No backup yet"
        return when (val days = daysSince(timestamp)) {
            0L -> "Last backup: today"
            1L -> "Last backup: yesterday"
            else -> "Last backup: $days days ago, ${backupDateFormat.format(Date(timestamp))}"
        }
    }

    private fun daysSince(timestamp: Long): Long {
        val elapsed = startOfDay(System.currentTimeMillis()) - startOfDay(timestamp)
        return (elapsed.toDouble() / DAY_MILLIS).roundToInt().toLong().coerceAtLeast(0L)
    }

    private fun startOfDay(timestamp: Long): Long =
        Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private companion object {
        const val STALE_AFTER_DAYS = 14L
        const val DAY_MILLIS = 24.0 * 60 * 60 * 1000
    }
}
