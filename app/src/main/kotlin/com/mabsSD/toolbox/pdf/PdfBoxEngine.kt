package com.mabsSD.toolbox.pdf

import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * [PdfEngine] backed by PdfBox-Android.
 *
 * Kept deliberately thin: it translates library exceptions into [PdfError] and
 * does nothing else, so swapping the library means rewriting only this file.
 */
class PdfBoxEngine : PdfEngine {

    override fun readInfo(source: InputStream): PdfInfo =
        source.use { stream ->
            loadDocument(stream).use { doc ->
                PdfInfo(pageCount = doc.numberOfPages)
            }
        }

    override fun extractPages(
        source: InputStream,
        pages: List<Int>,
        destination: OutputStream,
    ) {
        if (pages.isEmpty()) throw PdfError.NothingSelected()

        source.use { stream ->
            loadDocument(stream).use { src ->
                val count = src.numberOfPages
                if (pages.any { it < 0 || it >= count }) throw PdfError.Damaged()

                PDDocument().use { out ->
                    // importPage copies the page but leaves some resources shared
                    // with the source document, so the save must happen while the
                    // source is still open. Keeping both `use` blocks nested is
                    // load-bearing, not stylistic.
                    pages.forEach { index -> out.importPage(src.getPage(index)) }
                    out.save(destination)
                }
            }
        }
    }

    override fun merge(sources: List<InputStream>, destination: OutputStream) {
        if (sources.size < 2) {
            sources.forEach { runCatching { it.close() } }
            throw PdfError.TooFewDocuments()
        }

        try {
            val merger = PDFMergerUtility()
            merger.destinationStream = destination
            sources.forEach { merger.addSource(it) }
            // PDFMergerUtility carries outlines across for us, which is the one
            // thing a naive page-by-page copy loses.
            merger.mergeDocuments(null)
        } catch (e: InvalidPasswordException) {
            throw PdfError.Encrypted()
        } catch (e: IOException) {
            throw PdfError.Damaged()
        } finally {
            sources.forEach { runCatching { it.close() } }
        }
    }

    override fun deletePages(
        source: InputStream,
        pages: Set<Int>,
        destination: OutputStream,
    ) {
        source.use { stream ->
            loadDocument(stream).use { doc ->
                val remaining = doc.numberOfPages - pages.count { it in 0 until doc.numberOfPages }
                if (remaining <= 0) throw PdfError.NoPages()

                // Descending, so each removal cannot shift the index of the next.
                pages.filter { it in 0 until doc.numberOfPages }
                    .sortedDescending()
                    .forEach { doc.removePage(it) }

                doc.save(destination)
            }
        }
    }

    override fun rotatePages(
        source: InputStream,
        rotations: Map<Int, Int>,
        destination: OutputStream,
    ) {
        source.use { stream ->
            loadDocument(stream).use { doc ->
                rotations.forEach { (index, degrees) ->
                    if (index in 0 until doc.numberOfPages) {
                        val page = doc.getPage(index)
                        // Normalise into 0/90/180/270; PDF viewers reject anything else.
                        page.rotation = ((page.rotation + degrees) % 360 + 360) % 360
                    }
                }
                doc.save(destination)
            }
        }
    }

    /** Load, translating the library's failure modes into [PdfError]. */
    private fun loadDocument(stream: InputStream): PDDocument {
        val doc = try {
            PDDocument.load(stream)
        } catch (e: InvalidPasswordException) {
            throw PdfError.Encrypted()
        } catch (e: IOException) {
            throw PdfError.Damaged()
        } catch (e: IllegalArgumentException) {
            // PdfBox throws this for some malformed cross-reference tables
            // rather than an IOException.
            throw PdfError.Damaged()
        }

        if (doc.isEncrypted) {
            doc.close()
            throw PdfError.Encrypted()
        }
        if (doc.numberOfPages == 0) {
            doc.close()
            throw PdfError.NoPages()
        }
        return doc
    }
}
