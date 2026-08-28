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
 * PDF engine built on PdfBox-Android. Every PDF write in the app goes
 * through this class so the underlying library can be swapped later without
 * touching call sites (P2-02: the "wrap it behind PdfEngine" rule).
 *
 * The exporter works page-by-page, re-encoding buffers immediately, so large
 * scans are not held in memory at once (P1-09 memory pass).
 */
object PdfExporter {

    /**
     * Write a multi-page PDF from [pages] to [output].
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

        PDDocument().use { document ->
            pages.forEach { spec ->
                val bitmap = spec.bitmap
                val page = PDPage()
                page.rotation = spec.rotationDegrees

                val widthPt = pageWidthPt
                    ?: ptFromPx(bitmap.width)
                val heightPt = pageHeightPt
                    ?: ptFromPx(bitmap.height)
                page.mediaBox = PDRectangle(widthPt, heightPt)

                document.addPage(page)

                val imageBytes = encodeJpeg(bitmap, jpegQuality)
                val image = JPEGFactory.createFromStream(
                    document,
                    imageBytes.inputStream()
                )

                val content = PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                )
                content.drawImage(image, 0f, 0f, widthPt, heightPt)
                content.close()
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
     */
    fun decodePage(context: Context, uri: Uri, maxDimension: Int = 2400): Bitmap? {
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
                    }
                    BitmapFactory.decodeStream(stream2, null, opts)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
