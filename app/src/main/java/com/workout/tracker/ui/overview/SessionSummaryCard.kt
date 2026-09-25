package com.workout.tracker.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What was logged today, or the most recent session on a rest day. */
@Composable
fun SessionSummaryCard(
    onExerciseClick: (Long) -> Unit,
    onWalkingClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()) }

    // Render nothing while loading rather than flashing an empty card into place.
    if (uiState.isLoading) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            val summary = uiState.summary

            if (summary == null) {
                Text(
                    "No workouts logged yet",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Log a workout and it will show up here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                return@Column
            }

            Text(
                text = headerFor(summary, dateFormat),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            Spacer(Modifier.height(10.dp))

            summary.entries.forEachIndexed { index, entry ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                SessionRow(
                    entry = entry,
                    onClick = {
                        val id = entry.exerciseId
                        if (id == null) onWalkingClick() else onExerciseClick(id)
                    },
                )
            }
        }
    }
}

private fun headerFor(summary: SessionSummary, dateFormat: SimpleDateFormat): String {
    val date = dateFormat.format(Date(summary.dateTimestamp))
    if (summary.isToday) return "TODAY  ·  $date"

    val ago = when (summary.daysAgo) {
        1 -> "yesterday"
        else -> "${summary.daysAgo} days ago"
    }
    return "LAST SESSION  ·  $date  ·  $ago"
}

@Composable
private fun SessionRow(entry: SessionEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            entry.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            entry.detail,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
        )
    }
}
