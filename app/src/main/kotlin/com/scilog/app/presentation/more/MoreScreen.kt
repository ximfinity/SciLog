package com.scilog.app.presentation.more

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToCatchUpDose: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.exportUri.collect { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export SciLog Data"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionHeader("Tools")
            MoreListItem(
                icon = Icons.Outlined.Calculate,
                title = "Catch-Up Calculator",
                subtitle = "Estimate a dose to reach your target level",
                onClick = onNavigateToCatchUpDose
            )

            Spacer(Modifier.height(8.dp))
            SectionHeader("Management")
            MoreListItem(
                icon = Icons.Outlined.Science,
                title = "Vial Inventory",
                subtitle = "Track remaining medication",
                onClick = onNavigateToInventory
            )
            MoreListItem(
                icon = Icons.Outlined.Upload,
                title = "Import Data",
                subtitle = "Paste CSV or use the AI prompt generator",
                onClick = onNavigateToImport
            )

            Spacer(Modifier.height(8.dp))
            SectionHeader("Export")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Share your shots and weight history as a file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedButton(
                        onClick = { viewModel.exportCsv() },
                        enabled = !state.isExporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.TableChart, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export as CSV")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionHeader("Settings & Info")
            MoreListItem(
                icon = Icons.Outlined.Settings,
                title = "Configuration",
                subtitle = "Injection schedule, goal weight, PK settings",
                onClick = onNavigateToConfig
            )
            MoreListItem(
                icon = Icons.Outlined.Link,
                title = "Resources",
                subtitle = "Communities and research links",
                onClick = onNavigateToResources
            )
            MoreListItem(
                icon = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                title = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                subtitle = null,
                onClick = onToggleTheme
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun MoreListItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                }
            }
            Icon(Icons.Outlined.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                modifier = Modifier.size(18.dp))
        }
    }
}
