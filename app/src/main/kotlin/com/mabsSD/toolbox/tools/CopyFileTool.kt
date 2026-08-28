package com.mabsSD.toolbox.tools

import android.content.Context
import android.net.Uri
import com.mabsSD.toolbox.utils.WorkingFileManager

class CopyFileTool(
    private val context: Context,
    private val workingFileManager: WorkingFileManager
) : Tool {
    override val id = "copy_file"
    override val title = "Copy File"
    override val description = "Copy a file to a new location"
    override val icon = "content_copy"
    override val acceptedInputs = listOf(ToolInputType.ANY)

    override suspend fun run(
        inputs: List<ToolInput>,
        params: Map<String, Any>,
        onProgress: (ToolProgress) -> Unit
    ): ToolResult {
        require(inputs.isNotEmpty()) { "No input file provided" }

        val input = inputs.first()
        onProgress(ToolProgress.Running(0f, "Starting copy..."))

        val outputUri = workingFileManager.createTempFile(
            prefix = "copy_",
            extension = getExtension(input.uri)
        )

        context.contentResolver.openInputStream(input.uri)?.use { input ->
            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                input.copyTo(output)
            }
        }

        onProgress(ToolProgress.Running(1f, "Copy complete"))

        val fileName = workingFileManager.getFileName(input.uri) ?: "copied_file"
        val fileSize = workingFileManager.getFileSize(outputUri)

        return ToolResult(
            outputUri = outputUri,
            outputName = "copy_$fileName",
            outputSize = fileSize
        )
    }

    private fun getExtension(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    val name = it.getString(nameIndex)
                    name.substringAfterLast('.', "")
                } else ""
            } else ""
        } ?: ""
    }
}
