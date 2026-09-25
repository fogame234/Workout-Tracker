package com.workout.tracker.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.tracker.data.backup.BackupBundle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // File picker to save export
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) viewModel.writeExportTo(uri) else viewModel.cancelExport()
    }

    // Watch for export data ready -> open file picker
    LaunchedEffect(uiState.exportJson) {
        if (uiState.exportJson != null) {
            val date = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault()).format(Date())
            exportLauncher.launch("workout_backup_$date.json")
        }
    }

    // File picker to read import. Nothing is written until the user confirms.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.onImportFileSelected(uri)
    }

    uiState.pendingImport?.let { bundle ->
        ImportConfirmationDialog(
            bundle = bundle,
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::cancelImport,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Export your workout data as a JSON file to keep a backup. Import to restore from a previous backup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            if (uiState.lastBackupLabel.isNotEmpty()) {
                val statusColor = if (uiState.lastBackupIsStale) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, Modifier.size(16.dp), tint = statusColor)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        uiState.lastBackupLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Export card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Export Data", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Saves all workout days, exercises, logs, and walking data to a file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { viewModel.prepareExport() },
                        enabled = !uiState.isExporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Export Backup")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Import card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Import Data", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Restores data from a backup file. This will replace all current data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        enabled = !uiState.isImporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.isImporting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.FileDownload, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Import Backup")
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ImportConfirmationDialog(
    bundle: BackupBundle,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val entries = plural(bundle.exerciseLogs.size, "logged entry", "logged entries")
    val walks = plural(bundle.walkingLogs.size, "walk", "walks")
    val exportedOn = bundle.exportDate
        .takeIf { it > 0L }
        ?.let { " Exported ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))}." }
        .orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace all data?") },
        text = {
            Text(
                "This backup contains $entries and $walks.$exportedOn\n\n" +
                    "Restoring replaces everything currently in the app. This cannot be undone.",
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Restore") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun plural(count: Int, singular: String, plural: String): String =
    "$count ${if (count == 1) singular else plural}"
