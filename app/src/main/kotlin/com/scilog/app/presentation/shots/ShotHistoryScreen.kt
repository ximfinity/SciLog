package com.scilog.app.presentation.shots

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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotHistoryScreen(
    onBack: () -> Unit,
    onEditShot: (Long) -> Unit,
    viewModel: ShotHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var shotToDelete by remember { mutableStateOf<Shot?>(null) }

    if (shotToDelete != null) {
        AlertDialog(
            onDismissRequest = { shotToDelete = null },
            title = { Text("Delete injection?") },
            text = { Text("This will remove the shot and recalculate your serum levels.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteShot(shotToDelete!!)
                    shotToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { shotToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Injection History")
                        if (!state.isLoading) {
                            Text(
                                "${state.shots.size} record${if (state.shots.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.shots.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Inventory2, null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                        Text("No injections logged yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                }
            }
            else -> {
                val grouped = state.shots.groupByMonth()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    grouped.forEach { (monthLabel, shots) ->
                        item(key = monthLabel) {
                            Text(
                                monthLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(shots, key = { it.id }) { shot ->
                            ShotHistoryItem(
                                shot = shot,
                                onEdit = { onEditShot(shot.id) },
                                onDelete = { shotToDelete = shot }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ShotHistoryItem(
    shot: Shot,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("EEE MMM d, yyyy  h:mm a", Locale.US) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${shot.medicationType.displayName}  ${shot.doseMg} mg",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (shot.isMicrodose) {
                    Badge { Text("Micro") }
                }
            }
            Text(
                dateFmt.format(Date(shot.timestampMs)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
            )
            if (shot.injectionSite != null) {
                Text(
                    shot.injectionSite.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                )
            }
            if (shot.notes.isNotBlank()) {
                Text(
                    shot.notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                    maxLines = 1
                )
            }
        }
        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.Edit, "Edit",
                    tint = MaterialTheme.colorScheme.primary.copy(0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.Delete, "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
}

private fun List<Shot>.groupByMonth(): List<Pair<String, List<Shot>>> {
    val fmt = SimpleDateFormat("MMMM yyyy", Locale.US)
    return groupBy { fmt.format(Date(it.timestampMs)) }
        .entries
        .sortedByDescending { it.value.first().timestampMs }
        .map { it.key to it.value }
}
