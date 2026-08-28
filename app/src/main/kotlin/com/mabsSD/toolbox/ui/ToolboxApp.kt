package com.mabsSD.toolbox.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mabsSD.toolbox.ui.screens.HomeScreen
import com.mabsSD.toolbox.ui.screens.ResultScreen
import com.mabsSD.toolbox.ui.screens.ScanScreen
import com.mabsSD.toolbox.ui.screens.ToolScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Tool : Screen("tool/{toolId}") {
        fun createRoute(toolId: String) = "tool/$toolId"
    }
    data object Result : Screen("result/{outputUri}") {
        fun createRoute(outputUri: String) = "result/${Uri.encode(outputUri)}"
    }
}

@Composable
fun ToolboxApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier.fillMaxSize()
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onToolSelected = { toolId ->
                    navController.navigate(Screen.Tool.createRoute(toolId))
                }
            )
        }

        composable(
            route = Screen.Tool.route,
            arguments = listOf(
                navArgument("toolId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val toolId = backStackEntry.arguments?.getString("toolId") ?: return@composable
            if (toolId == "scan") {
                ScanScreen(
                    onNavigateToResult = { navController.navigate(Screen.Result.createRoute("last")) },
                    onBack = { navController.popBackStack() }
                )
            } else {
                ToolScreen(
                    toolId = toolId,
                    onNavigateToResult = { navController.navigate(Screen.Result.createRoute("last")) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("outputUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val outputUri = backStackEntry.arguments?.getString("outputUri") ?: return@composable
            ResultScreen(
                outputUriArg = outputUri,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
