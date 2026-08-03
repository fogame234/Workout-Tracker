package com.workout.tracker.ui.logexercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.tracker.domain.model.DifficultyLevel
import com.workout.tracker.domain.model.ExerciseLog
import com.workout.tracker.domain.model.ExerciseType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogExerciseScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogExerciseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) snackbar.showSnackbar("Entry logged!")
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.exercise?.name ?: "Log Exercise") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        val exercise = uiState.exercise ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Prescription reminder
            val prescription = buildString {
                exercise.defaultSets?.let { append("$it sets") }
                exercise.defaultReps?.let { append("  ×  $it") }
                exercise.defaultDurationSeconds?.let { s ->
                    val m = s / 60; val sec = s % 60
                    if (m > 0) append("  ${m}m")
                    if (sec > 0) append(" ${sec}s")
                }
            }
            if (prescription.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        "Target: $prescription",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            when (exercise.exerciseType) {
                ExerciseType.WEIGHTED -> WeightedFields(uiState, viewModel)
                ExerciseType.TIMED -> TimedFields(uiState, viewModel)
                ExerciseType.CARDIO -> CardioFields(uiState, viewModel)
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                uiState.notes, viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )

            Spacer(Modifier.height(16.dp))
            Button(viewModel::save, Modifier.fillMaxWidth(), enabled = !uiState.isSaving) {
                if (uiState.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Log Entry")
            }

            // Recent history
            if (uiState.recentLogs.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("History", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                uiState.recentLogs.reversed().take(10).forEach { log ->
                    HistoryRow(log, exercise.exerciseType) { viewModel.deleteLog(log) }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightedFields(s: LogExerciseUiState, vm: LogExerciseViewModel) {
    OutlinedTextField(
        s.weight, vm::onWeightChange, label = { Text("Weight (lbs) *") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(), singleLine = true,
    )
    Spacer(Modifier.height(12.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Sets dropdown 1-5
        var setsExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = setsExpanded,
            onExpandedChange = { setsExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = s.setsCompleted?.toString() ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Sets *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(setsExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                singleLine = true,
            )
            ExposedDropdownMenu(expanded = setsExpanded, onDismissRequest = { setsExpanded = false }) {
                (1..5).forEach { count ->
                    DropdownMenuItem(
                        text = { Text("$count") },
                        onClick = {
                            vm.onSetsChange(count)
                            setsExpanded = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            s.repsPerSet, vm::onRepsChange, label = { Text("Reps / Set *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f), singleLine = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimedFields(s: LogExerciseUiState, vm: LogExerciseViewModel) {
    DurationRow(s, vm, required = true)

    Spacer(Modifier.height(12.dp))

    // Difficulty dropdown
    var diffExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = diffExpanded,
        onExpandedChange = { diffExpanded = it },
    ) {
        OutlinedTextField(
            value = s.difficulty?.displayName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Difficulty *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(diffExpanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = diffExpanded, onDismissRequest = { diffExpanded = false }) {
            DifficultyLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.displayName) },
                    onClick = {
                        vm.onDifficultyChange(level)
                        diffExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CardioFields(s: LogExerciseUiState, vm: LogExerciseViewModel) {
    DurationRow(s, vm, required = true)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        s.distanceMiles, vm::onDistanceChange, label = { Text("Distance (miles)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(), singleLine = true,
    )
}

@Composable
private fun DurationRow(s: LogExerciseUiState, vm: LogExerciseViewModel, required: Boolean = false) {
    val label = if (required) "Duration *" else "Duration"
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
        OutlinedTextField(
            s.durationMinutes, vm::onDurMinChange, label = { Text("Min") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f), singleLine = true,
        )
        Text(":", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            s.durationSeconds, vm::onDurSecChange, label = { Text("Sec") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f), singleLine = true,
        )
    }
}

@Composable
private fun HistoryRow(log: ExerciseLog, type: ExerciseType, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(fmt.format(Date(log.dateTimestamp)), style = MaterialTheme.typography.labelSmall)
                Text(summarize(log, type), style = MaterialTheme.typography.bodyMedium)
                log.difficulty?.let { d ->
                    val label = try { DifficultyLevel.valueOf(d).displayName } catch (_: Exception) { d }
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                log.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun summarize(l: ExerciseLog, t: ExerciseType): String = when (t) {
    ExerciseType.WEIGHTED -> {
        val w = l.weightLbs?.let { "%.0f lbs".format(it) } ?: "BW"
        "$w × ${l.repsPerSet ?: "?"} reps × ${l.setsCompleted ?: "?"} sets"
    }
    ExerciseType.TIMED -> {
        l.durationSeconds?.let { s -> "${s / 60}m ${s % 60}s" } ?: "?"
    }
    ExerciseType.CARDIO -> {
        val d = l.distanceMiles?.let { "%.2f mi".format(it) } ?: ""
        val dur = l.durationSeconds?.let { s -> "${s / 60}m ${s % 60}s" } ?: ""
        "$d  $dur".trim()
    }
}
