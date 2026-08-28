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
    val outputSize: Long
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
