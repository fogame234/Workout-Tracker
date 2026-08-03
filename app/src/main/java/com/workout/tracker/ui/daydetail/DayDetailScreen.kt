package com.workout.tracker.ui.daydetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.tracker.domain.model.ExerciseType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    onNavigateBack: () -> Unit,
    onLogExercise: (Long) -> Unit,
    onViewProgress: (Long) -> Unit,
    viewModel: DayDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Day ${uiState.day?.dayNumber ?: ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(uiState.day?.title ?: "")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val grouped = uiState.exercises.groupBy { it.exercise.category }
            grouped.forEach { (category, exercises) ->
                item(key = "header_$category") {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(exercises, key = { it.exercise.id }) { item ->
                    ExerciseCard(
                        item = item,
                        onLogClick = { onLogExercise(item.exercise.id) },
                        onProgressClick = { onViewProgress(item.exercise.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    item: ExerciseWithLastLog,
    onLogClick: () -> Unit,
    onProgressClick: () -> Unit,
) {
    val exercise = item.exercise
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val icon = when (exercise.exerciseType) {
        ExerciseType.WEIGHTED -> Icons.Default.FitnessCenter
        ExerciseType.TIMED -> Icons.Default.Timer
        ExerciseType.CARDIO -> Icons.Default.DirectionsRun
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                    val prescription = buildPrescription(exercise)
                    if (prescription.isNotEmpty()) {
                        Text(
                            prescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Last logged info
            item.lastLog?.let { log ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Last: ${formatLogSummary(log, exercise.exerciseType)} — ${dateFormat.format(Date(log.dateTimestamp))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onLogClick, Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Log")
                }
                FilledTonalButton(onClick = onProgressClick) {
                    Icon(Icons.Default.ShowChart, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun buildPrescription(exercise: com.workout.tracker.domain.model.Exercise): String {
    val parts = mutableListOf<String>()
    exercise.defaultSets?.let { parts.add("$it sets") }
    exercise.defaultReps?.let { parts.add("× $it reps") }
    exercise.defaultDurationSeconds?.let { secs ->
        val m = secs / 60
        val s = secs % 60
        parts.add(if (m > 0 && s > 0) "${m}m ${s}s" else if (m > 0) "${m} min" else "${s}s")
    }
    return parts.joinToString("  ")
}

private fun formatLogSummary(log: com.workout.tracker.domain.model.ExerciseLog, type: ExerciseType): String =
    when (type) {
        ExerciseType.WEIGHTED -> {
            val w = log.weightLbs?.let { "%.0f lbs".format(it) } ?: "BW"
            val r = log.repsPerSet?.let { "× $it reps" } ?: ""
            val s = log.setsCompleted?.let { "× $it sets" } ?: ""
            "$w $r $s".trim()
        }
        ExerciseType.TIMED -> {
            val dur = log.durationSeconds?.let { secs ->
                val m = secs / 60; val s = secs % 60
                if (m > 0) "${m}m ${s}s" else "${s}s"
            } ?: "?"
            val diff = log.difficulty?.let {
                try { " — ${com.workout.tracker.domain.model.DifficultyLevel.valueOf(it).displayName}" }
                catch (_: Exception) { "" }
            } ?: ""
            "$dur$diff"
        }
        ExerciseType.CARDIO -> {
            val d = log.distanceMiles?.let { "%.2f mi".format(it) } ?: ""
            val dur = log.durationSeconds?.let { secs ->
                val m = secs / 60; val s = secs % 60
                "${m}m ${s}s"
            } ?: ""
            "$d  $dur".trim()
        }
    }
