package com.scilog.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    BottomNavItem("tab_home",    "Home",    Icons.Outlined.Home),
    BottomNavItem("tab_chart",   "Chart",   Icons.Outlined.ShowChart),
    BottomNavItem("tab_history", "History", Icons.Outlined.List),
    BottomNavItem("tab_more",    "More",    Icons.Outlined.MoreHoriz)
)

@Composable
fun BottomNavBar(navController: NavController, currentRoute: String?) {
    NavigationBar {
        navItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
