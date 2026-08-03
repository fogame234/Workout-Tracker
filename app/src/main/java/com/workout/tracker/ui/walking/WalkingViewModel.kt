package com.workout.tracker.ui.walking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.domain.model.WalkingLog
import com.workout.tracker.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalkingUiState(
    val miles: String = "",
    val km: String = "",
    val durationMinutes: String = "",
    val durationSeconds: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val recentLogs: List<WalkingLog> = emptyList(),
)

@HiltViewModel
class WalkingViewModel @Inject constructor(
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalkingUiState())
    val uiState: StateFlow<WalkingUiState> = _uiState.asStateFlow()

    // Prevents infinite loop when auto-filling the other distance field
    private var isConverting = false

    init {
        viewModelScope.launch {
            repository.getAllWalkingLogs().collect { logs ->
                _uiState.update { it.copy(recentLogs = logs) }
            }
        }
    }

    fun onMilesChange(value: String) {
        if (isConverting) return
        isConverting = true
        val km = value.toDoubleOrNull()?.let { "%.2f".format(it * 1.60934) } ?: ""
        _uiState.update { it.copy(miles = value, km = km, errorMessage = null) }
        isConverting = false
    }

    fun onKmChange(value: String) {
        if (isConverting) return
        isConverting = true
        val miles = value.toDoubleOrNull()?.let { "%.2f".format(it / 1.60934) } ?: ""
        _uiState.update { it.copy(km = value, miles = miles, errorMessage = null) }
        isConverting = false
    }

    fun onDurMinChange(v: String) = _uiState.update { it.copy(durationMinutes = v, errorMessage = null) }
    fun onDurSecChange(v: String) = _uiState.update { it.copy(durationSeconds = v, errorMessage = null) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }

    fun save() {
        val s = _uiState.value

        val miles = s.miles.toDoubleOrNull()
        val km = s.km.toDoubleOrNull()
        val durMin = s.durationMinutes.toLongOrNull() ?: 0L
        val durSec = s.durationSeconds.toLongOrNull() ?: 0L
        val totalSecs = durMin * 60 + durSec

        val err = when {
            miles == null || miles <= 0 -> "Enter a distance"
            totalSecs <= 0 -> "Enter the duration"
            else -> null
        }
        if (err != null) {
            _uiState.update { it.copy(errorMessage = err) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.insertWalkingLog(
                WalkingLog(
                    distanceMiles = miles!!,
                    distanceKm = km ?: (miles * 1.60934),
                    durationSeconds = totalSecs,
                    notes = s.notes.ifBlank { null },
                ),
            )
            _uiState.update {
                it.copy(
                    isSaving = false, isSaved = true,
                    miles = "", km = "",
                    durationMinutes = "", durationSeconds = "",
                    notes = "",
                )
            }
            _uiState.update { it.copy(isSaved = false) }
        }
    }

    fun deleteLog(log: WalkingLog) {
        viewModelScope.launch { repository.deleteWalkingLog(log) }
    }
}
