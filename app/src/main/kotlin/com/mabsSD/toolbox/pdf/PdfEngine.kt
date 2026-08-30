package com.mabsSD.toolbox.pdf

import java.io.InputStream
import java.io.OutputStream

/** What we can learn about a document without modifying it. */
data class PdfInfo(
    val pageCount: Int,
)

/**
 * Failures a user can actually be told about.
 *
 * P2-07 requires encrypted, damaged and zero-page PDFs to produce a clear
 * message rather than a crash, so those cases are modelled explicitly instead
 * of surfacing raw IOExceptions from the library.
 */
sealed class PdfError(message: String) : Exception(message) {
    class Encrypted : PdfError("This PDF is password-protected. Unlock it before using it here.")
    class Damaged : PdfError("This file could not be read as a PDF. It may be damaged.")
    class NoPages : PdfError("This PDF has no pages.")
    class NothingSelected : PdfError("Select at least one page.")
    class TooFewDocuments : PdfError("Choose at least two PDFs to merge.")
}

/**
 * Every PDF read and write in the app goes through this interface and nothing
 * else (P2-02).
 *
 * PdfBox-Android has not shipped a release in years, so the concrete engine is
 * kept swappable and covered by fixture tests: a replacement can be proved
 * equivalent by running the same suite against it.
 *
 * Implementations own the streams they are handed and close them.
 */
interface PdfEngine {

    /** Page count and validity. Throws [PdfError] for unusable documents. */
    fun readInfo(source: InputStream): PdfInfo

    /** Write a new PDF containing only [pages] (zero-based), in the order given. */
    fun extractPages(source: InputStream, pages: List<Int>, destination: OutputStream)

    /** Concatenate [sources] in order into one PDF, preserving outlines where present. */
    fun merge(sources: List<InputStream>, destination: OutputStream)

    /** Write a copy with [pages] (zero-based) removed. */
    fun deletePages(source: InputStream, pages: Set<Int>, destination: OutputStream)

    /**
     * Write a copy with the given rotations applied.
     *
     * @param rotations zero-based page index to clockwise degrees, added to any
     *   rotation the page already carries.
     */
    fun rotatePages(source: InputStream, rotations: Map<Int, Int>, destination: OutputStream)
}
