package com.mabsSD.toolbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mabsSD.toolbox.tools.ToolRegistry

data class ToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

/**
 * Tools that own a dedicated screen instead of running through ToolRegistry.
 * Scan is interactive (camera, page tray) so it never fitted the
 * input -> run -> output shape the registry expects.
 */
private val screenRoutedTools = setOf("scan")

val defaultTools = listOf(
    ToolItem("scan", "Scan", "Camera to PDF", Icons.Default.CameraAlt),
    ToolItem("split", "Split", "Extract pages", Icons.Default.Splitscreen),
    ToolItem("merge", "Merge", "Combine PDFs", Icons.AutoMirrored.Filled.MergeType),
    ToolItem("compress", "Compress", "Hit a size limit", Icons.Default.Compress),
    ToolItem("ocr", "OCR", "Copy text out", Icons.Default.TextFields),
    ToolItem("copy_file", "Copy File", "Duplicate a file", Icons.Default.ContentCopy),
)

/**
 * A tool is tappable only if something is actually behind it. Deriving this from
 * the registry rather than a second hardcoded list is what stops the grid from
 * advertising tools that resolve to "Unknown tool" when tapped.
 */
private fun isAvailable(id: String): Boolean =
    id in screenRoutedTools || ToolRegistry.get(id) != null

@Composable
fun HomeScreen(
    onToolSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = inner.calculateTopPadding(),
                bottom = inner.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { Header() }

            items(defaultTools, key = { it.id }) { tool ->
                ToolCard(
                    tool = tool,
                    enabled = isAvailable(tool.id),
                    onClick = { onToolSelected(tool.id) }
                )
            }
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 24.dp, bottom = 20.dp)) {
        Text(
            text = "Toolbox",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        PrivacyBadge()
    }
}

/**
 * The privacy claim, stated once, at the top. It is the product's whole reason to
 * exist, so it gets to be the first thing on the screen rather than a settings
 * footnote nobody reads.
 */
@Composable
private fun PrivacyBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(percent = 50)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.size(7.dp))
        Text(
            text = "Nothing leaves your phone",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun ToolCard(
    tool: ToolItem,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val outline = MaterialTheme.colorScheme.outline

    Card(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .border(1.dp, outline, shape)
            .semantics {
                if (!enabled) stateDescription = "Coming soon"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            // Disabled cards recede into the page rather than sitting proud of it,
            // so "not ready" is legible before the label is read.
            disabledContainerColor = MaterialTheme.colorScheme.background,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            IconTile(icon = tool.icon, enabled = enabled)

            Column {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (enabled) tool.subtitle else "Coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector, enabled: Boolean, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .background(
                color = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(13.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp)
        )
    }
}
