package com.mabsSD.toolbox.tools

import android.content.Context
import com.mabsSD.toolbox.pdf.PdfCompressor
import com.mabsSD.toolbox.pdf.PdfError
import com.mabsSD.toolbox.utils.ImageCompressor
import com.mabsSD.toolbox.utils.PdfExporter
import com.mabsSD.toolbox.utils.WorkingFileManager
import com.mabsSD.toolbox.utils.formatFileSize

/**
 * Shrinks a PDF or image toward a target size (P3).
 *
 * Routes on [ToolInput.type] rather than sniffing content, since the caller
 * (CompressScreen) already knows which picker it launched. CompressScreen
 * bypasses the generic ToolScreen picker entirely because this tool needs a
 * target-size choice before it can run — something the generic screen has no
 * way to collect.
 */
class CompressTool(
    private val context: Context,
    private val workingFileManager: WorkingFileManager,
) : Tool {
    override val id = "compress"
    override val title = "Compress"
    override val description = "Shrink a PDF or image toward a target file size"
    override val icon = "compress"
    override val acceptedInputs = listOf(ToolInputType.PDF, ToolInputType.IMAGE)

    companion object {
        /** Long, in bytes. */
        const val PARAM_TARGET_BYTES = "targetBytes"
    }

    override suspend fun run(
        inputs: List<ToolInput>,
        params: Map<String, Any>,
        onProgress: (ToolProgress) -> Unit,
    ): ToolResult {
        val input = inputs.firstOrNull() ?: throw PdfError.Damaged()
        val targetBytes = params[PARAM_TARGET_BYTES] as? Long
            ?: throw IllegalArgumentException("No target size given")

        return when (input.type) {
            ToolInputType.PDF -> compressPdf(input, targetBytes, onProgress)
            ToolInputType.IMAGE -> compressImage(input, targetBytes, onProgress)
            ToolInputType.ANY -> throw IllegalArgumentException("Compress needs a PDF or image, not ANY")
        }
    }

    private fun compressPdf(
        input: ToolInput,
        targetBytes: Long,
        onProgress: (ToolProgress) -> Unit,
    ): ToolResult {
        val source = context.contentResolver.openInputStream(input.uri) ?: throw PdfError.Damaged()
        val outputUri = workingFileManager.createTempFile(prefix = "compressed_", extension = "pdf")

        val outcome = context.contentResolver.openOutputStream(outputUri)?.use { out ->
            PdfCompressor.compress(source, out, targetBytes) { progress, message ->
                onProgress(ToolProgress.Running(progress, message))
            }
        } ?: run {
            source.close()
            throw PdfError.Damaged()
        }

        val finalSize = workingFileManager.getFileSize(outputUri)
        return ToolResult(
            outputUri = outputUri,
            outputName = outputName(input, "pdf"),
            outputSize = finalSize,
            note = missedTargetNote(outcome.hitTarget, targetBytes),
        )
    }

    private fun compressImage(
        input: ToolInput,
        targetBytes: Long,
        onProgress: (ToolProgress) -> Unit,
    ): ToolResult {
        onProgress(ToolProgress.Running(0.05f, "Reading image..."))
        // Bounded to 4000px on load: the search below downscales further as
        // needed, but decoding an unbounded source resolution first would
        // undercut the whole point of a memory-safe compressor.
        val bitmap = PdfExporter.decodePage(context, input.uri, maxDimension = 4000)
            ?: throw PdfError.Damaged()

        val outcome = try {
            ImageCompressor.compressToTarget(bitmap, targetBytes) { progress, message ->
                onProgress(ToolProgress.Running(progress, message))
            }
        } finally {
            bitmap.recycle()
        }

        val outputUri = workingFileManager.createTempFile(prefix = "compressed_", extension = "jpg")
        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            out.write(outcome.bytes)
        } ?: throw PdfError.Damaged()

        val finalSize = workingFileManager.getFileSize(outputUri)
        return ToolResult(
            outputUri = outputUri,
            outputName = outputName(input, "jpg"),
            outputSize = finalSize,
            note = missedTargetNote(outcome.hitTarget, targetBytes),
        )
    }

    private fun outputName(input: ToolInput, extension: String): String {
        val stem = workingFileManager.getFileName(input.uri)
            ?.substringBeforeLast('.', "document")
            ?: "document"
        return "${stem}_compressed.$extension"
    }

    private fun missedTargetNote(hitTarget: Boolean, targetBytes: Long): String? {
        if (hitTarget) return null
        return "Couldn't reach ${formatFileSize(targetBytes)} without hurting legibility. " +
            "This is the smallest readable version."
    }
}
