package com.mabsSD.toolbox.tools

import android.content.Context
import com.mabsSD.toolbox.ocr.OcrEngine
import com.mabsSD.toolbox.pdf.PdfError
import com.mabsSD.toolbox.utils.PdfExporter
import com.mabsSD.toolbox.utils.WorkingFileManager
import java.nio.charset.StandardCharsets

/**
 * "Copy text from image" (P4-02): recognise, then hand the result to the
 * generic Result screen as a .txt file so Save/Share come for free and the
 * text renders in-place via ResultScreen's text preview.
 *
 * Ships standalone ahead of the searchable-PDF text layer, per the plan's own
 * risk mitigation — if the layer's coordinate mapping fights back, this still
 * shipped real value on its own.
 */
class OcrTool(
    private val context: Context,
    private val workingFileManager: WorkingFileManager,
) : Tool {
    override val id = "ocr"
    override val title = "OCR"
    override val description = "Recognise text in a photo and copy or share it"
    override val icon = "text_fields"
    override val acceptedInputs = listOf(ToolInputType.IMAGE)

    override suspend fun run(
        inputs: List<ToolInput>,
        params: Map<String, Any>,
        onProgress: (ToolProgress) -> Unit,
    ): ToolResult {
        val input = inputs.firstOrNull() ?: throw PdfError.Damaged()

        onProgress(ToolProgress.Running(0.1f, "Reading image..."))
        val bitmap = PdfExporter.decodePage(context, input.uri, maxDimension = 3000)
            ?: throw PdfError.Damaged()

        val text = try {
            onProgress(ToolProgress.Running(0.35f, "Recognising text..."))
            OcrEngine.recognize(bitmap)
        } finally {
            bitmap.recycle()
        }

        onProgress(ToolProgress.Running(0.9f, "Done"))

        val outputUri = workingFileManager.createTempFile(prefix = "ocr_", extension = "txt")
        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            out.write(text.toByteArray(StandardCharsets.UTF_8))
        } ?: throw PdfError.Damaged()

        val stem = workingFileManager.getFileName(input.uri)
            ?.substringBeforeLast('.', "image")
            ?: "image"

        return ToolResult(
            outputUri = outputUri,
            outputName = "${stem}_text.txt",
            outputSize = workingFileManager.getFileSize(outputUri),
            note = if (text.isBlank()) "No text was detected in this image." else null,
        )
    }
}
