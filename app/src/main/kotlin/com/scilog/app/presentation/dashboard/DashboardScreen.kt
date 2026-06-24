package com.scilog.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

            // Combined status dashboard card
            item {
                DashboardSummaryCard(
                    stoplight            = state.stoplight,
                    stoplightLabel       = state.stoplightLabel,
                    stoplightDescription = state.stoplightDescription,
                    currentMg            = state.currentLevelMg,
                    fractionOfPeak       = state.fractionOfPeak,
                    hoursUntilNextDose   = state.hoursUntilNextDose,
                    dosageInsight        = state.dosageInsight,
                    weightGuidance       = state.weightGuidance,
                    latestWeightLbs      = state.latestWeight?.weightLbs,
                    onNavigateToCatchUpDose = onNavigateToCatchUpDose
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
                                    Text("View →", style = MaterialTheme.typography.labelSmall)
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

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
