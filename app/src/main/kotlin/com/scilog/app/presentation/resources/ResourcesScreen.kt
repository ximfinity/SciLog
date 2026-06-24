package com.scilog.app.presentation.resources

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class ResourceLink(val title: String, val description: String, val url: String, val isFeatured: Boolean = false)

private val resources = listOf(
    ResourceLink(
        title = "The Fat Scientist",
        description = "Evidence-based GLP-1 information, dosing guides, and community insights.",
        url = "https://www.fatscientist.com/",
        isFeatured = true
    ),
    ResourceLink(
        title = "r/tirzepatidecompound",
        description = "Compounded tirzepatide community — dosing protocols, sourcing tips, and experiences.",
        url = "https://www.reddit.com/r/tirzepatidecompound/",
        isFeatured = true
    ),
    ResourceLink(
        title = "Dose Timing Calculator",
        description = "Estimate your next dose timing based on your current schedule.",
        url = "https://www.fatscientist.com/"
    ),
    ResourceLink(
        title = "r/Semaglutide",
        description = "Community experiences, tips, and research.",
        url = "https://www.reddit.com/r/Semaglutide/"
    ),
    ResourceLink(
        title = "r/Tirzepatide",
        description = "Tirzepatide community resources and experiences.",
        url = "https://www.reddit.com/r/Tirzepatide/"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resources & Tools") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(resources) { resource ->
                val isFeatured = resource.isFeatured
                Card(
                    onClick = { openUrl(resource.url) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFeatured) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    resource.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isFeatured) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isFeatured) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("Featured", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Text(
                                resource.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = (if (isFeatured) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.75f)
                            )
                        }
                        Icon(
                            Icons.Outlined.OpenInBrowser,
                            null,
                            tint = if (isFeatured) MaterialTheme.colorScheme.onPrimaryContainer.copy(0.6f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
                        )
                    }
                }
            }
        }
    }
}
