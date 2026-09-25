package com.workout.tracker.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.tracker.domain.model.Exercise
import com.workout.tracker.domain.model.ExerciseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseInfoScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExerciseInfoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        val content = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when {
            uiState.isLoading -> Box(content, Alignment.Center) { CircularProgressIndicator() }

            uiState.exercise == null -> Box(content, Alignment.Center) {
                Text(
                    "This exercise is no longer available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                val exercise = uiState.exercise!!
                Column(
                    modifier = content
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(typeLabel(exercise.exerciseType))
                        Chip(exercise.category)
                    }

                    prescriptionOf(exercise)?.let { prescription ->
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Text(
                                "Programmed for: $prescription",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    val guide = uiState.guide
                    if (guide == null) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "No guide has been written for this exercise yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(Modifier.height(20.dp))
                        Text(guide.summary, style = MaterialTheme.typography.bodyLarge)

                        SectionTitle("How to do it")
                        guide.steps.forEachIndexed { index, step ->
                            NumberedStep(index + 1, step)
                            Spacer(Modifier.height(10.dp))
                        }

                        if (guide.tips.isNotEmpty()) {
                            SectionTitle("Form tips")
                            guide.tips.forEach { tip ->
                                BulletLine(tip)
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        SectionTitle("Muscles worked")
                        Text(guide.musclesWorked, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun typeLabel(type: ExerciseType) = when (type) {
    ExerciseType.WEIGHTED -> "Weighted"
    ExerciseType.TIMED -> "Timed"
    ExerciseType.CARDIO -> "Cardio"
}

/** Mirrors the target line shown on the logging screen, so both read the same way. */
private fun prescriptionOf(exercise: Exercise): String? {
    val parts = buildList {
        exercise.defaultSets?.let { add("$it sets") }
        exercise.defaultReps?.let { add("× $it") }
        exercise.defaultDurationSeconds?.let { secs ->
            val m = secs / 60
            val s = secs % 60
            add(if (m > 0 && s > 0) "${m}m ${s}s" else if (m > 0) "${m}m" else "${s}s")
        }
    }
    return parts.joinToString("  ").ifBlank { null }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Chip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    Row(Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(24.dp),
        ) {
            Text(
                number.toString(),
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
