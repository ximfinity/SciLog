package com.scilog.app.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SpeedDialFab(
    onLogShot: () -> Unit,
    onLogWeight: () -> Unit,
    onInventory: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpeedDialItem("Inventory", Icons.Outlined.Science) { onInventory(); expanded = false }
                SpeedDialItem("Log Weight", Icons.Outlined.FitnessCenter) { onLogWeight(); expanded = false }
                SpeedDialItem("Log Shot", Icons.Outlined.Vaccines) { onLogShot(); expanded = false }
            }
        }
        FloatingActionButton(onClick = { expanded = !expanded }) {
            Icon(
                if (expanded) Icons.Outlined.Close else Icons.Outlined.Add,
                contentDescription = if (expanded) "Close" else "Actions"
            )
        }
    }
}

@Composable
private fun SpeedDialItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        SmallFloatingActionButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
    }
}
