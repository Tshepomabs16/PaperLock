package com.mabsSD.toolbox.ui

/**
 * What each tool can't do, in plain words. Shown as "Good to know" on the
 * tool's own screen, where it matters, and listed in full under Settings.
 * One source for both so they can't drift apart; keep each line true to the
 * code it describes.
 */
object ToolLimits {
    val scan = listOf(
        "Up to 30 pages per scan.",
        "Pages are saved at up to 2400 px on the long side, about 200 dpi on an A4 page.",
        "Needs Google Play services, which provides the scanner camera.",
    )

    val split = listOf(
        "Password-protected PDFs aren't supported.",
    )

    val merge = listOf(
        "PDFs only. To put photos into a PDF, use Scan and import them.",
        "Password-protected PDFs aren't supported.",
    )

    val compress = listOf(
        "PaperLock first shrinks only the pictures inside a PDF, which keeps its text selectable. " +
            "If that isn't enough, pages are turned into images: text then can't be selected or " +
            "searched, and links and form fields stop working. The result tells you which happened.",
        "Below about 60 dpi text stops being readable, so a very small target can be missed.",
        "Files already under the target are kept as they are.",
        "Password-protected PDFs aren't supported.",
    )

    val ocr = listOf(
        "Images only, not PDFs.",
        "Latin-script languages only, such as English, Afrikaans, French and Zulu.",
        "Printed text works far better than handwriting.",
    )

    val sharing = listOf(
        "PDFs and images only, and not both in the same share.",
    )

    /** In the order Settings lists them. */
    val all: List<Pair<String, List<String>>> = listOf(
        "Scan" to scan,
        "Split" to split,
        "Merge" to merge,
        "Compress" to compress,
        "OCR" to ocr,
        "Sharing to PaperLock" to sharing,
    )

    fun forTool(id: String): List<String> = when (id) {
        "scan" -> scan
        "split" -> split
        "merge" -> merge
        "compress" -> compress
        "ocr" -> ocr
        else -> emptyList()
    }
}
