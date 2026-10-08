package com.mabsSD.toolbox.pdf

import android.graphics.Bitmap
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/** What actually happened, since a target size can be aspirational (P3-05). */
data class CompressOutcome(
    val hitTarget: Boolean,
    val finalSizeBytes: Long,
)

/**
 * Compresses a PDF to a target size by rasterising every page and re-encoding
 * it as JPEG at a reduced DPI and/or quality.
 *
 * This treats every page as an image regardless of its original content,
 * which is deliberate rather than a shortcut: PdfBox-Android cannot decode
 * every filter a "real" PDF compressor would need to touch (CCITT, JBIG2,
 * JPX), but the framework and PdfBox can both *render* a page to a bitmap
 * unconditionally. Rasterising is the one approach that works on any input,
 * which matters more here than preserving vector text in a document that is,
 * for this app's use case, almost always a photograph to begin with.
 *
 * Memory: pages are rendered and encoded one at a time, and only the encoded
 * JPEG bytes (tens of KB) are kept between trials — not the decoded bitmaps
 * (single-digit MB each at scan resolution). A 20-page document under search
 * therefore costs one page's worth of decoded pixels at any moment, not
 * twenty.
 */
object PdfCompressor {

    /** Below this, JPEG artifacts start eating into text legibility. */
    private const val MIN_JPEG_QUALITY = 30
    private const val MAX_JPEG_QUALITY = 92

    /** Tried in order; the first that hits the target at MAX quality wins outright. */
    private val DPI_STEPS = listOf(150, 120, 100, 80, 60)

    private const val MAX_BISECT_ITERATIONS = 7

    fun compress(
        source: InputStream,
        destination: OutputStream,
        targetBytes: Long,
        onProgress: (Float, String) -> Unit = { _, _ -> },
    ): CompressOutcome {
        source.use { stream ->
            loadPdfOrThrow(stream).use { doc ->
                return compressDocument(doc, destination, targetBytes, onProgress)
            }
        }
    }

    private fun compressDocument(
        doc: PDDocument,
        destination: OutputStream,
        targetBytes: Long,
        onProgress: (Float, String) -> Unit,
    ): CompressOutcome {
        val pageCount = doc.numberOfPages
        val renderer = PDFRenderer(doc)
        // Points, not pixels — PDRectangle is already DPI-independent, so the
        // output page keeps the source's physical size no matter what DPI the
        // content was rasterised at.
        val pageBoxes = (0 until pageCount).map { doc.getPage(it).mediaBox }

        var smallestSeen: PageSet? = null
        var winner: PageSet? = null

        searchDpi@ for ((stepIndex, dpi) in DPI_STEPS.withIndex()) {
            fun progressAt(withinStep: Float, message: String) =
                onProgress((stepIndex + withinStep) / DPI_STEPS.size * 0.9f, message)

            val atMax = renderAndEncode(renderer, pageCount, dpi, MAX_JPEG_QUALITY) {
                progressAt(it * 0.5f, "Rendering at $dpi DPI...")
            }
            if (atMax.totalBytes <= targetBytes) {
                winner = atMax
                break@searchDpi
            }
            if (smallestSeen == null || atMax.totalBytes < smallestSeen.totalBytes) smallestSeen = atMax

            val atMin = renderAndEncode(renderer, pageCount, dpi, MIN_JPEG_QUALITY) {
                progressAt(0.5f + it * 0.2f, "Trying $dpi DPI at the readability floor...")
            }
            if (atMin.totalBytes < smallestSeen.totalBytes) smallestSeen = atMin

            if (atMin.totalBytes > targetBytes) {
                // Even the floor quality doesn't fit at this DPI; only a lower
                // DPI can help, so don't waste time bisecting quality here.
                continue@searchDpi
            }

            // atMin fits and atMax doesn't: the target lives strictly between
            // them, so binary-search the highest quality that still fits.
            var lo = MIN_JPEG_QUALITY
            var hi = MAX_JPEG_QUALITY
            var candidate = atMin
            var iteration = 0
            while (hi - lo > 4 && iteration < MAX_BISECT_ITERATIONS) {
                val mid = (lo + hi) / 2
                val trial = renderAndEncode(renderer, pageCount, dpi, mid) {}
                iteration++
                progressAt(0.7f + (iteration.toFloat() / MAX_BISECT_ITERATIONS) * 0.3f, "Trying $dpi DPI, quality $mid...")
                if (trial.totalBytes <= targetBytes) {
                    candidate = trial
                    lo = mid
                } else {
                    hi = mid
                }
            }
            winner = candidate
            break@searchDpi
        }

        val chosen = winner ?: smallestSeen
            ?: error("DPI_STEPS must not be empty")

        onProgress(0.95f, "Writing file...")
        // The written PDF is larger than the sum of its embedded JPEGs once
        // page objects, dictionaries and the xref table are added, so the
        // outcome's reported size comes from what was actually written, not
        // from the pre-write page total the search was comparing against.
        val counting = CountingOutputStream(destination)
        writePdf(chosen.pages, pageBoxes, counting)

        return CompressOutcome(hitTarget = winner != null, finalSizeBytes = counting.bytesWritten)
    }

    private class PageSet(val pages: List<ByteArray>) {
        val totalBytes: Long = pages.sumOf { it.size.toLong() }
    }

    /**
     * Render every page at [dpi] and JPEG-encode at [quality], one page at a
     * time. Each bitmap is recycled immediately after encoding so peak memory
     * is one decoded page, not the whole document.
     */
    private fun renderAndEncode(
        renderer: PDFRenderer,
        pageCount: Int,
        dpi: Int,
        quality: Int,
        onProgress: (Float) -> Unit,
    ): PageSet {
        val pages = ArrayList<ByteArray>(pageCount)
        for (i in 0 until pageCount) {
            val bitmap = renderer.renderImageWithDPI(i, dpi.toFloat(), ImageType.RGB)
            val bytes = ByteArrayOutputStream().use { buffer ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, buffer)
                buffer.toByteArray()
            }
            bitmap.recycle()
            pages += bytes
            onProgress((i + 1).toFloat() / pageCount)
        }
        return PageSet(pages)
    }

    private fun writePdf(
        pageJpegs: List<ByteArray>,
        pageBoxes: List<PDRectangle>,
        destination: OutputStream,
    ) {
        PDDocument().use { out ->
            pageJpegs.forEachIndexed { index, jpeg ->
                val box = pageBoxes[index]
                val page = PDPage(box)
                out.addPage(page)

                val image = JPEGFactory.createFromStream(out, jpeg.inputStream())
                PDPageContentStream(out, page).use { content ->
                    content.drawImage(image, 0f, 0f, box.width, box.height)
                }
            }
            out.save(destination)
        }
    }

    /** Counts bytes as they pass through, without buffering them. */
    private class CountingOutputStream(private val delegate: OutputStream) : OutputStream() {
        var bytesWritten: Long = 0
            private set

        override fun write(b: Int) {
            delegate.write(b)
            bytesWritten++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
            bytesWritten += len
        }

        override fun flush() = delegate.flush()
        override fun close() = delegate.close()
    }
}
