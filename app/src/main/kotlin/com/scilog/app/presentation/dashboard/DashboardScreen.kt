package com.scilog.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scilog.app.core.util.DateTimeUtils
import com.scilog.app.domain.usecase.weight.GetWeightGuidanceUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToChart: () -> Unit,
    onNavigateToLogShot: () -> Unit,
    onNavigateToEditShot: (Long) -> Unit,
    onNavigateToShotHistory: () -> Unit,
    onNavigateToWeight: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToCatchUpDose: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SciLog", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onNavigateToConfig) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Configuration")
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (isDarkTheme) "Light mode" else "Dark mode"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            SpeedDialFab(
                onLogShot = onNavigateToLogShot,
                onLogWeight = onNavigateToWeight,
                onInventory = onNavigateToInventory
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Stoplight medication status
            item {
                StoplightCard(
                    level               = state.stoplight,
                    label               = state.stoplightLabel,
                    description         = state.stoplightDescription,
                    currentMg           = state.currentLevelMg,
                    fractionOfPeak      = state.fractionOfPeak,
                    hoursUntilNextDose  = state.hoursUntilNextDose
                )
            }

            // Serum level forecast chart
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Serum Level Forecast", style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = onNavigateToChart, contentPadding = PaddingValues(0.dp)) {
                                Text("Full chart →", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        HomeDecayChart(
                            actualPoints     = state.decayPoints,
                            projectedPoints  = state.projectedDecayPoints,
                            cMaxSS           = state.cMaxSS,
                            cMinSS           = state.cMinSS,
                            projectedDosesMs = state.projectedDosesMs,
                            targetCmaxSS     = state.targetCmaxSS,
                            targetCminSS     = state.targetCminSS,
                            modifier         = Modifier.fillMaxWidth()
                        )
                        if (state.decayPoints.isNotEmpty()) {
                            Text(
                                "— Actual  - - Projected  · ${state.pkParams.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }

            // Dosage insight card
            state.dosageInsight?.let { insight ->
                item {
                    DosageInsightCard(
                        insight = insight,
                        onOpenCatchUpCalculator = onNavigateToCatchUpDose
                    )
                }
            }

            // Weight progress chart
            if (state.weightHistory.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Weight Progress", style = MaterialTheme.typography.titleSmall)
                                TextButton(onClick = onNavigateToWeight, contentPadding = PaddingValues(0.dp)) {
                                    Text("Log →", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            WeightProgressChart(
                                weights  = state.weightHistory,
                                shots    = state.allShotsForChart,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small))
                                    Text("Weight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.tertiary, shape = MaterialTheme.shapes.small))
                                    Text("Injection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                            }
                        }
                    }
                }
            }

            // Weight guidance banner
            state.weightGuidance?.let { guidance ->
                if (guidance.guidance != GetWeightGuidanceUseCase.Guidance.INSUFFICIENT_DATA) {
                    item { GuidanceBanner(guidance = guidance, onNavigateToWeight = onNavigateToWeight) }
                }
            }

            // Quick-action chips
            item {
                Text(
                    "Quick Log",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { QuickActionChip(Icons.Outlined.FitnessCenter, "Weight", onNavigateToWeight) }
                    item { QuickActionChip(Icons.Outlined.Science, "Inventory", onNavigateToInventory) }
                }
            }

            // Recent shots (2 max)
            if (state.recentShots.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Shots",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        TextButton(onClick = onNavigateToShotHistory, contentPadding = PaddingValues(0.dp)) {
                            Text("View all →", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                items(state.recentShots) { shot ->
                    ShotHistoryRow(
                        shot     = shot,
                        onEdit   = { onNavigateToEditShot(shot.id) },
                        onDelete = { viewModel.deleteShot(shot) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun GuidanceBanner(
    guidance: GetWeightGuidanceUseCase.GuidanceResult,
    onNavigateToWeight: () -> Unit
) {
    val (containerColor, iconTint) = when (guidance.guidance) {
        GetWeightGuidanceUseCase.Guidance.CONSIDER_DOSE_INCREASE ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        GetWeightGuidanceUseCase.Guidance.WEIGHT_LOSS_TOO_RAPID ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Card(
        onClick = onNavigateToWeight,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.TrendingDown, null, tint = iconTint)
            Text(guidance.message, style = MaterialTheme.typography.bodyMedium, color = iconTint, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    ElevatedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ShotHistoryRow(
    shot: com.scilog.app.domain.model.Shot,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${shot.medicationType.displayName} — ${shot.doseMg} mg",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                DateTimeUtils.relativeTime(shot.timestampMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (shot.isMicrodose) {
                Badge(modifier = Modifier.padding(end = 4.dp)) { Text("Micro") }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Edit, "Edit shot",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Delete, "Delete shot",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}
