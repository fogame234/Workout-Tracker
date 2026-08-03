package com.workout.tracker.ui.walking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.tracker.domain.model.WalkingLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkingScreen(
    onNavigateBack: () -> Unit,
    onViewProgress: () -> Unit,
    viewModel: WalkingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) snackbar.showSnackbar("Walk logged!")
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Walking") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    androidx.compose.material3.TextButton(onClick = onViewProgress) {
                        Text("Progress")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text("Distance *", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    uiState.miles, viewModel::onMilesChange,
                    label = { Text("Miles") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
                OutlinedTextField(
                    uiState.km, viewModel::onKmChange,
                    label = { Text("Kilometers") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Duration *", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                OutlinedTextField(
                    uiState.durationMinutes, viewModel::onDurMinChange,
                    label = { Text("Min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
                Text(":", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    uiState.durationSeconds, viewModel::onDurSecChange,
                    label = { Text("Sec") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                uiState.notes, viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )

            Spacer(Modifier.height(16.dp))
            Button(viewModel::save, Modifier.fillMaxWidth(), enabled = !uiState.isSaving) {
                if (uiState.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Log Walk")
            }

            if (uiState.recentLogs.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("History", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                uiState.recentLogs.reversed().take(10).forEach { log ->
                    WalkingHistoryRow(log) { viewModel.deleteLog(log) }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WalkingHistoryRow(log: WalkingLog, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val pace = if (log.distanceMiles > 0) log.durationSeconds / 60.0 / log.distanceMiles else null

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(fmt.format(Date(log.dateTimestamp)), style = MaterialTheme.typography.labelSmall)
                Text(
                    "%.2f mi / %.2f km".format(log.distanceMiles, log.distanceKm),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val m = log.durationSeconds / 60
                val s = log.durationSeconds % 60
                Text(
                    "${m}m ${s}s" + (pace?.let { "  •  %.1f min/mi".format(it) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
