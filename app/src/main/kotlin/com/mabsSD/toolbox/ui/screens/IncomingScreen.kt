package com.mabsSD.toolbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mabsSD.toolbox.tools.ToolInputType
import com.mabsSD.toolbox.ui.IncomingBatch
import com.mabsSD.toolbox.ui.components.BorderedBlock
import com.mabsSD.toolbox.ui.components.SectionLabel
import com.mabsSD.toolbox.ui.components.ToolboxTopBar

/** (tool id, why you'd pick it for these files) */
private typealias Choice = Pair<String, String>

/**
 * The tools that can take exactly these files. Every tool here works on one
 * kind of input, so a mix of PDFs and images has no single tool to offer.
 */
internal fun choicesFor(batch: IncomingBatch): List<Choice> {
    val types = batch.files.map { it.type }.toSet()
    val many = batch.files.size > 1
    return when {
        types == setOf(ToolInputType.PDF) && many -> listOf(
            "merge" to "Join these ${batch.files.size} PDFs into one",
        )
        types == setOf(ToolInputType.PDF) -> listOf(
            "split" to "Pull selected pages out into a new PDF",
            "compress" to "Shrink it under a size limit",
            "merge" to "Join it with other PDFs",
        )
        types == setOf(ToolInputType.IMAGE) && many -> listOf(
            "scan" to "Turn these ${batch.files.size} images into one PDF",
        )
        types == setOf(ToolInputType.IMAGE) -> listOf(
            "scan" to "Turn it into a PDF, with a clean B&W filter",
            "ocr" to "Copy the text out of it",
            "compress" to "Shrink it under a size limit",
        )
        else -> emptyList()
    }
}

@Composable
fun IncomingScreen(
    batch: IncomingBatch,
    onToolChosen: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val choices = choicesFor(batch)

    Scaffold(
        topBar = { ToolboxTopBar(title = "Open with Toolbox", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            SectionLabel(if (batch.files.size == 1) "1 file" else "${batch.files.size} files")
            Spacer(Modifier.height(8.dp))
            batch.files.forEach { file ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Icon(
                        if (file.type == ToolInputType.PDF) Icons.Default.PictureAsPdf else Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        file.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (batch.skipped > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (batch.skipped == 1) "1 file couldn't be read, or isn't a PDF or image, and was left out."
                    else "${batch.skipped} files couldn't be read, or aren't PDFs or images, and were left out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))

            if (choices.isEmpty()) {
                BorderedBlock {
                    Text(
                        if (batch.files.isEmpty()) "Toolbox couldn't open what was shared. It works with PDFs and images."
                        else "Toolbox can't work on PDFs and images together. Share them separately.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                return@Column
            }

            SectionLabel("What do you want to do?")
            Spacer(Modifier.height(10.dp))
            choices.forEach { (toolId, why) ->
                val tool = allTools.firstOrNull { it.id == toolId } ?: return@forEach
                ChoiceCard(tool = tool, why = why, onClick = { onToolChosen(toolId) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ChoiceCard(tool: ToolItem, why: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                    why,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
