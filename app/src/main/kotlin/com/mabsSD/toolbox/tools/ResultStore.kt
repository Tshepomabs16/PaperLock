package com.mabsSD.toolbox.tools

import android.net.Uri
import com.mabsSD.toolbox.history.HistoryDao
import com.mabsSD.toolbox.history.HistoryEntry
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
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Called once from ToolboxApplication after the database is built. */
    fun attachHistory(dao: HistoryDao) {
        historyDao = dao
    }

    /** A genuinely new result: updates the pointer and records history. */
    fun set(result: ToolResult, toolId: String) {
        lastResult = result
        historyDao?.let { dao ->
            scope.launch {
                dao.insert(
                    HistoryEntry(
                        toolId = toolId,
                        outputUri = result.outputUri.toString(),
                        outputName = result.outputName,
                        outputSize = result.outputSize,
                        createdAtMillis = System.currentTimeMillis(),
                    )
                )
            }
        }
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
