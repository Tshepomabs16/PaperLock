package com.mabsSD.toolbox.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt

const val DEFAULT_PAGE_DPI = 150f
const val DEFAULT_JPEG_QUALITY = 95

data class PdfPageSpec(
    val bitmap: Bitmap,
    val rotationDegrees: Int = 0
)

/**
 * Builds a PDF from scanned bitmaps.
 *
 * Distinct from [com.mabsSD.toolbox.pdf.PdfEngine], which manipulates existing
 * documents: this one only creates them from images, a different job with
 * different memory characteristics. Both sit on PdfBox-Android and would need
 * porting together if the library is ever swapped.
 *
 * Works page-by-page, re-encoding buffers immediately, so large scans are not
 * held in memory at once (P1-09 memory pass).
 */
object PdfExporter {

    /**
     * Write a multi-page PDF from [pages] to [output]. The caller owns the
     * bitmaps and recycles them; for long documents prefer [exportPages],
     * which never needs more than one page in memory.
     *
     * @param pageWidthPt / [pageHeightPt] set the page size in PDF points.
     *   Pass `null, null` to size each page to the source image's aspect.
     */
    fun exportToPdf(
        pages: List<PdfPageSpec>,
        output: OutputStream,
        pageWidthPt: Float? = null,
        pageHeightPt: Float? = null,
        jpegQuality: Int = DEFAULT_JPEG_QUALITY
    ) {
        check(pages.isNotEmpty()) { "Cannot export an empty document" }
        writePages(pages.size, output, pageWidthPt, pageHeightPt, jpegQuality, recycle = false) { pages[it] }
    }

    /**
     * Streaming export: [producePage] is called for one page at a time, and
     * that page's bitmap is encoded and recycled before the next is asked for.
     * Peak memory is one decoded page plus the compressed JPEGs so far, no
     * matter how many pages there are.
     */
    fun exportPages(
        pageCount: Int,
        output: OutputStream,
        jpegQuality: Int = DEFAULT_JPEG_QUALITY,
        producePage: (Int) -> PdfPageSpec,
    ) {
        check(pageCount > 0) { "Cannot export an empty document" }
        writePages(pageCount, output, null, null, jpegQuality, recycle = true, producePage)
    }

    private fun writePages(
        pageCount: Int,
        output: OutputStream,
        pageWidthPt: Float?,
        pageHeightPt: Float?,
        jpegQuality: Int,
        recycle: Boolean,
        producePage: (Int) -> PdfPageSpec,
    ) {
        PDDocument().use { document ->
            for (index in 0 until pageCount) {
                val spec = producePage(index)
                val bitmap = spec.bitmap
                val widthPt = pageWidthPt ?: ptFromPx(bitmap.width)
                val heightPt = pageHeightPt ?: ptFromPx(bitmap.height)

                val imageBytes = encodeJpeg(bitmap, jpegQuality)
                // The pixels are no longer needed once encoded; free them
                // before PdfBox does any work of its own.
                if (recycle) bitmap.recycle()

                val page = PDPage(PDRectangle(widthPt, heightPt))
                page.rotation = spec.rotationDegrees
                document.addPage(page)

                val image = JPEGFactory.createFromStream(document, imageBytes.inputStream())
                PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { content ->
                    content.drawImage(image, 0f, 0f, widthPt, heightPt)
                }
            }
            document.save(output)
        }
    }

    private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /** Convert a bitmap pixel dimension to PDF points at [DEFAULT_PAGE_DPI]. */
    private fun ptFromPx(px: Int): Float =
        (px * 72f / DEFAULT_PAGE_DPI).roundToInt().toFloat()

    /**
     * Load a page [Uri] backed by a file (as returned by the ML Kit scanner)
     * as a downsampled bitmap to avoid OOM.
     *
     * @param mutable decode straight into a mutable bitmap so filters can run
     *   in place without a second full-size copy.
     */
    fun decodePage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2400,
        mutable: Boolean = false,
    ): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, bounds)

                var sample = 1
                while (bounds.outWidth / sample > maxDimension ||
                    bounds.outHeight / sample > maxDimension
                ) {
                    sample *= 2
                }

                context.contentResolver.openInputStream(uri)?.use { stream2 ->
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inMutable = mutable
                    }
                    BitmapFactory.decodeStream(stream2, null, opts)
                }
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            // Treated like an unreadable page rather than crashing the app.
            null
        }
    }
}
