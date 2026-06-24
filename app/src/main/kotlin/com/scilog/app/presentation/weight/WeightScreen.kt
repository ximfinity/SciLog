package com.scilog.app.presentation.weight

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scilog.app.core.util.DateTimeUtils
import com.scilog.app.domain.usecase.weight.GetWeightGuidanceUseCase
import com.scilog.app.presentation.theme.LocalAppIsDark
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(onBack: (() -> Unit)? = null, viewModel: WeightViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US) }
    val graphicsLayer = rememberGraphicsLayer()

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
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
                    }
                },
                actions = {
                    if (state.allWeights.size >= 2) {
                        IconButton(onClick = {
                            scope.launch {
                                val androidBitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                val dir  = File(context.cacheDir, "exports").also { it.mkdirs() }
                                val file = File(dir, "weight_chart_${System.currentTimeMillis()}.png")
                                file.outputStream().use { out ->
                                    androidBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                }
                                val uri: Uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share weight chart"))
                            }
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share chart")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val filteredWeights = remember(state.allWeights, state.selectedRange) {
            weightsByRange(state.allWeights, state.selectedRange)
        }
        val filteredShots = remember(state.shots, state.allWeights, state.selectedRange) {
            if (filteredWeights.isEmpty()) emptyList()
            else {
                val minTs = filteredWeights.first().timestampMs
                state.shots.filter { it.timestampMs >= minTs }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Full chart (only if we have data)
            if (filteredWeights.size >= 2) {
                // Date range chips
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WeightDateRange.entries.forEach { range ->
                            FilterChip(
                                selected = state.selectedRange == range,
                                onClick  = { viewModel.setSelectedRange(range) },
                                label    = { Text(range.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Toggle chips
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = state.showTrendLine,
                            onClick  = { viewModel.toggleTrendLine() },
                            label    = { Text("Trend", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = state.showProjection,
                            onClick  = { viewModel.toggleProjection() },
                            label    = { Text("Projection", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = state.showMinMax,
                            onClick  = { viewModel.toggleMinMax() },
                            label    = { Text("Min/Max", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // The chart
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (LocalAppIsDark.current) MaterialTheme.colorScheme.surface else Color(0xFFF5F0E7)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            WeightFullChart(
                                weights         = filteredWeights,
                                shots           = filteredShots,
                                targetWeightLbs = state.targetWeightLbs,
                                showTrendLine   = state.showTrendLine,
                                showProjection  = state.showProjection,
                                showMinMax      = state.showMinMax,
                                graphicsLayer   = graphicsLayer,
                                modifier        = Modifier.fillMaxWidth()
                            )
                            // Legend
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                ChartLegendDot(Color(0xFF4F6B57), "Daily avg")
                                if (state.showTrendLine) ChartLegendDash(Color(0xFF4F6B57).copy(alpha = 0.55f), "Trend")
                                if (state.showProjection) ChartLegendDash(Color(0xFF4F6B57).copy(alpha = 0.40f), "Projection")
                                if (state.targetWeightLbs != null) ChartLegendDash(Color(0xFF2D5A27).copy(alpha = 0.60f), "Goal")
                            }

                            // Stats row
                            if (filteredWeights.size >= 2) {
                                val firstLbs = filteredWeights.first().weightLbs
                                val lastLbs  = filteredWeights.last().weightLbs
                                val deltaLbs = lastLbs - firstLbs
                                val deltaPct = deltaLbs / firstLbs * 100.0
                                val latestDoseMg = filteredShots.lastOrNull()?.doseMg

                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    WeightStatCol(
                                        value = "%+.1f lbs".format(deltaLbs),
                                        label = "CHANGE"
                                    )
                                    WeightStatCol(
                                        value = "%+.1f%%".format(deltaPct),
                                        label = "BODY WT"
                                    )
                                    if (latestDoseMg != null) {
                                        WeightStatCol(
                                            value = "%.1f mg".format(latestDoseMg),
                                            label = "CURRENT DOSE"
                                        )
                                    }
                                }
                            }
                        }
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

            // Log entry card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Log Weight", style = MaterialTheme.typography.titleMedium)

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

            // History with edit + delete
            if (state.weights.isNotEmpty()) {
                item { Text("Recent History", style = MaterialTheme.typography.titleMedium) }
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

@Composable
private fun ChartLegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color, radius = 4f)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
    }
}

@Composable
private fun ChartLegendDash(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 16.dp, height = 8.dp)) {
            drawLine(color = color, start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = 2f)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
    }
}

@Composable
private fun WeightStatCol(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}
