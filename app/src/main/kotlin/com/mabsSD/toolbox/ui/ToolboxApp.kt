package com.mabsSD.toolbox.ui

import android.net.Uri
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mabsSD.toolbox.ui.components.DockDestination
import com.mabsSD.toolbox.ui.components.DockHeight
import com.mabsSD.toolbox.ui.components.DockMargin
import com.mabsSD.toolbox.ui.components.FloatingDock
import com.mabsSD.toolbox.ui.components.LocalDockClearance
import com.mabsSD.toolbox.ui.components.popEnter
import com.mabsSD.toolbox.ui.components.popExit
import com.mabsSD.toolbox.ui.components.pushEnter
import com.mabsSD.toolbox.ui.components.pushExit
import com.mabsSD.toolbox.ui.components.tabEnter
import com.mabsSD.toolbox.ui.components.tabExit
import com.mabsSD.toolbox.ui.screens.CompressScreen
import com.mabsSD.toolbox.ui.screens.FilesScreen
import com.mabsSD.toolbox.ui.screens.HomeScreen
import com.mabsSD.toolbox.ui.screens.IncomingScreen
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
    data object Incoming : Screen("incoming")
    data object Tool : Screen("tool/{toolId}") {
        fun createRoute(toolId: String) = "tool/$toolId"
    }
    data object Result : Screen("result/{outputUri}") {
        fun createRoute(outputUri: String) = "result/${Uri.encode(outputUri)}"
    }
}

/**
 * Only these four show the dock.
 *
 * Deliberately a top-level property, not a member of Screen's own companion
 * object: a sealed class's companion referencing its own sibling nested
 * objects hits a JVM static-initialization-order bug — the companion's
 * <clinit> can run before the sibling data objects are loaded, leaving this
 * list holding nulls. Crashed with a NullPointerException on first launch.
 */
private val topLevelRoutes = setOf(Screen.Home.route, Screen.Files.route, Screen.Tools.route, Screen.Settings.route)

private val dockDestinations = listOf(
    DockDestination(Screen.Home.route, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    DockDestination(Screen.Files.route, "Files", Icons.Outlined.Folder, Icons.Rounded.Folder),
    DockDestination(Screen.Tools.route, "Tools", Icons.Outlined.GridView, Icons.Rounded.GridView),
    DockDestination(Screen.Settings.route, "Settings", Icons.Outlined.Settings, Icons.Rounded.Settings),
)

/** Landscape screens at least this wide get the side rail instead of the bottom dock. */
private const val WIDE_SCREEN_DP = 600

/** Fixed slot for the rail: its ~62dp pill plus margin either side. */
private val RailSlotWidth = 84.dp

/** Content never stretches past this on tablets; lines and cards stay readable. */
private val MaxContentWidth = 840.dp

private fun isTopLevel(route: String?) = route in topLevelRoutes

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
    // Side rail only when the screen is both wide and landscape-shaped (tablet
    // or phone turned sideways). A tablet held upright gets the bottom dock,
    // like a phone: the rail there wasted width and sat far from the thumb.
    val config = LocalConfiguration.current
    val rail = config.screenWidthDp >= WIDE_SCREEN_DP && config.screenWidthDp > config.screenHeightDp

    // Like the desktop dock's auto-hide: scrolling content down tucks the dock
    // away, scrolling back up brings it back. Every new screen starts with it shown.
    var dockShown by remember { mutableStateOf(true) }
    LaunchedEffect(currentRoute) { dockShown = true }
    val hideOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6f) dockShown = false else if (available.y > 6f) dockShown = true
                return Offset.Zero
            }
        }
    }
    val goTo: (DockDestination) -> Unit = { dest ->
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val scan = { navController.navigate(Screen.Tool.createRoute("scan")) }

    val toResult = { navController.navigate(Screen.Result.createRoute("last")) }

    // A share or "Open with" from another app: offer the tools that fit.
    // Same instance MainActivity feeds, since both are scoped to the activity.
    val incomingVm: IncomingViewModel = viewModel()
    val incoming by incomingVm.batch.collectAsState()
    val showChooser by incomingVm.showChooser.collectAsState()
    LaunchedEffect(showChooser) {
        if (showChooser) {
            incomingVm.chooserShown()
            navController.navigate(Screen.Incoming.route) { launchSingleTop = true }
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Landscape on wide screens: the dock stands upright as a rail. Its slot
        // keeps a fixed width even while the rail is hidden on tool screens:
        // letting the slot collapse changed the content's width mid-transition,
        // so every screen reflowed sideways as it slid in or out.
        if (rail) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                    .width(RailSlotWidth),
            ) {
                // Fully qualified: the bare name resolves to the outer Row's RowScope variant.
                androidx.compose.animation.AnimatedVisibility(
                    visible = onTopLevelScreen,
                    enter = slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = 400f)) { -it * 2 } + fadeIn(),
                    exit = slideOutHorizontally(spring(stiffness = 600f)) { -it * 2 } + fadeOut(),
                ) {
                    FloatingDock(dockDestinations, currentRoute, goTo, scan, vertical = true)
                }
            }
        }

        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .nestedScroll(hideOnScroll),
        ) {
          CompositionLocalProvider(
              LocalDockClearance provides if (rail) 0.dp else DockHeight + DockMargin + 16.dp,
          ) {
            NavHost(
            navController = navController,
            startDestination = if (seen) Screen.Home.route else Screen.Onboarding.route,
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxSize(),
            // Tabs cross-fade; anything opened from them is pushed on and popped off.
            enterTransition = { if (isTopLevel(initialState.destination.route) && isTopLevel(targetState.destination.route)) tabEnter() else pushEnter() },
            exitTransition = { if (isTopLevel(initialState.destination.route) && isTopLevel(targetState.destination.route)) tabExit() else pushExit() },
            popEnterTransition = { if (isTopLevel(initialState.destination.route) && isTopLevel(targetState.destination.route)) tabEnter() else popEnter() },
            popExitTransition = { if (isTopLevel(initialState.destination.route) && isTopLevel(targetState.destination.route)) tabExit() else popExit() },
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

            composable(Screen.Incoming.route) {
                val batch = incoming
                if (batch == null) {
                    // Only after Android killed and restored the app: the shared
                    // copies are gone with the old process, so there's nothing to offer.
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                IncomingScreen(
                    batch = batch,
                    onToolChosen = { toolId ->
                        navController.navigate(Screen.Tool.createRoute(toolId)) {
                            popUpTo(Screen.Incoming.route) { inclusive = true }
                        }
                        // navigate() updates the back stack synchronously, so this
                        // is the new tool screen's entry, before it first composes.
                        navController.currentBackStackEntry?.let { incomingVm.handTo(it.id, batch.files) }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.Tool.route,
                arguments = listOf(navArgument("toolId") { type = NavType.StringType })
            ) { entry ->
                val toolId = entry.arguments?.getString("toolId") ?: return@composable
                val back = { navController.popBackStack(); Unit }
                // Files shared from another app, if that's how we got here.
                val shared = remember(entry) { incomingVm.filesFor(entry.id) }

                // Tools whose input cannot be expressed as "pick one file" get
                // their own screen: scan is a camera flow, split needs a page
                // range, merge needs several files in a chosen order.
                when (toolId) {
                    "scan" -> ScanScreen(onNavigateToResult = toResult, onBack = back, initialImages = shared)
                    "split" -> SplitScreen(onNavigateToResult = toResult, onBack = back, initialPdf = shared.firstOrNull())
                    "merge" -> MergeScreen(onNavigateToResult = toResult, onBack = back, initialPdfs = shared)
                    "compress" -> CompressScreen(onNavigateToResult = toResult, onBack = back, initialFile = shared.firstOrNull())
                    else -> ToolScreen(toolId = toolId, onNavigateToResult = toResult, onBack = back, initialFile = shared.firstOrNull())
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

            // Phones: the dock floats over the bottom of the content. It leaves
            // with a spring when a tool opens (as iOS hides its tab bar on push)
            // and while scrolling down.
            if (!rail) {
                // Fully qualified: inside the Row, the bare name resolves to RowScope's variant.
                androidx.compose.animation.AnimatedVisibility(
                    visible = onTopLevelScreen && dockShown,
                    enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = 420f)) { it * 2 } + fadeIn(),
                    exit = slideOutVertically(spring(stiffness = 600f)) { it * 2 } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = DockMargin),
                ) {
                    FloatingDock(dockDestinations, currentRoute, goTo, scan, vertical = false)
                }
            }
        }
    }
}
