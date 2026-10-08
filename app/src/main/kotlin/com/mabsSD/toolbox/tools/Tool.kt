package com.mabsSD.toolbox.tools

import android.net.Uri

enum class ToolInputType {
    PDF,
    IMAGE,
    ANY
}

data class ToolInput(
    val uri: Uri,
    val type: ToolInputType
)

data class ToolResult(
    val outputUri: Uri,
    val outputName: String,
    val outputSize: Long,
    /**
     * Non-fatal detail worth showing next to the result, e.g. compression
     * missing its target size but still shipping the closest usable file
     * (P3-05). Null for the common case where the result speaks for itself.
     */
    val note: String? = null,
)

sealed class ToolProgress {
    data object Idle : ToolProgress()
    data class Running(val progress: Float, val message: String) : ToolProgress()
    data class Complete(val result: ToolResult) : ToolProgress()
    data class Error(val message: String) : ToolProgress()
}

interface Tool {
    val id: String
    val title: String
    val description: String
    val icon: String
    val acceptedInputs: List<ToolInputType>

    suspend fun run(
        inputs: List<ToolInput>,
        params: Map<String, Any> = emptyMap(),
        onProgress: (ToolProgress) -> Unit = {}
    ): ToolResult
}
