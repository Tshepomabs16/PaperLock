package com.mabsSD.toolbox.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mabsSD.toolbox.tools.ResultStore
import com.mabsSD.toolbox.tools.ToolResult
import com.mabsSD.toolbox.tools.ToolRegistry
import com.mabsSD.toolbox.ui.components.FileHistoryCard
import com.mabsSD.toolbox.ui.toolboxContainer

data class ToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

/** Featured on Home; the full set (including Copy File) lives on the Tools tab. */
val quickTools = listOf(
    ToolItem("split", "Split", "Extract pages", Icons.Default.Splitscreen),
    ToolItem("merge", "Merge", "Combine PDFs", Icons.AutoMirrored.Filled.MergeType),
    ToolItem("compress", "Compress", "Hit a size limit", Icons.Default.Compress),
    ToolItem("ocr", "OCR", "Copy text out", Icons.Default.TextFields),
)

/** The full catalogue, used by the Tools tab and shared availability logic. */
val allTools = listOf(ToolItem("scan", "Scan", "Camera to PDF", Icons.Default.CameraAlt)) +
    quickTools +
    listOf(ToolItem("copy_file", "Copy File", "Duplicate a file", Icons.Default.ContentCopy))

/**
 * A tool is tappable only if something is actually behind it. Deriving this from
 * the registry rather than a second hardcoded list is what stops the grid from
 * advertising tools that resolve to "Unknown tool" when tapped.
 */
fun isToolAvailable(id: String): Boolean = id == "scan" || ToolRegistry.get(id) != null

@Composable
fun HomeScreen(
    onToolSelected: (String) -> Unit,
    onSeeAllFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { context.toolboxContainer() }
    val recent by container.historyDb.historyDao().observeRecent(5)
        .collectAsState(initial = emptyList())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = inner.calculateTopPadding(),
                bottom = inner.calculateBottomPadding() + 96.dp, // clears the FAB
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Header()
                Spacer(Modifier.height(20.dp))
                ScanButton(onClick = { onToolSelected("scan") })
                Spacer(Modifier.height(28.dp))
                Text(
                    "Quick tools",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                QuickToolsGrid(onToolSelected = onToolSelected)
                Spacer(Modifier.height(28.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Recent",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (recent.isNotEmpty()) {
                        TextButton(onClick = onSeeAllFiles) { Text("See all") }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (recent.isEmpty()) {
                item { RecentEmptyState(onScan = { onToolSelected("scan") }) }
            } else {
                items(recent, key = { it.id }) { entry ->
                    FileHistoryCard(
                        entry = entry,
                        onClick = {
                            ResultStore.updateLastResult(
                                ToolResult(
                                    outputUri = Uri.parse(entry.outputUri),
                                    outputName = entry.outputName,
                                    outputSize = entry.outputSize,
                                )
                            )
                            onSeeAllFiles()
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 24.dp, bottom = 4.dp)) {
        Text(
            "Toolbox",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        OfflineBadge()
    }
}

/** The privacy claim, stated once, at the top of the app's main screen. */
@Composable
private fun OfflineBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Offline • Private",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun ScanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Scan document",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun QuickToolsGrid(onToolSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(184.dp), // two rows; nested inside the outer LazyColumn's scroll
    ) {
        itemsIndexed(quickTools, span = { _, _ -> GridItemSpan(1) }) { _, tool ->
            QuickToolCard(tool = tool, onClick = { onToolSelected(tool.id) })
        }
    }
}

@Composable
private fun QuickToolCard(tool: ToolItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth().height(86.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
            ) {
                Icon(
                    tool.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(tool.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    tool.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RecentEmptyState(onScan: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your recent files will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onScan) { Text("Scan a document") }
    }
}

