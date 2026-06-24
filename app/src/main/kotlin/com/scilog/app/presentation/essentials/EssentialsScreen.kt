package com.scilog.app.presentation.essentials

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EssentialsScreen(onBack: () -> Unit, viewModel: EssentialsViewModel = hiltViewModel()) {
    val today by viewModel.today.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Essentials") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Track your daily water and protein — both often drop on GLP-1 therapies.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
            )

            // Water card
            EssentialCard(
                emoji = "💧",
                label = "Water",
                current = today?.waterOunces ?: 0.0,
                target = today?.waterTargetOz ?: 64.0,
                unit = "oz",
                progress = today?.waterProgress ?: 0f,
                quickAmounts = listOf(8.0, 12.0, 16.0, 20.0),
                onQuickAdd = { viewModel.addWater(it) },
                color = MaterialTheme.colorScheme.primary
            )

            // Protein card
            EssentialCard(
                emoji = "🥩",
                label = "Protein",
                current = today?.proteinGrams ?: 0.0,
                target = today?.proteinTargetG ?: 100.0,
                unit = "g",
                progress = today?.proteinProgress ?: 0f,
                quickAmounts = listOf(10.0, 20.0, 30.0, 50.0),
                onQuickAdd = { viewModel.addProtein(it) },
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun EssentialCard(
    emoji: String,
    label: String,
    current: Double,
    target: Double,
    unit: String,
    progress: Float,
    quickAmounts: List<Double>,
    onQuickAdd: (Double) -> Unit,
    color: androidx.compose.ui.graphics.Color
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
                Column {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "%.0f / %.0f $unit".format(current, target),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = color
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickAmounts.forEach { amount ->
                    ElevatedButton(
                        onClick = { onQuickAdd(amount) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text("+${amount.toInt()}$unit", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
