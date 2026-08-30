package com.mabsSD.toolbox.pdf

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Fixture tests for the PDF engine (P2-09).
 *
 * These exist so PdfBox-Android can be replaced: a candidate engine is
 * equivalent if it passes this suite unchanged. Instrumented rather than JVM
 * unit tests because PdfBox-Android needs a real Android runtime.
 *
 * Every fixture stamps a distinct marker on each page and assertions extract
 * the text back out, so the tests prove page *identity and order*, not just
 * that the page count happens to be right.
 */
@RunWith(AndroidJUnit4::class)
class PdfBoxEngineTest {

    private lateinit var engine: PdfEngine

    @Before
    fun setUp() {
        PDFBoxResourceLoader.init(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        engine = PdfBoxEngine()
    }

    /** A PDF whose page N contains the text "PAGE-N". */
    private fun makePdf(pageCount: Int): ByteArray {
        val out = ByteArrayOutputStream()
        PDDocument().use { doc ->
            repeat(pageCount) { i ->
                val page = PDPage()
                doc.addPage(page)
                PDPageContentStream(doc, page).use { content ->
                    content.beginText()
                    content.setFont(PDType1Font.HELVETICA, 24f)
                    content.newLineAtOffset(72f, 700f)
                    content.showText("PAGE-${i + 1}")
                    content.endText()
                }
            }
            doc.save(out)
        }
        return out.toByteArray()
    }

    /** The page markers present in [pdf], in document order. */
    private fun markersOf(pdf: ByteArray): List<String> =
        PDDocument.load(ByteArrayInputStream(pdf)).use { doc ->
            (1..doc.numberOfPages).map { pageNo ->
                val stripper = PDFTextStripper().apply {
                    startPage = pageNo
                    endPage = pageNo
                }
                stripper.getText(doc).trim()
            }
        }

    private fun input(bytes: ByteArray) = ByteArrayInputStream(bytes)

    // ---- readInfo --------------------------------------------------------

    @Test
    fun readInfo_reportsPageCount() {
        assertEquals(5, engine.readInfo(input(makePdf(5))).pageCount)
        assertEquals(1, engine.readInfo(input(makePdf(1))).pageCount)
    }

    @Test
    fun readInfo_rejectsGarbage() {
        try {
            engine.readInfo(input("this is definitely not a pdf".toByteArray()))
            fail("expected PdfError.Damaged")
        } catch (e: PdfError.Damaged) {
            assertTrue(e.message!!.contains("damaged"))
        }
    }

    @Test
    fun readInfo_rejectsEmptyInput() {
        try {
            engine.readInfo(input(ByteArray(0)))
            fail("expected PdfError.Damaged")
        } catch (e: PdfError.Damaged) {
            // expected
        }
    }

    // ---- extractPages ----------------------------------------------------

    @Test
    fun extractPages_keepsOnlySelectedPagesInOrder() {
        val out = ByteArrayOutputStream()
        engine.extractPages(input(makePdf(10)), listOf(0, 2, 4), out)

        assertEquals(listOf("PAGE-1", "PAGE-3", "PAGE-5"), markersOf(out.toByteArray()))
    }

    @Test
    fun extractPages_honoursRequestedOrderNotDocumentOrder() {
        val out = ByteArrayOutputStream()
        engine.extractPages(input(makePdf(5)), listOf(4, 0, 2), out)

        assertEquals(listOf("PAGE-5", "PAGE-1", "PAGE-3"), markersOf(out.toByteArray()))
    }

    @Test
    fun extractPages_singlePage() {
        val out = ByteArrayOutputStream()
        engine.extractPages(input(makePdf(3)), listOf(1), out)

        assertEquals(listOf("PAGE-2"), markersOf(out.toByteArray()))
    }

    @Test
    fun extractPages_wholeDocument() {
        val out = ByteArrayOutputStream()
        engine.extractPages(input(makePdf(4)), listOf(0, 1, 2, 3), out)

        assertEquals(listOf("PAGE-1", "PAGE-2", "PAGE-3", "PAGE-4"), markersOf(out.toByteArray()))
    }

    @Test
    fun extractPages_rejectsEmptySelection() {
        try {
            engine.extractPages(input(makePdf(3)), emptyList(), ByteArrayOutputStream())
            fail("expected PdfError.NothingSelected")
        } catch (e: PdfError.NothingSelected) {
            // expected
        }
    }

    @Test
    fun extractPages_rejectsOutOfRangeIndex() {
        try {
            engine.extractPages(input(makePdf(3)), listOf(0, 99), ByteArrayOutputStream())
            fail("expected PdfError for out-of-range page")
        } catch (e: PdfError) {
            // expected
        }
    }

    // ---- merge -----------------------------------------------------------

    @Test
    fun merge_concatenatesInGivenOrder() {
        val a = makePdf(2) // PAGE-1, PAGE-2
        val b = makePdf(1) // PAGE-1
        val out = ByteArrayOutputStream()

        engine.merge(listOf(input(a), input(b)), out)

        assertEquals(listOf("PAGE-1", "PAGE-2", "PAGE-1"), markersOf(out.toByteArray()))
    }

    @Test
    fun merge_respectsSourceOrdering() {
        val two = makePdf(2)
        val three = makePdf(3)

        val first = ByteArrayOutputStream()
        engine.merge(listOf(input(two), input(three)), first)

        val second = ByteArrayOutputStream()
        engine.merge(listOf(input(three), input(two)), second)

        assertEquals(5, engine.readInfo(input(first.toByteArray())).pageCount)
        assertEquals(5, engine.readInfo(input(second.toByteArray())).pageCount)
        assertEquals(
            listOf("PAGE-1", "PAGE-2", "PAGE-1", "PAGE-2", "PAGE-3"),
            markersOf(first.toByteArray())
        )
        assertEquals(
            listOf("PAGE-1", "PAGE-2", "PAGE-3", "PAGE-1", "PAGE-2"),
            markersOf(second.toByteArray())
        )
    }

    @Test
    fun merge_ofThreeDocuments() {
        val out = ByteArrayOutputStream()
        engine.merge(listOf(input(makePdf(1)), input(makePdf(2)), input(makePdf(3))), out)

        assertEquals(6, engine.readInfo(input(out.toByteArray())).pageCount)
    }

    @Test
    fun merge_rejectsSingleDocument() {
        try {
            engine.merge(listOf(input(makePdf(2))), ByteArrayOutputStream())
            fail("expected PdfError.TooFewDocuments")
        } catch (e: PdfError.TooFewDocuments) {
            // expected
        }
    }

    // ---- deletePages -----------------------------------------------------

    @Test
    fun deletePages_removesSelectedPages() {
        val out = ByteArrayOutputStream()
        engine.deletePages(input(makePdf(5)), setOf(1, 3), out)

        assertEquals(listOf("PAGE-1", "PAGE-3", "PAGE-5"), markersOf(out.toByteArray()))
    }

    @Test
    fun deletePages_handlesDescendingRemovalWithoutIndexShift() {
        // Deleting 0 and 1 must remove the first two pages, not page 1 and then
        // whatever slid into index 1.
        val out = ByteArrayOutputStream()
        engine.deletePages(input(makePdf(4)), setOf(0, 1), out)

        assertEquals(listOf("PAGE-3", "PAGE-4"), markersOf(out.toByteArray()))
    }

    @Test
    fun deletePages_rejectsRemovingEveryPage() {
        try {
            engine.deletePages(input(makePdf(2)), setOf(0, 1), ByteArrayOutputStream())
            fail("expected PdfError.NoPages")
        } catch (e: PdfError.NoPages) {
            // expected
        }
    }

    // ---- rotatePages -----------------------------------------------------

    @Test
    fun rotatePages_appliesRotationToNamedPagesOnly() {
        val out = ByteArrayOutputStream()
        engine.rotatePages(input(makePdf(3)), mapOf(1 to 90), out)

        PDDocument.load(ByteArrayInputStream(out.toByteArray())).use { doc ->
            assertEquals(0, doc.getPage(0).rotation)
            assertEquals(90, doc.getPage(1).rotation)
            assertEquals(0, doc.getPage(2).rotation)
        }
    }

    @Test
    fun rotatePages_normalisesPastFullTurn() {
        val once = ByteArrayOutputStream()
        engine.rotatePages(input(makePdf(1)), mapOf(0 to 270), once)

        val twice = ByteArrayOutputStream()
        engine.rotatePages(input(once.toByteArray()), mapOf(0 to 180), twice)

        PDDocument.load(ByteArrayInputStream(twice.toByteArray())).use { doc ->
            // 270 + 180 = 450, which must land on 90 rather than an angle no
            // reader will honour.
            assertEquals(90, doc.getPage(0).rotation)
        }
    }

    @Test
    fun rotatePages_acceptsNegativeDegrees() {
        val out = ByteArrayOutputStream()
        engine.rotatePages(input(makePdf(1)), mapOf(0 to -90), out)

        PDDocument.load(ByteArrayInputStream(out.toByteArray())).use { doc ->
            assertEquals(270, doc.getPage(0).rotation)
        }
    }

    // ---- round trip ------------------------------------------------------

    @Test
    fun extractThenMerge_reconstructsOriginalOrder() {
        val original = makePdf(4)

        val firstHalf = ByteArrayOutputStream()
        engine.extractPages(input(original), listOf(0, 1), firstHalf)
        val secondHalf = ByteArrayOutputStream()
        engine.extractPages(input(original), listOf(2, 3), secondHalf)

        val rejoined = ByteArrayOutputStream()
        engine.merge(listOf(input(firstHalf.toByteArray()), input(secondHalf.toByteArray())), rejoined)

        assertEquals(markersOf(original), markersOf(rejoined.toByteArray()))
    }
}
