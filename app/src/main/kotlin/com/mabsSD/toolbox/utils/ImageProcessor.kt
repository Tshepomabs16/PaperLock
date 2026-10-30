package com.mabsSD.toolbox.utils

import android.graphics.Bitmap
import kotlin.math.max

enum class ScanFilter(val uiName: String) {
    ORIGINAL("Original"),
    GRAYSCALE("Grayscale"),
    BINARY("B&W")
}

/**
 * On-device image processing for the scan pipeline. Our own pixel loops over
 * a [Bitmap] — no network, no third party — so the privacy property holds end
 * to end.
 *
 * Filters run **in place** on a mutable bitmap. The previous version returned
 * a fresh bitmap and allocated three full-size IntArrays per call; on a
 * 2400px page that is ~80 MB transient per filter, which across a page tray
 * of thumbnails was a large part of why long scans ran out of memory.
 */
object ImageProcessor {

    /**
     * Half-width of the local-mean window as a fraction of the long side:
     * 1/16, i.e. a window 1/8 of the page across, as in Bradley & Roth.
     */
    private const val WINDOW_DIVISOR = 16

    /**
     * Apply [filter] to [bitmap], modifying it. [bitmap] must be mutable.
     * [ScanFilter.ORIGINAL] leaves it untouched.
     */
    fun applyInPlace(bitmap: Bitmap, filter: ScanFilter) {
        if (filter == ScanFilter.ORIGINAL) return
        require(bitmap.isMutable) { "applyInPlace needs a mutable bitmap" }

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        toGrayValues(pixels)

        when (filter) {
            ScanFilter.GRAYSCALE -> for (i in pixels.indices) {
                val g = pixels[i]
                pixels[i] = (0xFF shl 24) or (g shl 16) or (g shl 8) or g
            }
            ScanFilter.BINARY -> adaptiveThreshold(pixels, w, h, windowFor(w, h))
            ScanFilter.ORIGINAL -> Unit
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /** Copying variant, for callers that must keep the source intact. */
    fun apply(input: Bitmap, filter: ScanFilter): Bitmap {
        val copy = input.copy(Bitmap.Config.ARGB_8888, true)
        applyInPlace(copy, filter)
        return copy
    }

    /**
     * The local-mean window scales with resolution, so a thumbnail, the
     * on-screen preview and the exported page all threshold to the same look.
     *
     * It must also be much wider than the thickest stroke. It used to be 8px
     * at 2400px (a 17px window): inside a bold heading the whole window was
     * ink, the "mean" was ink, and the letters came out as hollow outlines.
     * At 1/8 of the page the mean is always mostly paper, and the integral
     * image makes the size free.
     */
    internal fun windowFor(w: Int, h: Int): Int = max(2, max(w, h) / WINDOW_DIVISOR)

    /** ARGB -> perceptual luminance 0..255, stored back into the same array. */
    private fun toGrayValues(pixels: IntArray) {
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            pixels[i] = (299 * r + 587 * g + 114 * b) / 1000
        }
    }

    /**
     * Adaptive local-mean threshold (Sauvola-style) for unevenly lit pages:
     * a pixel is ink if it is darker than (1 - k) of its neighbourhood mean.
     *
     * The mean comes from a summed-area table, so each pixel costs four
     * lookups regardless of window size — which is what lets the window be
     * wide enough for bold text (see [windowFor]).
     *
     * [gray] holds luminance on entry and ARGB black/white on exit.
     */
    internal fun adaptiveThreshold(gray: IntArray, w: Int, h: Int, window: Int, k: Double = 0.2) {
        // 255 * pixel count must fit in an Int; scan pages are decoded at
        // <= 2400px a side, well inside this.
        require(w.toLong() * h * 255 < Int.MAX_VALUE) { "Image too large for the integral image" }

        val stride = w + 1
        val integral = IntArray(stride * (h + 1))
        for (y in 0 until h) {
            var rowSum = 0
            val src = y * w
            val dst = (y + 1) * stride
            for (x in 0 until w) {
                rowSum += gray[src + x]
                integral[dst + x + 1] = integral[dst - stride + x + 1] + rowSum
            }
        }

        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val scale = 1.0 - k
        for (y in 0 until h) {
            val y0 = max(0, y - window)
            val y1 = minOf(h - 1, y + window) + 1
            for (x in 0 until w) {
                val x0 = max(0, x - window)
                val x1 = minOf(w - 1, x + window) + 1
                val sum = integral[y1 * stride + x1] - integral[y0 * stride + x1] -
                    integral[y1 * stride + x0] + integral[y0 * stride + x0]
                val count = (x1 - x0) * (y1 - y0)
                val i = y * w + x
                gray[i] = if (gray[i] * count <= sum * scale) black else white
            }
        }
    }
}
