package com.mabsSD.toolbox.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mabsSD.toolbox.utils.PdfExporter
import com.mabsSD.toolbox.utils.PdfPageSpec
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class PdfCompressorTest {

    @Before
    fun setUp() {
        PDFBoxResourceLoader.init(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /**
     * A page-sized bitmap that looks like a scanned document: white background,
     * a grid of black text-like rules. Compressible under JPEG at low
     * quality/DPI, but not so trivially that every target is reachable at the
     * top of the DPI list — which is what actually exercises the search.
     */
    private fun scanLikePage(widthPx: Int = 1240, heightPx: Int = 1754): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { color = Color.BLACK; strokeWidth = 3f }
        var y = 80f
        while (y < heightPx - 80) {
            canvas.drawLine(80f, y, widthPx - 80f, y, paint)
            y += 34f
        }
        return bitmap
    }

    private fun fixturePdf(pageCount: Int): ByteArray {
        val pages = List(pageCount) { PdfPageSpec(scanLikePage()) }
        val out = ByteArrayOutputStream()
        // Export at high JPEG quality so the fixture starts large enough that
        // compression has real room to work with.
        PdfExporter.exportToPdf(pages, out, jpegQuality = 98)
        pages.forEach { it.bitmap.recycle() }
        return out.toByteArray()
    }

    private fun pageBoxesOf(pdf: ByteArray): List<Pair<Float, Float>> =
        PDDocument.load(ByteArrayInputStream(pdf)).use { doc ->
            (0 until doc.numberOfPages).map {
                val box = doc.getPage(it).mediaBox
                box.width to box.height
            }
        }

    private fun pageCountOf(pdf: ByteArray): Int =
        PDDocument.load(ByteArrayInputStream(pdf)).use { it.numberOfPages }

    @Test
    fun compress_wellAboveOriginalSize_hitsTargetImmediately() {
        val source = fixturePdf(2)
        val out = ByteArrayOutputStream()

        val outcome = PdfCompressor.compress(
            ByteArrayInputStream(source), out, targetBytes = source.size * 10L
        )

        assertTrue("expected the generous target to be reachable", outcome.hitTarget)
        assertEquals(out.size().toLong(), outcome.finalSizeBytes)
        assertTrue(out.size() <= source.size * 10)
        assertEquals(2, pageCountOf(out.toByteArray()))
    }

    @Test
    fun compress_preservesPageGeometry() {
        val source = fixturePdf(1)
        val sourceBoxes = pageBoxesOf(source)
        val out = ByteArrayOutputStream()

        PdfCompressor.compress(ByteArrayInputStream(source), out, targetBytes = source.size * 5L)

        val outBoxes = pageBoxesOf(out.toByteArray())
        assertEquals(sourceBoxes.size, outBoxes.size)
        sourceBoxes.zip(outBoxes).forEach { (expected, actual) ->
            // Page size must survive exactly: it is passed straight through
            // from the source's own mediaBox, never recomputed from the
            // rendered bitmap's pixel dimensions.
            assertTrue(abs(expected.first - actual.first) < 0.01f)
            assertTrue(abs(expected.second - actual.second) < 0.01f)
        }
    }

    @Test
    fun compress_reachableSmallTarget_actuallyShrinksTheFile() {
        val source = fixturePdf(3)

        val generous = ByteArrayOutputStream()
        val generousOutcome = PdfCompressor.compress(
            ByteArrayInputStream(source), generous, targetBytes = source.size * 10L
        )

        // A small but reachable target: below the generous result, comfortably
        // above what the lowest DPI/quality floor can produce for this fixture.
        val small = ByteArrayOutputStream()
        val smallTarget = generousOutcome.finalSizeBytes / 3
        val smallOutcome = PdfCompressor.compress(
            ByteArrayInputStream(source), small, targetBytes = smallTarget
        )

        assertTrue(
            "expected the reduced target to actually be smaller: " +
                "${smallOutcome.finalSizeBytes} vs ${generousOutcome.finalSizeBytes}",
            smallOutcome.finalSizeBytes < generousOutcome.finalSizeBytes
        )
        assertEquals(3, pageCountOf(small.toByteArray()))
    }

    @Test
    fun compress_unreachableTarget_stillProducesAValidSmallestEffort() {
        val source = fixturePdf(2)
        val out = ByteArrayOutputStream()

        // No real document fits in 200 bytes; this must degrade gracefully
        // (P3-05) rather than throwing or looping forever.
        val outcome = PdfCompressor.compress(ByteArrayInputStream(source), out, targetBytes = 200)

        assertTrue("an impossible target must be reported as missed", !outcome.hitTarget)
        assertTrue(outcome.finalSizeBytes > 0)
        assertEquals(2, pageCountOf(out.toByteArray()))
    }

    @Test
    fun compress_unreachableTarget_isNotLargerThanAReachableOne() {
        val source = fixturePdf(2)

        val reachable = ByteArrayOutputStream()
        val reachableOutcome = PdfCompressor.compress(
            ByteArrayInputStream(source), reachable, targetBytes = source.size * 10L
        )

        val impossible = ByteArrayOutputStream()
        val impossibleOutcome = PdfCompressor.compress(
            ByteArrayInputStream(source), impossible, targetBytes = 200
        )

        // Pushed to the floor, the "best effort" must be at or below anything
        // produced for a target the search could actually satisfy.
        assertTrue(impossibleOutcome.finalSizeBytes <= reachableOutcome.finalSizeBytes)
    }

    @Test
    fun compress_rejectsUnreadableInput() {
        val out = ByteArrayOutputStream()
        try {
            PdfCompressor.compress(
                ByteArrayInputStream("not a pdf".toByteArray()), out, targetBytes = 1_000_000
            )
            fail("expected PdfError.Damaged")
        } catch (e: PdfError.Damaged) {
            // expected
        }
    }

    // Text-preserving stage ------------------------------------------------

    /** A page with real (vector) text and a large, noisy photo: most of the weight is the photo. */
    private fun textAndPhotoPdf(): ByteArray {
        val photo = Bitmap.createBitmap(2400, 1800, Bitmap.Config.ARGB_8888)
        val rnd = java.util.Random(4471)
        val px = IntArray(photo.width * photo.height) { Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        photo.setPixels(px, 0, photo.width, 0, 0, photo.width, photo.height)
        // Opaque, like a real photo: an ARGB bitmap flagged as having alpha
        // makes JPEGFactory attach a soft mask, and the compressor (rightly)
        // leaves pictures with transparency alone.
        photo.setHasAlpha(false)
        val out = ByteArrayOutputStream()
        PDDocument().use { doc ->
            val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
            doc.addPage(page)
            val image = com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromImage(doc, photo, 0.95f)
            com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
                cs.beginText()
                cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 24f)
                cs.newLineAtOffset(72f, 760f)
                cs.showText("INVOICE 4471")
                cs.endText()
                cs.drawImage(image, 72f, 300f, 450f, 338f)
            }
            doc.save(out)
        }
        photo.recycle()
        return out.toByteArray()
    }

    private fun textOf(pdf: ByteArray): String =
        PDDocument.load(ByteArrayInputStream(pdf)).use { com.tom_roush.pdfbox.text.PDFTextStripper().getText(it) }

    @Test
    fun compress_shrinksPicturesFirst_andKeepsTheText() {
        val source = textAndPhotoPdf()
        val target = source.size / 3L
        val out = ByteArrayOutputStream()
        val outcome = PdfCompressor.compress(ByteArrayInputStream(source), out, targetBytes = target)

        assertTrue("target should be reachable by shrinking the photo", outcome.hitTarget)
        assertTrue("pages must not have been turned into images", !outcome.pagesRasterised)
        assertTrue("output ${out.size()} should be within target $target", out.size() <= target)
        assertTrue("text should still be extractable", textOf(out.toByteArray()).contains("INVOICE 4471"))
    }

    @Test
    fun compress_textOnlyPdfBelowItsSize_reportsRasterising() {
        // No pictures to shrink, so only rasterising can change the size; the
        // outcome must say so rather than imply the text survived.
        val source = PDDocument().use { doc ->
            val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
            doc.addPage(page)
            com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
                cs.beginText(); cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12f)
                cs.newLineAtOffset(72f, 760f); cs.showText("Plain text page"); cs.endText()
            }
            ByteArrayOutputStream().also { doc.save(it) }.toByteArray()
        }
        val outcome = PdfCompressor.compress(ByteArrayInputStream(source), ByteArrayOutputStream(), targetBytes = 200)
        assertTrue(outcome.pagesRasterised)
    }
}
