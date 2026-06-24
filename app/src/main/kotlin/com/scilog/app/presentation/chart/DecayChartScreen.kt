package com.scilog.app.presentation.chart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scilog.app.core.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecayChartScreen(
    onBack: (() -> Unit)?,
    viewModel: DecayChartViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Serum PK Model") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSymptomOverlay() }) {
                        Icon(
                            if (state.showSymptoms) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            "Toggle symptoms"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Model info chip
            item {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "2-compartment PK · ka=0.5/d · CL=0.8 L/d · V₂=10 L · V₃=12 L · Q=1.5 L/d",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Stats row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentPct = if (state.cMaxSS > 0) (state.currentConcMgL / state.cMaxSS * 100).toInt() else 0
                    StatChip(
                        label = "Now",
                        value = "${currentPct}% of peak",
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        label = "Peak (SS)",
                        value = "100%",
                        valueColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    val troughPct = if (state.cMaxSS > 0) (state.cMinSS / state.cMaxSS * 100).toInt() else 0
                    StatChip(
                        label = "Trough (SS)",
                        value = "${troughPct}% of peak",
                        valueColor = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // PK chart
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DecayChartCanvas(
                            actualPoints    = state.actualPoints,
                            projectedPoints = state.projectedPoints,
                            cMaxSS          = state.cMaxSS,
                            cMinSS          = state.cMinSS,
                            symptoms        = state.symptoms,
                            showSymptoms    = state.showSymptoms,
                            targetCmaxSS    = state.targetCmaxSS,
                            targetCminSS    = state.targetCminSS
                        )
                        // Legend
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LegendItem("— Actual",    MaterialTheme.colorScheme.primary)
                            LegendItem("- - Projected", MaterialTheme.colorScheme.primary.copy(0.45f))
                            LegendItem("Cmax SS", Color(0xFF2E7D32))
                            LegendItem("Cmin SS", Color(0xFFE65100))
                            if (state.targetDoseMg != null) {
                                LegendItem("Target (${state.targetDoseMg}mg)", Color(0xFF1565C0))
                            }
                        }
                        if (state.showSymptoms && state.symptoms.isNotEmpty()) SymptomLegend()
                    }
                }
            }

            // Accumulation note
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.5f)
                    )
                ) {
                    Text(
                        "Sawtooth pattern shows drug accumulation over ~4 weeks until steady-state is reached. " +
                        "Cmax and Cmin reference lines represent theoretical steady-state peak and trough concentrations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Symptom list
            if (state.showSymptoms && state.symptoms.isNotEmpty()) {
                item { Text("Logged Symptoms", style = MaterialTheme.typography.titleMedium) }
                items(state.symptoms) { symptom ->
                    SymptomRow(symptom = symptom)
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.65f))
            Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp).padding(top = 3.dp)) {
            drawCircle(color = color, radius = 4f)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
    }
}

@Composable
private fun SymptomLegend() {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            "Nausea"   to com.scilog.app.presentation.theme.ChartNausea,
            "Fatigue"  to com.scilog.app.presentation.theme.ChartFatigue,
            "Appetite" to com.scilog.app.presentation.theme.ChartAppetite
        ).forEach { (label, color) ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp).padding(top = 3.dp)) {
                    drawCircle(color = color)
                }
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SymptomRow(symptom: com.scilog.app.domain.model.Symptom) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(symptom.symptomType.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                DateTimeUtils.relativeTime(symptom.timestampMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
            )
        }
        Row {
            repeat(symptom.severity) {
                Text("●", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            repeat(5 - symptom.severity) {
                Text("●", color = MaterialTheme.colorScheme.outlineVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}
