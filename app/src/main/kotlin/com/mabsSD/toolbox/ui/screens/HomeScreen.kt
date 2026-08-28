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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
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
import com.mabsSD.toolbox.ui.theme.BorderWidth
import com.mabsSD.toolbox.ui.theme.BorderWidthThin

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
    val readyCount = defaultTools.count { isAvailable(it.id) }

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
                bottom = inner.calculateBottomPadding() + 32.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Masthead(total = defaultTools.size, ready = readyCount)
            }

            itemsIndexed(defaultTools, key = { _, t -> t.id }) { index, tool ->
                ToolCard(
                    tool = tool,
                    index = index + 1,
                    enabled = isAvailable(tool.id),
                    // One accent block per screen. The first available tool is the
                    // primary action; everything else is an outline.
                    accent = isAvailable(tool.id) && index == 0,
                    onClick = { onToolSelected(tool.id) }
                )
            }
        }
    }
}

@Composable
private fun Masthead(total: Int, ready: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 28.dp, bottom = 22.dp)) {
        StatusLine()

        Spacer(Modifier.height(18.dp))

        Text(
            text = "TOOLBOX",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(16.dp))

        // Full-bleed rule. The heavy horizontal line is what anchors the
        // oversized wordmark and separates masthead from grid without a gap.
        Box(
            Modifier
                .fillMaxWidth()
                .height(BorderWidth)
                .background(MaterialTheme.colorScheme.onBackground)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "$total TOOLS — $ready READY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The privacy claim, stated once, at the top. It is the product's whole reason to
 * exist, so it leads the screen rather than sitting in a settings footnote.
 */
@Composable
private fun StatusLine(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            Modifier
                .size(7.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = "OFFLINE — NOTHING LEAVES YOUR PHONE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun ToolCard(
    tool: ToolItem,
    index: Int,
    enabled: Boolean,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    val container = when {
        accent -> scheme.primary
        enabled -> scheme.surface
        else -> scheme.background
    }
    val content = when {
        accent -> scheme.onPrimary
        enabled -> scheme.onSurface
        else -> scheme.onSurfaceVariant
    }
    // Disabled blocks drop to a hairline in the faint tone so the grid still reads
    // as a grid, but unbuilt tools visibly recede behind the live ones.
    val borderColor = if (enabled) scheme.onBackground else scheme.outlineVariant
    val borderWidth = if (enabled) BorderWidth else BorderWidthThin

    Card(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .height(158.dp)
            .border(borderWidth, borderColor, MaterialTheme.shapes.medium)
            .semantics { if (!enabled) stateDescription = "Coming soon" },
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = index.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (accent) content else scheme.onSurfaceVariant,
                )
            }

            Column {
                Text(
                    text = tool.title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = content,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (enabled) tool.subtitle else "COMING SOON",
                        style = if (enabled) {
                            MaterialTheme.typography.bodySmall
                        } else {
                            MaterialTheme.typography.labelSmall
                        },
                        color = if (accent) {
                            content.copy(alpha = 0.85f)
                        } else {
                            scheme.onSurfaceVariant
                        },
                    )
                    if (accent) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = content,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
