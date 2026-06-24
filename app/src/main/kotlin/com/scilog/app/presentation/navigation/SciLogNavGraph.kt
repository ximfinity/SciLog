package com.scilog.app.presentation.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.scilog.app.presentation.auth.AuthScreen
import com.scilog.app.presentation.chart.DecayChartScreen
import com.scilog.app.presentation.config.ConfigScreen
import com.scilog.app.presentation.dashboard.DashboardScreen
import com.scilog.app.presentation.dosage.CatchUpDoseScreen
import com.scilog.app.presentation.history.HistoryTabScreen
import com.scilog.app.presentation.import_.ImportScreen
import com.scilog.app.presentation.inventory.InventoryScreen
import com.scilog.app.presentation.more.MoreScreen
import com.scilog.app.presentation.resources.ResourcesScreen
import com.scilog.app.presentation.shots.LogShotScreen
import com.scilog.app.presentation.theme.LocalAppIsDark
import com.scilog.app.presentation.theme.SciLogTheme
import com.scilog.app.presentation.weight.WeightScreen

private val TAB_ROUTES = setOf("tab_home", "tab_chart", "tab_history", "tab_more")

sealed class Screen(val route: String) {
    object Auth     : Screen("auth")
    object LogShot  : Screen("log_shot?shotId={shotId}") {
        fun forNew()          = "log_shot?shotId=-1"
        fun forEdit(id: Long) = "log_shot?shotId=$id"
    }
    object Weight      : Screen("weight")
    object Inventory   : Screen("inventory")
    object Config      : Screen("config")
    object Resources   : Screen("resources")
    object Import      : Screen("import")
    object CatchUpDose : Screen("catch_up_dose")
    object ShotHistory : Screen("shot_history")   // legacy alias → redirects to tab_history
}

@Composable
fun SciLogNavGraph() {
    val navController = rememberNavController()
    val systemDark    = isSystemInDarkTheme()
    var isDarkTheme by rememberSaveable { mutableStateOf(systemDark) }

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute  = currentEntry?.destination?.route
    val showBottomBar = currentRoute in TAB_ROUTES

    CompositionLocalProvider(LocalAppIsDark provides isDarkTheme) {
        SciLogTheme(darkTheme = isDarkTheme) {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        BottomNavBar(navController = navController, currentRoute = currentRoute)
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController    = navController,
                    startDestination = Screen.Auth.route,
                    modifier         = Modifier.padding(innerPadding)
                ) {
                    // ── Auth ────────────────────────────────────────────────
                    composable(Screen.Auth.route) {
                        AuthScreen(onAuthenticated = {
                            navController.navigate("tab_home") {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        })
                    }

                    // ── Tab: Home ────────────────────────────────────────────
                    composable("tab_home") {
                        DashboardScreen(
                            isDarkTheme             = isDarkTheme,
                            onToggleTheme           = { isDarkTheme = !isDarkTheme },
                            onNavigateToChart       = { navController.navigate("tab_chart") { launchSingleTop = true } },
                            onNavigateToLogShot     = { navController.navigate(Screen.LogShot.forNew()) },
                            onNavigateToEditShot    = { id -> navController.navigate(Screen.LogShot.forEdit(id)) },
                            onNavigateToShotHistory = { navController.navigate("tab_history") { launchSingleTop = true } },
                            onNavigateToWeight      = { navController.navigate(Screen.Weight.route) },
                            onNavigateToInventory   = { navController.navigate(Screen.Inventory.route) },
                            onNavigateToConfig      = { navController.navigate(Screen.Config.route) },
                            onNavigateToResources   = { navController.navigate(Screen.Resources.route) },
                            onNavigateToImport      = { navController.navigate(Screen.Import.route) },
                            onNavigateToCatchUpDose = { navController.navigate(Screen.CatchUpDose.route) }
                        )
                    }

                    // ── Tab: Chart ───────────────────────────────────────────
                    composable("tab_chart") {
                        DecayChartScreen(onBack = null)
                    }

                    // ── Tab: History ─────────────────────────────────────────
                    composable("tab_history") {
                        HistoryTabScreen(
                            onNavigateToLogShot = { navController.navigate(Screen.LogShot.forNew()) },
                            onEditShot          = { id -> navController.navigate(Screen.LogShot.forEdit(id)) },
                            onNavigateToWeight  = { navController.navigate(Screen.Weight.route) }
                        )
                    }

                    // ── Tab: More ────────────────────────────────────────────
                    composable("tab_more") {
                        MoreScreen(
                            isDarkTheme             = isDarkTheme,
                            onToggleTheme           = { isDarkTheme = !isDarkTheme },
                            onNavigateToConfig      = { navController.navigate(Screen.Config.route) },
                            onNavigateToInventory   = { navController.navigate(Screen.Inventory.route) },
                            onNavigateToResources   = { navController.navigate(Screen.Resources.route) },
                            onNavigateToImport      = { navController.navigate(Screen.Import.route) },
                            onNavigateToCatchUpDose = { navController.navigate(Screen.CatchUpDose.route) }
                        )
                    }

                    // ── Secondary screens ────────────────────────────────────
                    composable(
                        route     = Screen.LogShot.route,
                        arguments = listOf(navArgument("shotId") {
                            type         = NavType.LongType
                            defaultValue = -1L
                        })
                    ) {
                        LogShotScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Weight.route) {
                        WeightScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Inventory.route) {
                        InventoryScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Config.route) {
                        ConfigScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Resources.route) {
                        ResourcesScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Import.route) {
                        ImportScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.CatchUpDose.route) {
                        CatchUpDoseScreen(onBack = { navController.popBackStack() })
                    }
                    // Legacy route redirects to history tab
                    composable(Screen.ShotHistory.route) {
                        LaunchedEffect(Unit) {
                            navController.navigate("tab_history") {
                                popUpTo(Screen.ShotHistory.route) { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    }
}
