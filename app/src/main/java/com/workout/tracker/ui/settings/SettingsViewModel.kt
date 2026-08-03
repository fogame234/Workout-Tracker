package com.workout.tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val exportJson: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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

    fun onExportWritten() {
        _uiState.update { it.copy(exportJson = null, message = "Backup exported successfully") }
    }

    fun onExportFailed() {
        _uiState.update { it.copy(exportJson = null, message = "Failed to save file") }
    }

    fun importFromJson(json: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            val result = backupManager.importFromJson(json)
            result.onSuccess { count ->
                _uiState.update { it.copy(isImporting = false, message = "Restored $count entries") }
            }.onFailure { e ->
                _uiState.update { it.copy(isImporting = false, message = "Import failed: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
