package com.workout.tracker.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val context = LocalContext.current

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
        if (uri != null && uiState.exportJson != null) {
            val success = writeToUri(context, uri, uiState.exportJson!!)
            if (success) viewModel.onExportWritten() else viewModel.onExportFailed()
        } else {
            viewModel.onExportFailed()
        }
    }

    // Watch for export data ready -> open file picker
    LaunchedEffect(uiState.exportJson) {
        if (uiState.exportJson != null) {
            val date = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault()).format(Date())
            exportLauncher.launch("workout_backup_$date.json")
        }
    }

    // File picker to read import
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val json = readFromUri(context, uri)
            if (json != null) viewModel.importFromJson(json)
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { _ ->
        Column(
            modifier = modifier
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

            Spacer(Modifier.height(24.dp))

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
    }
}

private fun writeToUri(context: Context, uri: Uri, content: String): Boolean = try {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(content.toByteArray(Charsets.UTF_8))
    }
    true
} catch (_: Exception) {
    false
}

private fun readFromUri(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        stream.bufferedReader(Charsets.UTF_8).readText()
    }
} catch (_: Exception) {
    null
}
