package com.mabsSD.toolbox.tools

import android.net.Uri

/**
 * In-memory holder for the most recent tool result so it can be handed
 * between screens without serializing [Uri]s through navigation arguments.
 *
 * Results are transient working files; they may be evicted at any time. The
 * database-backed history (P5) will supersede this.
 */
object ResultStore {
    var lastResult: ToolResult? = null
        private set

    fun set(result: ToolResult) {
        lastResult = result
    }

    fun clear() {
        lastResult = null
    }
}
