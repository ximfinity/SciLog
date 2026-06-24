package com.scilog.app.presentation.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scilog.app.core.util.DateTimeUtils
import com.scilog.app.domain.model.MedicationType
import com.scilog.app.domain.model.Vial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(onBack: () -> Unit, viewModel: InventoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.showAddDialog) AddVialDialog(viewModel = viewModel, state = state)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vial Inventory") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Outlined.Add, "Add Vial")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.vials.isEmpty()) {
                item {
                    Text(
                        "No vials tracked. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }
            items(state.vials) { vial ->
                VialCard(
                    vial = vial,
                    currentDoseMg = state.currentDoseMg,
                    onMarkOpen = { viewModel.markOpen(vial) },
                    onDelete = { viewModel.deleteVial(vial) }
                )
            }
        }
    }
}

@Composable
private fun VialCard(
    vial: Vial,
    currentDoseMg: Double,
    onMarkOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val shotsLeft = vial.shotsRemaining(currentDoseMg)
    val statusColor = when {
        vial.isExpired -> MaterialTheme.colorScheme.error
        vial.isLow -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (vial.isLow || vial.isExpired) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(vial.medicationType.displayName, style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (vial.isOpen) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (vial.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                        )
                        Text(
                            if (vial.isOpen) "Open" else "Sealed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (vial.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!vial.isOpen) {
                        TextButton(onClick = onMarkOpen, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Mark Open", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Volume progress bar
            LinearProgressIndicator(
                progress = { vial.percentRemaining },
                modifier = Modifier.fillMaxWidth(),
                color = if (vial.isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            // Volume + concentration row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "%.2f / %.2f mL".format(vial.remainingVolumeMl, vial.startingVolumeMl),
                    style = MaterialTheme.typography.bodySmall
                )
                Text("${vial.concentrationMgPerMl} mg/mL", style = MaterialTheme.typography.bodySmall)
                if (shotsLeft != null && currentDoseMg > 0) {
                    Text(
                        "$shotsLeft shots @ ${currentDoseMg}mg",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (shotsLeft <= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (shotsLeft <= 2) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            // Dates
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Expires ${DateTimeUtils.formatDate(vial.expirationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (vial.isExpired) MaterialTheme.colorScheme.error else statusColor
                )
                vial.bestUseDateMs?.let { bestUse ->
                    val expired = System.currentTimeMillis() > bestUse
                    Text(
                        "Best use by ${DateTimeUtils.formatDate(bestUse)} (28d from open)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (expired) MaterialTheme.colorScheme.error else statusColor
                    )
                }
                if (vial.openedDateMs > 0L) {
                    Text(
                        "Opened ${DateTimeUtils.formatDate(vial.openedDateMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            // Optional metadata
            if (vial.pharmacy.isNotBlank() || vial.provider.isNotBlank() || vial.additives.isNotBlank() || vial.costUsd >= 0) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (vial.pharmacy.isNotBlank()) MetaRow("Pharmacy", vial.pharmacy)
                    if (vial.provider.isNotBlank()) MetaRow("Provider", vial.provider)
                    if (vial.additives.isNotBlank()) MetaRow("Additives", vial.additives)
                    if (vial.costUsd >= 0) MetaRow("Cost", "$%.2f".format(vial.costUsd))
                }
            }

            if (vial.isLow) {
                Text("Low supply — consider ordering a refill.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        Text(value, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun AddVialDialog(viewModel: InventoryViewModel, state: InventoryUiState) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissDialog() },
        title = { Text("Add New Vial") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Medication type
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MedicationType.entries.forEach { type ->
                        FilterChip(
                            selected = state.newMedType == type,
                            onClick = { viewModel.setMedType(type) },
                            label = { Text(type.displayName) }
                        )
                    }
                }

                // Volume + concentration (required)
                OutlinedTextField(
                    value = state.newVolumeMl, onValueChange = { viewModel.setVolume(it) },
                    label = { Text("Volume (mL)") }, suffix = { Text("mL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.newConcentration, onValueChange = { viewModel.setConcentration(it) },
                    label = { Text("Concentration") }, suffix = { Text("mg/mL") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                // Sealed vs Open toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (state.newIsOpen) "Open" else "Sealed", style = MaterialTheme.typography.labelSmall)
                        Switch(checked = state.newIsOpen, onCheckedChange = { viewModel.setIsOpen(it) })
                    }
                }

                // Optional fields
                Text("Optional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                OutlinedTextField(
                    value = state.newPharmacy, onValueChange = { viewModel.setPharmacy(it) },
                    label = { Text("Pharmacy") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.newProvider, onValueChange = { viewModel.setProvider(it) },
                    label = { Text("Provider / Prescriber") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.newAdditives, onValueChange = { viewModel.setAdditives(it) },
                    label = { Text("Additives (e.g. BPC-157)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.newCostUsd, onValueChange = { viewModel.setCostUsd(it) },
                    label = { Text("Cost") }, prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { Button(onClick = { viewModel.addVial() }) { Text("Add") } },
        dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancel") } }
    )
}
