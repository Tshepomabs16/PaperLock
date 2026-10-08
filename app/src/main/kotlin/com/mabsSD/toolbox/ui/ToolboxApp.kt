package com.mabsSD.toolbox.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mabsSD.toolbox.ui.screens.CompressScreen
import com.mabsSD.toolbox.ui.screens.FilesScreen
import com.mabsSD.toolbox.ui.screens.HomeScreen
import com.mabsSD.toolbox.ui.screens.MergeScreen
import com.mabsSD.toolbox.ui.screens.OnboardingScreen
import com.mabsSD.toolbox.ui.screens.ResultScreen
import com.mabsSD.toolbox.ui.screens.ScanScreen
import com.mabsSD.toolbox.ui.screens.SettingsScreen
import com.mabsSD.toolbox.ui.screens.SplitScreen
import com.mabsSD.toolbox.ui.screens.ToolScreen
import com.mabsSD.toolbox.ui.screens.ToolsScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Files : Screen("files")
    data object Tools : Screen("tools")
    data object Settings : Screen("settings")
    data object Tool : Screen("tool/{toolId}") {
        fun createRoute(toolId: String) = "tool/$toolId"
    }
    data object Result : Screen("result/{outputUri}") {
        fun createRoute(outputUri: String) = "result/${Uri.encode(outputUri)}"
    }
}

/**
 * Only these four show the bottom nav and the scan FAB.
 *
 * Deliberately a top-level property, not a member of Screen's own companion
 * object: a sealed class's companion referencing its own sibling nested
 * objects hits a JVM static-initialization-order bug — the companion's
 * <clinit> can run before the sibling data objects are loaded, leaving this
 * list holding nulls. Crashed with a NullPointerException on first launch.
 */
private val topLevelRoutes = setOf(Screen.Home.route, Screen.Files.route, Screen.Tools.route, Screen.Settings.route)

private data class BottomNavSpec(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavSpec(Screen.Home, "Home", Icons.Default.Home),
    BottomNavSpec(Screen.Files, "Files", Icons.Default.Folder),
    BottomNavSpec(Screen.Tools, "Tools", Icons.Default.Build),
    BottomNavSpec(Screen.Settings, "Settings", Icons.Default.Settings),
)

@Composable
fun ToolboxApp(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { context.toolboxContainer() }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    // Resolving asynchronously avoids a one-frame flash of the wrong start
    // destination while the preference loads.
    val seenOnboarding by container.preferences.hasSeenOnboarding.collectAsState(initial = null)
    val seen = seenOnboarding ?: return

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTopLevelScreen = currentRoute in topLevelRoutes
    val showScanFab = currentRoute == Screen.Home.route || currentRoute == Screen.Files.route

    val toResult = { navController.navigate(Screen.Result.createRoute("last")) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (onTopLevelScreen) {
                ToolboxBottomNav(currentRoute = currentRoute, navController = navController)
            }
        },
        floatingActionButton = {
            if (showScanFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.Tool.createRoute("scan")) },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Scan document")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (seen) Screen.Home.route else Screen.Onboarding.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onDone = {
                        scope.launch { container.preferences.setOnboardingSeen() }
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onToolSelected = { toolId -> navController.navigate(Screen.Tool.createRoute(toolId)) },
                    onSeeAllFiles = { navController.navigate(Screen.Files.route) },
                )
            }

            composable(Screen.Files.route) {
                FilesScreen(
                    onOpenResult = { toResult() },
                )
            }

            composable(Screen.Tools.route) {
                ToolsScreen(onToolSelected = { toolId -> navController.navigate(Screen.Tool.createRoute(toolId)) })
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.Tool.route,
                arguments = listOf(navArgument("toolId") { type = NavType.StringType })
            ) { entry ->
                val toolId = entry.arguments?.getString("toolId") ?: return@composable
                val back = { navController.popBackStack(); Unit }

                // Tools whose input cannot be expressed as "pick one file" get
                // their own screen: scan is a camera flow, split needs a page
                // range, merge needs several files in a chosen order.
                when (toolId) {
                    "scan" -> ScanScreen(onNavigateToResult = toResult, onBack = back)
                    "split" -> SplitScreen(onNavigateToResult = toResult, onBack = back)
                    "merge" -> MergeScreen(onNavigateToResult = toResult, onBack = back)
                    "compress" -> CompressScreen(onNavigateToResult = toResult, onBack = back)
                    else -> ToolScreen(toolId = toolId, onNavigateToResult = toResult, onBack = back)
                }
            }

            composable(
                route = Screen.Result.route,
                arguments = listOf(navArgument("outputUri") { type = NavType.StringType })
            ) { entry ->
                val outputUri = entry.arguments?.getString("outputUri") ?: return@composable
                ResultScreen(
                    outputUriArg = outputUri,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun ToolboxBottomNav(
    currentRoute: String?,
    navController: androidx.navigation.NavHostController,
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.screen.route,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}
