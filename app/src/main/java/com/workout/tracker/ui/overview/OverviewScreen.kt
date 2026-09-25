package com.workout.tracker.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/** Just past the navigation transition, so a stats reload never lands mid-animation. */
private const val STATS_REFRESH_SETTLE_MS = 350L

@Composable
fun OverviewScreen(
    onExerciseClick: (Long) -> Unit,
    onWalkingProgressClick: () -> Unit = {},
    refreshTrigger: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = hiltViewModel(),
) {
    LaunchedEffect(refreshTrigger) {
        // Let the navigation animation finish first; the stats rebuild drops frames.
        delay(STATS_REFRESH_SETTLE_MS)
        viewModel.refresh()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "session") {
            SessionSummaryCard(
                onExerciseClick = onExerciseClick,
                onWalkingClick = onWalkingProgressClick,
            )
        }

        // Time period chips
        item(key = "period") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimePeriod.entries.forEach { period ->
                    FilterChip(
                        selected = period == uiState.selectedPeriod,
                        onClick = { viewModel.selectPeriod(period) },
                        label = { Text(period.label) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Weight section
        if (uiState.weightTiles.isNotEmpty()) {
            item(key = "h_weight") {
                SectionHeader(Icons.Default.FitnessCenter, "Weight exercises")
            }
            val weightPairs = uiState.weightTiles.chunked(2)
            weightPairs.forEachIndexed { idx, pair ->
                item(key = "w_row_$idx") {
                    TileRow(pair, onExerciseClick)
                }
            }
        }

        // Timed section
        if (uiState.timedTiles.isNotEmpty()) {
            item(key = "h_timed") {
                SectionHeader(Icons.Default.Timer, "Timed exercises")
            }
            val timedPairs = uiState.timedTiles.chunked(2)
            timedPairs.forEachIndexed { idx, pair ->
                item(key = "t_row_$idx") {
                    TileRow(pair, onExerciseClick)
                }
            }
        }

        // Cardio section
        if (uiState.cardioTiles.isNotEmpty()) {
            item(key = "h_cardio") {
                SectionHeader(Icons.Default.DirectionsRun, "Cardio")
            }
            val cardioPairs = uiState.cardioTiles.chunked(2)
            cardioPairs.forEachIndexed { idx, pair ->
                item(key = "c_row_$idx") {
                    TileRow(pair, onExerciseClick)
                }
            }
        }

        // Walking section
        if (uiState.walkingTiles.isNotEmpty()) {
            item(key = "h_walking") {
                SectionHeader(Icons.Default.DirectionsWalk, "Walking")
            }
            val walkingPairs = uiState.walkingTiles.chunked(2)
            walkingPairs.forEachIndexed { idx, pair ->
                item(key = "wk_row_$idx") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { tile ->
                            MetricTile(tile, onWalkingProgressClick, Modifier.weight(1f))
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        Modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TileRow(tiles: List<ExerciseTile>, onClick: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.forEach { tile ->
            val exerciseId = tile.exerciseIds.firstOrNull()
            MetricTile(
                tile = tile,
                onClick = { exerciseId?.let(onClick) },
                modifier = Modifier.weight(1f),
            )
        }
        if (tiles.size == 1) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricTile(
    tile: ExerciseTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (tile.hasData) MaterialTheme.colorScheme.surfaceVariant
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val headlineColor = if (tile.hasData) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                tile.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                tile.headline,
                style = MaterialTheme.typography.titleLarge,
                color = headlineColor,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    tile.subline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, modifier = Modifier.weight(1f, fill = false),
                )
                tile.changeText?.let { change ->
                    val isPositive = change.startsWith("+")
                    val color = if (isPositive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                    val icon = if (isPositive) Icons.Default.TrendingUp
                               else Icons.Default.TrendingDown
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, Modifier.size(14.dp), tint = color)
                        Spacer(Modifier.width(2.dp))
                        Text(change, style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
            }
        }
    }
}
