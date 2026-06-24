package com.scilog.app.presentation.weight

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scilog.app.core.util.DateTimeUtils
import com.scilog.app.domain.usecase.weight.GetWeightGuidanceUseCase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(onBack: () -> Unit, viewModel: WeightViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US) }

    // Edit dialog
    if (state.editingWeight != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEdit() },
            title = { Text("Edit Weight") },
            text = {
                OutlinedTextField(
                    value = state.editInputLbs,
                    onValueChange = { viewModel.setEditInput(it) },
                    label = { Text("Weight") },
                    suffix = { Text("lbs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmEdit() },
                    enabled = state.editInputLbs.toDoubleOrNull() != null
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEdit() }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight Tracker") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Log entry card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Log Weight", style = MaterialTheme.typography.titleMedium)

                        // Date & time picker
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = state.timestampMs }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        TimePickerDialog(
                                            context,
                                            { _, h, min ->
                                                cal.set(y, m, d, h, min, 0)
                                                viewModel.setTimestamp(cal.timeInMillis)
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            false
                                        ).show()
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.DateRange, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(dateFmt.format(Date(state.timestampMs)))
                        }

                        OutlinedTextField(
                            value = state.inputLbs,
                            onValueChange = { viewModel.setInput(it) },
                            label = { Text("Weight") },
                            suffix = { Text("lbs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = { viewModel.setNotes(it) },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                        state.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { viewModel.save() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSaving
                        ) { Text("Save") }
                    }
                }
            }

            // Guidance banner
            state.guidance?.let { guidance ->
                if (guidance.guidance != GetWeightGuidanceUseCase.Guidance.INSUFFICIENT_DATA) {
                    item {
                        val containerColor = when (guidance.guidance) {
                            GetWeightGuidanceUseCase.Guidance.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer
                            GetWeightGuidanceUseCase.Guidance.CONSIDER_DOSE_INCREASE -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = containerColor)
                        ) {
                            Text(guidance.message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // History with edit + delete
            if (state.weights.isNotEmpty()) {
                item { Text("History", style = MaterialTheme.typography.titleMedium) }
                items(state.weights) { weight ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "%.1f lbs".format(weight.weightLbs),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                DateTimeUtils.formatDateTime(weight.timestampMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Row {
                            IconButton(onClick = { viewModel.startEdit(weight) }) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(onClick = { viewModel.delete(weight) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
