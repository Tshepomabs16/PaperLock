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
}
