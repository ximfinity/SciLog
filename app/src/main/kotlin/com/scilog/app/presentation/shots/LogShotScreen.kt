package com.scilog.app.presentation.shots

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scilog.app.domain.model.InjectionSite
import com.scilog.app.domain.model.MedicationType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogShotScreen(
    onBack: () -> Unit,
    viewModel: LogShotViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    val title = if (state.isEditMode) "Edit Injection" else "Log Injection"
    val saveLabel = if (state.isEditMode) "Update Injection" else "Save Injection"
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date & time (backdating)
            Text("Date & Time", style = MaterialTheme.typography.titleMedium)
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

            HorizontalDivider()

            // Medication type
            Text("Medication", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MedicationType.entries.forEach { type ->
                    FilterChip(
                        selected = state.medicationType == type,
                        onClick = { viewModel.setMedicationType(type) },
                        label = { Text(type.displayName) }
                    )
                }
            }

            // Dose input
            OutlinedTextField(
                value = state.doseMg,
                onValueChange = { viewModel.setDose(it) },
                label = { Text("Dose (mg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text("mg") },
                singleLine = true
            )

            // Microdose toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Microdose", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Custom ultra-fine or split dosing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = state.isMicrodose, onCheckedChange = { viewModel.setMicrodose(it) })
            }

            HorizontalDivider()

            // Injection site
            Text("Injection Site", style = MaterialTheme.typography.titleMedium)
            InjectionSite.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { site ->
                        FilterChip(
                            selected = state.selectedSite == site,
                            onClick = { viewModel.setSite(site) },
                            label = { Text(site.abbreviation) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Vial selector
            if (state.activeVials.isNotEmpty()) {
                Text("Assign Vial (optional)", style = MaterialTheme.typography.titleMedium)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.activeVials.firstOrNull { it.id == state.selectedVialId }
                            ?.let { "${it.medicationType.displayName} — %.1f mL".format(it.remainingVolumeMl) }
                            ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vial") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = { viewModel.setVial(null); expanded = false }
                        )
                        state.activeVials.forEach { vial ->
                            DropdownMenuItem(
                                text = { Text("${vial.medicationType.displayName} — %.1f mL left".format(vial.remainingVolumeMl)) },
                                onClick = { viewModel.setVial(vial.id); expanded = false }
                            )
                        }
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            state.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.logShot() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(saveLabel)
            }
        }
    }
}
