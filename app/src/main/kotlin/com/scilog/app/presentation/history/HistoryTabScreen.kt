package com.scilog.app.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scilog.app.domain.model.Shot
import com.scilog.app.domain.model.Weight
import com.scilog.app.presentation.shots.ShotHistoryViewModel
import com.scilog.app.presentation.weight.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTabScreen(
    onNavigateToLogShot: () -> Unit,
    onEditShot: (Long) -> Unit,
    onNavigateToWeight: () -> Unit,
    shotViewModel: ShotHistoryViewModel = hiltViewModel(),
    weightViewModel: WeightViewModel = hiltViewModel()
) {
    val shotState by shotViewModel.uiState.collectAsStateWithLifecycle()
    val weightState by weightViewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var shotToDelete by remember { mutableStateOf<Shot?>(null) }
    var weightToDelete by remember { mutableStateOf<Weight?>(null) }

    // Delete confirmation dialogs
    shotToDelete?.let { shot ->
        AlertDialog(
            onDismissRequest = { shotToDelete = null },
            title = { Text("Delete injection?") },
            text = { Text("This will remove the shot and recalculate your serum levels.") },
            confirmButton = {
                TextButton(onClick = { shotViewModel.deleteShot(shot); shotToDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { shotToDelete = null }) { Text("Cancel") } }
        )
    }

    weightToDelete?.let { weight ->
        AlertDialog(
            onDismissRequest = { weightToDelete = null },
            title = { Text("Delete weight entry?") },
            confirmButton = {
                TextButton(onClick = { weightViewModel.delete(weight); weightToDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { weightToDelete = null }) { Text("Cancel") } }
        )
    }

    // Weight edit dialog
    if (weightState.editingWeight != null) {
        AlertDialog(
            onDismissRequest = { weightViewModel.cancelEdit() },
            title = { Text("Edit Weight") },
            text = {
                OutlinedTextField(
                    value = weightState.editInputLbs,
                    onValueChange = { weightViewModel.setEditInput(it) },
                    label = { Text("Weight (lbs)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { weightViewModel.confirmEdit() },
                    enabled = weightState.editInputLbs.toDoubleOrNull() != null
                ) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { weightViewModel.cancelEdit() }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToLogShot,
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Log Shot") }
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToWeight,
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("Log Weight") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            if (shotState.isLoading) "Shots"
                            else "Shots (${shotState.shots.size})"
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            if (weightState.weights.isEmpty()) "Weight"
                            else "Weight (${weightState.weights.size})"
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> ShotsTabContent(
                    isLoading = shotState.isLoading,
                    shots = shotState.shots,
                    onEdit = onEditShot,
                    onDelete = { shotToDelete = it }
                )
                1 -> WeightTabContent(
                    weights = weightState.weights,
                    onEdit = { weightViewModel.startEdit(it) },
                    onDelete = { weightToDelete = it }
                )
            }
        }
    }
}

@Composable
private fun ShotsTabContent(
    isLoading: Boolean,
    shots: List<Shot>,
    onEdit: (Long) -> Unit,
    onDelete: (Shot) -> Unit
) {
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        shots.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Inventory2, null, modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                Text("No injections logged yet", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
        }
        else -> {
            val fmt = SimpleDateFormat("MMMM yyyy", Locale.US)
            val grouped = shots
                .groupBy { fmt.format(Date(it.timestampMs)) }
                .entries
                .sortedByDescending { it.value.first().timestampMs }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                grouped.forEach { (month, monthShots) ->
                    item(key = month) {
                        Text(month, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                    }
                    items(monthShots, key = { it.id }) { shot ->
                        HistoryShotRow(shot = shot, onEdit = { onEdit(shot.id) }, onDelete = { onDelete(shot) })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun WeightTabContent(
    weights: List<Weight>,
    onEdit: (Weight) -> Unit,
    onDelete: (Weight) -> Unit
) {
    if (weights.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.FitnessCenter, null, modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                Text("No weight entries yet", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
        }
        return
    }
    val dateFmt = remember { SimpleDateFormat("EEE MMM d, yyyy  h:mm a", Locale.US) }
    val sorted = weights.sortedByDescending { it.timestampMs }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(sorted, key = { it.id }) { weight ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("%.1f lbs".format(weight.weightLbs), style = MaterialTheme.typography.bodyMedium)
                    Text(dateFmt.format(Date(weight.timestampMs)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                    if (weight.notes.isNotBlank()) {
                        Text(weight.notes, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.4f), maxLines = 1)
                    }
                }
                Row {
                    IconButton(onClick = { onEdit(weight) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Edit, "Edit",
                            tint = MaterialTheme.colorScheme.primary.copy(0.7f),
                            modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDelete(weight) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Delete, "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(0.65f),
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun HistoryShotRow(shot: Shot, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("EEE MMM d, yyyy  h:mm a", Locale.US) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${shot.medicationType.displayName}  ${shot.doseMg} mg",
                    style = MaterialTheme.typography.bodyMedium)
                if (shot.isMicrodose) Badge { Text("Micro") }
            }
            Text(dateFmt.format(Date(shot.timestampMs)), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
            if (shot.injectionSite != null) {
                Text(shot.injectionSite.displayName, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            }
        }
        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Edit, "Edit",
                    tint = MaterialTheme.colorScheme.primary.copy(0.7f),
                    modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Delete, "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(0.65f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
}
