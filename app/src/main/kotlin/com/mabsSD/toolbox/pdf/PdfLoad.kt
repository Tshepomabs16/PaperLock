package com.mabsSD.toolbox.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.io.IOException
import java.io.InputStream

/**
 * Load a [PDDocument], translating the library's failure modes into [PdfError].
 *
 * Shared by every entry point that opens an existing PDF ([PdfBoxEngine],
 * [PdfCompressor]) so the encrypted/damaged/empty classification stays in one
 * place rather than drifting between call sites.
 */
internal fun loadPdfOrThrow(stream: InputStream): PDDocument {
    val doc = try {
        PDDocument.load(stream)
    } catch (e: InvalidPasswordException) {
        throw PdfError.Encrypted()
    } catch (e: IOException) {
        throw PdfError.Damaged()
    } catch (e: IllegalArgumentException) {
        // PdfBox throws this for some malformed cross-reference tables rather
        // than an IOException.
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
