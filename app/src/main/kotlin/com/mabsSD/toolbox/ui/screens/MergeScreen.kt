package com.mabsSD.toolbox.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mabsSD.toolbox.tools.ToolProgress
import com.mabsSD.toolbox.tools.ToolRunner
import com.mabsSD.toolbox.tools.pdfInput
import com.mabsSD.toolbox.ui.components.BorderedBlock
import com.mabsSD.toolbox.ui.components.PrimaryButton
import com.mabsSD.toolbox.ui.components.SectionLabel
import com.mabsSD.toolbox.ui.components.SecondaryButton
import com.mabsSD.toolbox.ui.components.ToolboxTopBar
import com.mabsSD.toolbox.ui.theme.BorderWidth
import com.mabsSD.toolbox.ui.toolboxContainer

private data class MergeEntry(val uri: Uri, val name: String)

@Composable
fun MergeScreen(
    onNavigateToResult: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = remember { context.toolboxContainer() }
    val runner = remember { ToolRunner() }
    val runnerState by runner.state.collectAsState()

    val entries = remember { mutableStateListOf<MergeEntry>() }

    DisposableEffect(runner) { onDispose { runner.dispose() } }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            // The picker returns selections in an arbitrary order and allows the
            // same file twice; keep the first occurrence and let the user reorder.
            if (entries.none { it.uri == uri }) {
                val name = container.workingFileManager.getFileName(uri) ?: "Document"
                entries += MergeEntry(uri, name)
            }
        }
    }

    LaunchedEffect(runnerState) {
        if (runnerState is ToolProgress.Complete) onNavigateToResult()
    }

    Scaffold(
        topBar = { ToolboxTopBar(title = "Merge", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        val running = runnerState as? ToolProgress.Running
        if (running != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(running.message, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                SecondaryButton("Cancel", { runner.cancel() })
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            BorderedBlock {
                SectionLabel("What this does")
                Text(
                    "Join several PDFs into one, in the order you set below.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Spacer(Modifier.height(24.dp))

            if (entries.isEmpty()) {
                PrimaryButton(
                    text = "Choose PDFs",
                    onClick = { picker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Column
            }

            SectionLabel("${entries.size} files — output order")
            Spacer(Modifier.height(10.dp))

            entries.forEachIndexed { index, entry ->
                MergeRow(
                    position = index + 1,
                    name = entry.name,
                    canMoveUp = index > 0,
                    canMoveDown = index < entries.lastIndex,
                    onMoveUp = { entries.swap(index, index - 1) },
                    onMoveDown = { entries.swap(index, index + 1) },
                    onRemove = { entries.removeAt(index) },
                )
                Spacer(Modifier.height(8.dp))
            }

            (runnerState as? ToolProgress.Error)?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (entries.size < 2) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Add at least one more PDF to merge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = "Merge ${entries.size} files",
                onClick = {
                    runner.runTool(container.mergeTool, entries.map { pdfInput(it.uri) })
                },
                enabled = entries.size >= 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            SecondaryButton(
                text = "Add more",
                onClick = { picker.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MergeRow(
    position: Int,
    name: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .border(BorderWidth, MaterialTheme.colorScheme.onBackground, RectangleShape)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Text(
            text = position.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move $name up")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move $name down")
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove $name")
        }
    }
}

private fun <T> androidx.compose.runtime.snapshots.SnapshotStateList<T>.swap(a: Int, b: Int) {
    if (a == b) return
    val tmp = this[a]
    this[a] = this[b]
    this[b] = tmp
}
