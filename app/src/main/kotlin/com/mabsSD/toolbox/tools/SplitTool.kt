package com.mabsSD.toolbox.tools

import android.content.Context
import android.net.Uri
import com.mabsSD.toolbox.pdf.PageRange
import com.mabsSD.toolbox.pdf.PdfEngine
import com.mabsSD.toolbox.pdf.PdfError
import com.mabsSD.toolbox.utils.WorkingFileManager

/**
 * Extract a page selection into a new PDF.
 *
 * The selection arrives through [PARAM_PAGES] because the generic tool screen
 * has no way to express one; SplitScreen collects it and hands it over.
 */
class SplitTool(
    private val context: Context,
    private val workingFileManager: WorkingFileManager,
    private val engine: PdfEngine,
) : Tool {
    override val id = "split"
    override val title = "Split"
    override val description = "Pull selected pages out of a PDF into a new file"
    override val icon = "splitscreen"
    override val acceptedInputs = listOf(ToolInputType.PDF)

    companion object {
        /** Zero-based page indices, in output order. */
        const val PARAM_PAGES = "pages"
    }

    override suspend fun run(
        inputs: List<ToolInput>,
        params: Map<String, Any>,
        onProgress: (ToolProgress) -> Unit,
    ): ToolResult {
        val input = inputs.firstOrNull() ?: throw PdfError.Damaged()

        @Suppress("UNCHECKED_CAST")
        val pages = params[PARAM_PAGES] as? List<Int> ?: throw PdfError.NothingSelected()
        if (pages.isEmpty()) throw PdfError.NothingSelected()

        onProgress(ToolProgress.Running(0.1f, "Reading document..."))

        val outputUri = workingFileManager.createTempFile(prefix = "split_", extension = "pdf")

        onProgress(ToolProgress.Running(0.4f, "Extracting ${pages.size} pages..."))

        val source = context.contentResolver.openInputStream(input.uri)
            ?: throw PdfError.Damaged()
        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            engine.extractPages(source, pages, out)
        } ?: run {
            source.close()
            throw PdfError.Damaged()
        }

        onProgress(ToolProgress.Running(1f, "Done"))

        return ToolResult(
            outputUri = outputUri,
            outputName = outputName(workingFileManager.getFileName(input.uri), pages),
            outputSize = workingFileManager.getFileSize(outputUri),
        )
    }

    /**
     * P2-08 naming: keep the original stem and say what was taken, so a folder
     * of extracts stays readable — "statement_p2-4.pdf", not "output(3).pdf".
     */
    private fun outputName(sourceName: String?, pages: List<Int>): String {
        val stem = (sourceName ?: "document").substringBeforeLast('.', "document")
        val suffix = PageRange.describe(pages)
            .removePrefix("Pages ")
            .substringBefore(" (")
            .replace(", ", "_")
            .replace(" ", "")
        return "${stem}_p$suffix.pdf"
    }
}

/** Combine several PDFs, in the order given, into one. */
class MergeTool(
    private val context: Context,
    private val workingFileManager: WorkingFileManager,
    private val engine: PdfEngine,
) : Tool {
    override val id = "merge"
    override val title = "Merge"
    override val description = "Join several PDFs into a single document"
    override val icon = "merge_type"
    override val acceptedInputs = listOf(ToolInputType.PDF)

    override suspend fun run(
        inputs: List<ToolInput>,
        params: Map<String, Any>,
        onProgress: (ToolProgress) -> Unit,
    ): ToolResult {
        if (inputs.size < 2) throw PdfError.TooFewDocuments()

        onProgress(ToolProgress.Running(0.1f, "Opening ${inputs.size} documents..."))

        val outputUri = workingFileManager.createTempFile(prefix = "merged_", extension = "pdf")

        val streams = inputs.map { input ->
            context.contentResolver.openInputStream(input.uri) ?: run {
                throw PdfError.Damaged()
            }
        }

        onProgress(ToolProgress.Running(0.5f, "Merging..."))

        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            // merge() takes ownership of the streams and closes them, including
            // on failure, so there is no second close here.
            engine.merge(streams, out)
        } ?: run {
            streams.forEach { runCatching { it.close() } }
            throw PdfError.Damaged()
        }

        onProgress(ToolProgress.Running(1f, "Done"))

        return ToolResult(
            outputUri = outputUri,
            outputName = outputName(inputs),
            outputSize = workingFileManager.getFileSize(outputUri),
        )
    }

    private fun outputName(inputs: List<ToolInput>): String {
        val firstStem = workingFileManager.getFileName(inputs.first().uri)
            ?.substringBeforeLast('.', "document")
            ?: "document"
        return "${firstStem}_merged_${inputs.size}.pdf"
    }
}

/** Convenience for callers that only have a Uri. */
fun pdfInput(uri: Uri) = ToolInput(uri, ToolInputType.PDF)
