package com.workout.tracker.ui.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.tracker.ui.components.ProgressChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${uiState.exercise?.name ?: ""} Progress") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            if (uiState.metrics.size > 1) {
                Spacer(Modifier.height(4.dp))
                ScrollableTabRow(
                    selectedTabIndex = uiState.metrics.indexOf(uiState.selectedMetric).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 0.dp,
                ) {
                    uiState.metrics.forEach { m ->
                        Tab(m == uiState.selectedMetric, { viewModel.selectMetric(m) }, text = { Text(m.label) })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (uiState.chartData.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    Text(
                        "No entries yet — log a workout to track progress.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(uiState.selectedMetric?.label ?: "", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        ProgressChart(uiState.chartData)
                        if (uiState.chartData.size == 1) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Starting point — log more sessions to see your trend.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (uiState.changeText.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Overall Change", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                uiState.changeText,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                "From ${uiState.chartData.first().label} to ${uiState.chartData.last().label}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
