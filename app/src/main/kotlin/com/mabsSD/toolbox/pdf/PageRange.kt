package com.mabsSD.toolbox.pdf

/**
 * Parser for user-typed page selections like "1-3, 5, 9-7".
 *
 * Pure Kotlin with no Android dependency so it can be covered by fast JVM unit
 * tests rather than instrumented ones.
 *
 * Pages are 1-based on the way in, because that is what the user sees in every
 * PDF reader, and 0-based on the way out, because that is what PDFBox indexes
 * on. Getting that boundary wrong is the classic off-by-one in this feature, so
 * it is converted in exactly one place.
 */
object PageRange {

    sealed class Result {
        /** Zero-based, ascending, de-duplicated page indices. */
        data class Pages(val indices: List<Int>) : Result()
        data class Invalid(val message: String) : Result()
    }

    /**
     * @param text user input, e.g. "1-3,5"
     * @param pageCount total pages in the document, used to bounds-check
     */
    fun parse(text: String, pageCount: Int): Result {
        if (pageCount <= 0) return Result.Invalid("This document has no pages.")

        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.Invalid("Enter a page range, for example 1-3, 5.")

        val indices = linkedSetOf<Int>()

        for (rawPart in trimmed.split(',')) {
            val part = rawPart.trim()
            if (part.isEmpty()) continue

            val bounds = part.split('-').map { it.trim() }
            if (bounds.size > 2) {
                return Result.Invalid("\"$part\" is not a valid range.")
            }

            val first = bounds[0].toIntOrNull()
                ?: return Result.Invalid("\"$part\" is not a page number.")
            val second = if (bounds.size == 1) {
                first
            } else {
                bounds[1].toIntOrNull()
                    ?: return Result.Invalid("\"$part\" is not a page number.")
            }

            // Accept "9-7" as 7..9. Users type ranges backwards often enough that
            // rejecting it is more annoying than helpful.
            val from = minOf(first, second)
            val to = maxOf(first, second)

            if (from < 1) return Result.Invalid("Pages start at 1.")
            if (to > pageCount) {
                return Result.Invalid("This document only has $pageCount pages.")
            }

            for (page in from..to) indices += page - 1
        }

        if (indices.isEmpty()) return Result.Invalid("Enter a page range, for example 1-3, 5.")

        return Result.Pages(indices.sorted())
    }

    /** Human-readable summary, e.g. "Pages 1-3, 5 (4 pages)". */
    fun describe(indices: List<Int>): String {
        if (indices.isEmpty()) return "No pages"

        val groups = mutableListOf<IntRange>()
        var start = indices.first()
        var prev = start
        for (i in indices.drop(1)) {
            if (i == prev + 1) {
                prev = i
            } else {
                groups += start..prev
                start = i
                prev = i
            }
        }
        groups += start..prev

        val label = groups.joinToString(", ") { range ->
            if (range.first == range.last) "${range.first + 1}"
            else "${range.first + 1}-${range.last + 1}"
        }
        val noun = if (indices.size == 1) "page" else "pages"
        return "Pages $label (${indices.size} $noun)"
    }
}
