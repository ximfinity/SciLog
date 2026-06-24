package com.scilog.app.presentation.dosage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchUpDoseScreen(
    onBack: () -> Unit,
    viewModel: CatchUpDoseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catch-Up Calculator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Disclaimer banner — always visible
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(18.dp).padding(top = 1.dp)
                    )
                    Text(
                        "This is a pharmacokinetic estimate only. Discuss all dosing changes with your prescribing provider before making any adjustments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Current dose context
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Current Dose", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "%.2f mg ${state.pkParams.displayName}".format(state.currentDoseMg),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatLabel("SS Peak", "%.3f mg/L".format(state.cMaxSS))
                        StatLabel("SS Trough", "%.3f mg/L".format(state.cMinSS))
                        if (state.cMaxSS > 0.0001) {
                            StatLabel("Trough %", "${(state.cMinSS / state.cMaxSS * 100).roundToInt()}% of peak")
                        }
                    }
                }
            }

            // Target trough — % slider with presets
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Target Trough Level", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Set how much drug you want in your system just before your next dose. The SS Trough matches your regular schedule's trough — a good default target.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Preset buttons
                    val presets = listOf(
                        "SS Trough *" to state.cMinSS,
                        "50% peak" to state.cMaxSS * 0.50,
                        "75% peak" to state.cMaxSS * 0.75
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.forEach { (label, value) ->
                            val isSelected = state.cMaxSS > 0 &&
                                kotlin.math.abs(state.targetCminMgL - value) < 0.001
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTargetCmin(value) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // % slider
                    val targetPct = if (state.cMaxSS > 0.0001)
                        (state.targetCminMgL / state.cMaxSS * 100f).toFloat().coerceIn(0f, 100f)
                    else 0f

                    Slider(
                        value = targetPct,
                        onValueChange = { pct ->
                            viewModel.setTargetCmin(pct.toDouble() / 100.0 * state.cMaxSS)
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0%", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text(
                            "Target: ${targetPct.roundToInt()}% of peak",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("100%", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }

                    Text(
                        "* SS Trough = ${(state.cMinSS / state.cMaxSS.coerceAtLeast(0.001) * 100).roundToInt()}% of peak — maintains the same receptor occupancy as your regular schedule.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                }
            }

            // Recommended catch-up dose result
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Suggested Catch-Up Dose",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "%.2f mg".format(state.recommendedDoseMg),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.recommendedDoseMg >= state.pkParams.maxDoseMg) {
                        Text(
                            "Target may not be achievable at the maximum labeled dose (${state.pkParams.maxDoseMg} mg).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "A single dose of %.2f mg taken now is estimated to bring your trough to the target level before the next scheduled injection.".format(state.recommendedDoseMg),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.labelLarge)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
    }
}
