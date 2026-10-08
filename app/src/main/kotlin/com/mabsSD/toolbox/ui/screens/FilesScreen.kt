package com.mabsSD.toolbox.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mabsSD.toolbox.history.HistoryEntry
import com.mabsSD.toolbox.tools.ResultStore
import com.mabsSD.toolbox.tools.ToolResult
import com.mabsSD.toolbox.ui.components.FileHistoryCard
import com.mabsSD.toolbox.ui.toolboxContainer

private enum class SortOrder { DATE, NAME, SIZE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    onOpenResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = remember { context.toolboxContainer() }
    val allEntries by container.historyDb.historyDao().observeAll()
        .collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE) }

    val visible = remember(allEntries, query, sortOrder) {
        allEntries
            .filter { query.isBlank() || it.outputName.contains(query, ignoreCase = true) }
            .let { list ->
                when (sortOrder) {
                    SortOrder.DATE -> list.sortedByDescending { it.createdAtMillis }
                    SortOrder.NAME -> list.sortedBy { it.outputName.lowercase() }
                    SortOrder.SIZE -> list.sortedByDescending { it.outputSize }
                }
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = inner.calculateTopPadding(),
                bottom = inner.calculateBottomPadding() + 96.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    "Files",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search files") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                SortRow(current = sortOrder, onChange = { sortOrder = it })
                Spacer(Modifier.height(12.dp))
            }

            if (visible.isEmpty()) {
                item { FilesEmptyState(hasAny = allEntries.isNotEmpty()) }
            } else {
                items(visible, key = { it.id }) { entry ->
                    FileHistoryCard(
                        entry = entry,
                        onClick = { openHistoryEntry(entry, onOpenResult) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun openHistoryEntry(entry: HistoryEntry, onOpenResult: () -> Unit) {
    ResultStore.updateLastResult(
        ToolResult(
            outputUri = Uri.parse(entry.outputUri),
            outputName = entry.outputName,
            outputSize = entry.outputSize,
        )
    )
    onOpenResult()
}

@Composable
private fun SortRow(current: SortOrder, onChange: (SortOrder) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        listOf(SortOrder.DATE to "Date", SortOrder.NAME to "Name", SortOrder.SIZE to "Size")
            .forEach { (order, label) ->
                val selected = order == current
                androidx.compose.material3.FilterChip(
                    selected = selected,
                    onClick = { onChange(order) },
                    label = { Text(label) },
                )
            }
    }
}

@Composable
private fun FilesEmptyState(hasAny: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(40.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasAny) "No matches found." else "No files yet. Run a tool to see it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
