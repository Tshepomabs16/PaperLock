package com.mabsSD.toolbox.tools

import android.net.Uri
import com.mabsSD.toolbox.history.HistoryDao
import com.mabsSD.toolbox.history.HistoryEntry
import com.mabsSD.toolbox.utils.WorkingFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * In-memory holder for the most recent tool result so it can be handed
 * between screens without serializing [Uri]s through navigation arguments,
 * plus the single choke-point every successful result passes through — which
 * makes it the natural place to also persist history, rather than threading a
 * DAO through every tool screen individually.
 */
object ResultStore {
    var lastResult: ToolResult? = null
        private set

    private var historyDao: HistoryDao? = null
    private var files: WorkingFileManager? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Called once from ToolboxApplication after the database is built. */
    fun attach(dao: HistoryDao, workingFileManager: WorkingFileManager) {
        historyDao = dao
        files = workingFileManager
    }

    /**
     * A genuinely new result: renames it on disk to its display name, updates
     * the pointer and records history. Returns the result as published.
     */
    fun set(result: ToolResult, toolId: String): ToolResult {
        val published = files?.let { result.copy(outputUri = it.publish(result.outputUri, result.outputName)) }
            ?: result
        lastResult = published
        historyDao?.let { dao ->
            scope.launch {
                dao.insert(
                    HistoryEntry(
                        toolId = toolId,
                        outputUri = published.outputUri.toString(),
                        outputName = published.outputName,
                        outputSize = published.outputSize,
                        createdAtMillis = System.currentTimeMillis(),
                    )
                )
            }
        }
        return published
    }

    /**
     * The existing result moved (e.g. "Save to…" copied it to a user-chosen
     * location) but nothing new was produced — update the pointer without
     * writing a second history row for the same piece of work.
     */
    fun updateLastResult(result: ToolResult) {
        lastResult = result
    }

    fun clear() {
        lastResult = null
    }
}
