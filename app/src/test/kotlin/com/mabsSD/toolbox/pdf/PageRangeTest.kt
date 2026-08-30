package com.mabsSD.toolbox.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageRangeTest {

    private fun pages(text: String, count: Int = 10): List<Int> {
        val result = PageRange.parse(text, count)
        assertTrue("expected $text to parse, got $result", result is PageRange.Result.Pages)
        return (result as PageRange.Result.Pages).indices
    }

    private fun error(text: String, count: Int = 10): String {
        val result = PageRange.parse(text, count)
        assertTrue("expected $text to fail, got $result", result is PageRange.Result.Invalid)
        return (result as PageRange.Result.Invalid).message
    }

    @Test
    fun `single page converts to zero-based index`() {
        assertEquals(listOf(0), pages("1"))
        assertEquals(listOf(9), pages("10"))
    }

    @Test
    fun `range is inclusive of both ends`() {
        assertEquals(listOf(0, 1, 2), pages("1-3"))
    }

    @Test
    fun `comma separated parts combine`() {
        assertEquals(listOf(0, 1, 2, 4), pages("1-3,5"))
    }

    @Test
    fun `whitespace is tolerated everywhere`() {
        assertEquals(listOf(0, 1, 2, 4), pages("  1 - 3 ,  5  "))
    }

    @Test
    fun `reversed range is accepted and normalised`() {
        assertEquals(listOf(6, 7, 8), pages("9-7"))
    }

    @Test
    fun `overlapping parts are de-duplicated and sorted`() {
        assertEquals(listOf(0, 1, 2, 3), pages("3-4,1-2,2"))
    }

    @Test
    fun `trailing and doubled commas are ignored`() {
        assertEquals(listOf(0, 4), pages("1,,5,"))
    }

    @Test
    fun `page zero is rejected`() {
        assertEquals("Pages start at 1.", error("0"))
    }

    @Test
    fun `page beyond the document is rejected with the real count`() {
        assertEquals("This document only has 10 pages.", error("11"))
        assertEquals("This document only has 10 pages.", error("8-12"))
    }

    @Test
    fun `non numeric input is rejected`() {
        assertTrue(error("abc").contains("not a page number"))
        assertTrue(error("1-x").contains("not a page number"))
    }

    @Test
    fun `malformed range with too many dashes is rejected`() {
        assertTrue(error("1-2-3").contains("not a valid range"))
    }

    @Test
    fun `empty input is rejected with guidance`() {
        assertTrue(error("").contains("for example"))
        assertTrue(error("   ").contains("for example"))
        assertTrue(error(",,,").contains("for example"))
    }

    @Test
    fun `document with no pages is rejected before parsing`() {
        assertEquals("This document has no pages.", error("1", count = 0))
    }

    @Test
    fun `whole document can be selected`() {
        assertEquals((0..9).toList(), pages("1-10"))
    }

    @Test
    fun `describe collapses consecutive pages back into ranges`() {
        assertEquals("Pages 1-3, 5 (4 pages)", PageRange.describe(listOf(0, 1, 2, 4)))
        assertEquals("Pages 1 (1 page)", PageRange.describe(listOf(0)))
        assertEquals("Pages 1-10 (10 pages)", PageRange.describe((0..9).toList()))
        assertEquals("No pages", PageRange.describe(emptyList()))
    }

    @Test
    fun `describe round-trips a parsed selection`() {
        val indices = pages("2-4,7")
        assertEquals("Pages 2-4, 7 (4 pages)", PageRange.describe(indices))
    }
}
