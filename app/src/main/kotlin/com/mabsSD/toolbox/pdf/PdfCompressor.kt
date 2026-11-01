package com.mabsSD.toolbox.pdf

import android.graphics.Bitmap
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
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
    /**
     * True if pages were turned into images: text can no longer be selected,
     * searched or copied, and links and form fields are gone. False when only
     * the pictures inside the PDF were shrunk.
     */
    val pagesRasterised: Boolean,
)

/**
 * Compresses a PDF to a target size in two stages.
 *
 * 1. **Keep the text.** Shrink only the pictures embedded in the PDF —
 *    downscale and re-encode them as JPEG, over a few increasingly strong
 *    steps — and leave text, vector graphics, links and form fields alone.
 *    Most of a large PDF's weight is usually its pictures, so this is often
 *    enough, and a digital statement or contract stays searchable.
 * 2. **Rasterise.** If that can't reach the target, render every page to an
 *    image and re-encode at a reduced DPI and/or quality. This works on any
 *    input — PdfBox-Android can't decode every image filter (CCITT, JBIG2,
 *    JPX) but can always render a page — at the cost of the text.
 *
 * If neither reaches the target, the smaller result is kept, preferring the
 * one that kept its text.
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

    /** Stage 1 steps: (longest side in px, JPEG quality), gentlest first. */
    private val IMAGE_STEPS = listOf(2000 to 80, 1600 to 70, 1300 to 60, 1000 to 50)

    /** Pictures smaller than this aren't worth re-encoding. */
    private const val MIN_IMAGE_PIXELS = 160_000

    fun compress(
        source: InputStream,
        destination: OutputStream,
        targetBytes: Long,
        onProgress: (Float, String) -> Unit = { _, _ -> },
    ): CompressOutcome {
        // Each stage-1 step starts again from the original pictures, so keep
        // the source bytes rather than one document mutated step by step.
        val original = source.use { it.readBytes() }
        // Classifies encrypted/damaged/empty input before any work is done.
        val hasPictures = loadPdfOrThrow(original.inputStream()).use { countPictures(it) > 0 }

        // Stage 1: shrink pictures only.
        var keptText: ByteArray? = null
        val steps = if (hasPictures) IMAGE_STEPS else emptyList()
        for ((i, step) in steps.withIndex()) {
            onProgress(i.toFloat() / steps.size * 0.35f, "Shrinking pictures (text stays selectable)...")
            val attempt = loadPdfOrThrow(original.inputStream()).use { doc ->
                shrinkPictures(doc, maxSide = step.first, quality = step.second)
                ByteArrayOutputStream().also { doc.save(it) }.toByteArray()
            }
            if (keptText == null || attempt.size < keptText.size) keptText = attempt
            if (attempt.size <= targetBytes) {
                destination.write(attempt)
                return CompressOutcome(hitTarget = true, finalSizeBytes = attempt.size.toLong(), pagesRasterised = false)
            }
        }

        // Stage 2: rasterise every page.
        val rasterised = loadPdfOrThrow(original.inputStream()).use { doc ->
            rasterise(doc, targetBytes) { p, msg -> onProgress(0.35f + p * 0.6f, msg) }
        }
        val rasterBytes = rasterised.second
        return if (rasterised.first || keptText == null || rasterBytes.size < keptText.size) {
            destination.write(rasterBytes)
            CompressOutcome(rasterised.first, rasterBytes.size.toLong(), pagesRasterised = true)
        } else {
            // Neither fits; the text-preserving version is also the smaller one.
            destination.write(keptText)
            CompressOutcome(hitTarget = false, finalSizeBytes = keptText.size.toLong(), pagesRasterised = false)
        }
    }

    /** Embedded raster images reachable from the pages, counted once each. */
    private fun countPictures(doc: PDDocument): Int {
        val seen = java.util.IdentityHashMap<Any, Unit>()
        doc.pages.forEach { page -> page.resources?.let { visitImages(it, 0) { img, _, _ -> seen[img.cosObject] = Unit } } }
        return seen.size
    }

    /**
     * Downscale every sizeable picture to at most [maxSide] px and re-encode it
     * as JPEG at [quality]. Pictures with transparency, 1-bit images, ones
     * PdfBox can't decode, and ones that wouldn't get smaller are left as they
     * are. A picture used on several pages is re-encoded once and shared.
     */
    private fun shrinkPictures(doc: PDDocument, maxSide: Int, quality: Int) {
        val replaced = java.util.IdentityHashMap<Any, PDImageXObject?>()
        doc.pages.forEach { page ->
            val resources = page.resources ?: return@forEach
            visitImages(resources, 0) { image, owner, name ->
                val replacement = replaced.getOrPut(image.cosObject) { reencode(doc, image, maxSide, quality) }
                if (replacement != null) owner.put(name, replacement)
            }
        }
    }

    private fun reencode(doc: PDDocument, image: PDImageXObject, maxSide: Int, quality: Int): PDImageXObject? {
        if (image.isStencil || image.bitsPerComponent == 1) return null
        if (image.width.toLong() * image.height < MIN_IMAGE_PIXELS) return null
        val dict = image.cosObject
        if (dict.containsKey(COSName.SMASK) || dict.containsKey(COSName.MASK)) return null
        val originalLength = dict.length
        var bitmap: Bitmap? = null
        return try {
            bitmap = image.image ?: return null
            val longest = maxOf(bitmap.width, bitmap.height)
            if (longest > maxSide) {
                val scale = maxSide.toFloat() / longest
                val scaled = Bitmap.createScaledBitmap(
                    bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true,
                )
                if (scaled !== bitmap) bitmap.recycle()
                bitmap = scaled
            }
            val jpeg = JPEGFactory.createFromImage(doc, bitmap, quality / 100f)
            if (jpeg.cosObject.length < originalLength) jpeg else null
        } catch (e: Exception) {
            null // a filter PdfBox-Android can't decode (JPX, JBIG2...): leave it
        } catch (e: OutOfMemoryError) {
            null // an enormous picture: leave it rather than fail the whole job
        } finally {
            bitmap?.recycle()
        }
    }

    /** Calls [onImage] for each image XObject under [resources], following form XObjects. */
    private fun visitImages(
        resources: PDResources,
        depth: Int,
        onImage: (image: PDImageXObject, owner: PDResources, name: COSName) -> Unit,
    ) {
        if (depth > 6) return // malformed, self-referencing forms
        for (name in resources.xObjectNames.toList()) {
            val xObject = try { resources.getXObject(name) } catch (e: Exception) { null } ?: continue
            when (xObject) {
                is PDImageXObject -> onImage(xObject, resources, name)
                is PDFormXObject -> xObject.resources?.let { visitImages(it, depth + 1, onImage) }
            }
        }
    }

    /** Stage 2. Returns whether the target was met, and the PDF bytes. */
    private fun rasterise(
        doc: PDDocument,
        targetBytes: Long,
        onProgress: (Float, String) -> Unit,
    ): Pair<Boolean, ByteArray> {
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
        val out = ByteArrayOutputStream()
        writePdf(chosen.pages, pageBoxes, out)
        return Pair(winner != null, out.toByteArray())
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
}
