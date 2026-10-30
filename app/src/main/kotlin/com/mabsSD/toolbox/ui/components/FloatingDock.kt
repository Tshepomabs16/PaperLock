package com.mabsSD.toolbox.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A top-level destination in the dock: outlined icon at rest, filled when selected. */
data class DockDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/** Height of the horizontal dock, not counting its margin above the system bar. */
val DockHeight: Dp = 60.dp
val DockMargin: Dp = 12.dp

/**
 * Space a top-level screen should leave at the bottom of its scrolling content
 * so the last item can scroll clear of the floating dock. Zero when the dock
 * is a side rail (tablets, landscape), which doesn't cover content.
 */
val LocalDockClearance = compositionLocalOf { 0.dp }

/**
 * Floating navigation in the style of a desktop dock: a translucent pill
 * holding icons only, a short amber dash under the active one, and the Scan
 * action set apart at the end the way a dock keeps its "show apps" button.
 *
 * [vertical] lays it out as a rail for wide screens.
 */
@Composable
fun FloatingDock(
    destinations: List<DockDestination>,
    currentRoute: String?,
    onSelect: (DockDestination) -> Unit,
    onScan: () -> Unit,
    vertical: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(percent = 50),
        // Slightly see-through, like the desktop dock; content scrolls beneath it.
        color = scheme.surfaceContainerHigh.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.6f)),
        shadowElevation = 10.dp,
        modifier = modifier,
    ) {
        val items: @Composable () -> Unit = {
            destinations.forEach { dest ->
                DockButton(dest, selected = currentRoute == dest.route, vertical = vertical, onClick = { onSelect(dest) })
            }
            Separator(vertical)
            ScanButton(onScan)
        }
        if (vertical) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            ) { items() }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.height(DockHeight).padding(horizontal = 8.dp),
            ) { items() }
        }
    }
}

@Composable
private fun DockButton(dest: DockDestination, selected: Boolean, vertical: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val view = LocalView.current
    val source = remember { MutableInteractionSource() }
    // The selected icon settles in with a little spring; the dash grows from nothing.
    val iconScale by animateFloatAsState(if (selected) 1f else 0.9f, Motion.settle, label = "dockIcon")
    val dash by animateFloatAsState(if (selected) 1f else 0f, Motion.settle, label = "dockDash")

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(if (vertical) 48.dp else 54.dp, if (vertical) 54.dp else 48.dp)
            .then(rememberPressScale(source, pressedScale = 0.82f))
            .clip(CircleShape)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = ripple(),
                role = Role.Tab,
                onClick = {
                    if (!selected) view.tick()
                    onClick()
                },
            ),
    ) {
        Icon(
            if (selected) dest.selectedIcon else dest.icon,
            contentDescription = dest.label,
            tint = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
        )
        // The "running app" dash: under the icon in the dock, beside it in the rail.
        Box(
            Modifier
                .align(if (vertical) Alignment.CenterStart else Alignment.BottomCenter)
                .then(if (vertical) Modifier.padding(start = 2.dp) else Modifier.padding(bottom = 3.dp))
                .size(if (vertical) 3.dp else 16.dp * dash, if (vertical) 16.dp * dash else 3.dp)
                .graphicsLayer { alpha = dash }
                .background(scheme.tertiary, CircleShape),
        )
    }
}

@Composable
private fun Separator(vertical: Boolean) {
    val color = MaterialTheme.colorScheme.outline
    if (vertical) {
        Spacer(Modifier.padding(vertical = 4.dp).size(24.dp, 1.dp).background(color))
    } else {
        Spacer(Modifier.padding(horizontal = 4.dp).size(1.dp, 24.dp).background(color))
    }
}

@Composable
private fun ScanButton(onScan: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(4.dp)
            .size(44.dp)
            .then(rememberPressScale(source, pressedScale = 0.85f))
            .clip(CircleShape)
            .background(scheme.tertiary)
            .clickable(
                interactionSource = source,
                indication = ripple(color = scheme.onTertiary),
                role = Role.Button,
                onClick = onScan,
            ),
    ) {
        Icon(Icons.Rounded.CameraAlt, contentDescription = "Scan document", tint = scheme.onTertiary, modifier = Modifier.size(22.dp))
    }
}
