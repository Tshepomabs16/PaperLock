package com.mabsSD.toolbox.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** (section header, the tools that belong under it) */
private val sections: List<Pair<String, List<ToolItem>>> = listOf(
    "Capture" to allTools.filter { it.id == "scan" },
    "Organize" to allTools.filter { it.id in setOf("split", "merge") },
    "Optimize" to allTools.filter { it.id == "compress" },
    "Text" to allTools.filter { it.id == "ocr" },
    "Utility" to allTools.filter { it.id == "copy_file" },
)

@Composable
fun ToolsScreen(
    onToolSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
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
                top = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Tools",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            sections.forEach { (label, tools) ->
                if (tools.isEmpty()) return@forEach

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                tools.forEach { tool ->
                    item {
                        ToolGridCard(
                            tool = tool,
                            enabled = isToolAvailable(tool.id),
                            onClick = { onToolSelected(tool.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolGridCard(
    tool: ToolItem,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) scheme.surface else scheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 1.dp else 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .semantics { if (!enabled) stateDescription = "Coming soon" },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (enabled) scheme.primaryContainer else scheme.surface,
                        MaterialTheme.shapes.medium,
                    ),
            ) {
                Icon(
                    tool.icon,
                    contentDescription = null,
                    tint = if (enabled) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column {
                Text(
                    tool.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
                )
                Text(
                    if (enabled) tool.subtitle else "Coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
