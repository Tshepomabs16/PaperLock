package com.mabsSD.toolbox.utils

import android.graphics.Bitmap
import android.graphics.Color

enum class ScanFilter(val uiName: String) {
    ORIGINAL("Original"),
    GRAYSCALE("Grayscale"),
    BINARY("B&W")
}

/**
 * On-device image processing for the scan pipeline. All operations are
 * powered by our own pixel loops over a [Bitmap] — no network, no third
 * party — so the privacy property holds end to end.
 */
object ImageProcessor {

    /**
     * Convert an ARGB [Bitmap] to grayscale, returning a new mutable bitmap.
     * Uses luminance weighting that matches human perception.
     */
    fun toGrayscale(input: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(input.width * input.height)
        input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(gray, gray, gray)
        }
        output.setPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
        return output
    }

    /**
     * Otsu's method: choose a global threshold that minimises intra-class
     * variance, split into foreground/background. Returns the threshold value.
     */
    private fun otsuThreshold(histogram: IntArray, total: Int): Int {
        var sum = 0L
        for (t in 0 until 256) sum += t.toLong() * histogram[t]
        var sumB = 0L
        var wB = 0
        var wF: Int
        var maxVariance = -1.0
        var threshold = 127

        for (t in 0 until 256) {
            wB += histogram[t]
            if (wB == 0) continue
            wF = total - wB
            if (wF == 0) break
            sumB += t.toLong() * histogram[t]
            val mB = sumB.toDouble() / wB
            val mF = (sum - sumB).toDouble() / wF
            val between = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (between > maxVariance) {
                maxVariance = between
                threshold = t
            }
        }
        return threshold
    }

    private fun buildHistogram(grayscale: Bitmap): Pair<IntArray, Int> {
        val histogram = IntArray(256)
        val pixels = IntArray(grayscale.width * grayscale.height)
        grayscale.getPixels(pixels, 0, grayscale.width, 0, 0, grayscale.width, grayscale.height)
        for (p in pixels) {
            histogram[Color.red(p)]++
        }
        return histogram to pixels.size
    }

    /**
     * Global B&W via Otsu. Best for evenly-lit pages; fast single pass.
     */
    fun toBinaryOtsu(input: Bitmap): Bitmap {
        val gray = toGrayscale(input)
        val (histogram, total) = buildHistogram(gray)
        val threshold = otsuThreshold(histogram, total)

        val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(input.width * input.height)
        gray.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
        for (i in pixels.indices) {
            val value = Color.red(pixels[i])
            val binary = if (value <= threshold) Color.BLACK else Color.WHITE
            pixels[i] = binary
        }
        output.setPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
        gray.recycle()
        return output
    }

    /**
     * Adaptive local threshold (Sauvola-style using local mean) for pages
     * with uneven lighting. Costlier than Otsu but yields clean text where a
     * global threshold clips at the wrong luminance.
     *
     * @param window neighborhood radius (pixels) used to estimate local mean.
     * @param k scaling factor in the range (0, 1); lower keeps more ink.
     */
    fun toBinaryAdaptive(
        input: Bitmap,
        window: Int = 8,
        k: Double = 0.2
    ): Bitmap {
        val gray = toGrayscale(input)
        val w = gray.width
        val h = gray.height
        val pixels = IntArray(w * h)
        gray.getPixels(pixels, 0, w, 0, 0, w, h)
        val grayVals = IntArray(w * h)
        for (i in pixels.indices) grayVals[i] = Color.red(pixels[i])

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0
                var count = 0
                val y0 = (y - window).coerceAtLeast(0)
                val y1 = (y + window).coerceAtMost(h - 1)
                val x0 = (x - window).coerceAtLeast(0)
                val x1 = (x + window).coerceAtMost(w - 1)
                for (yy in y0..y1) {
                    val rowOffset = yy * w
                    for (xx in x0..x1) {
                        sum += grayVals[rowOffset + xx]
                        count++
                    }
                }
                val mean = sum.toDouble() / count
                val value = grayVals[y * w + x]
                val t = mean * (1.0 - k)
                outPixels[y * w + x] = if (value <= t) Color.BLACK else Color.WHITE
            }
        }
        output.setPixels(outPixels, 0, w, 0, 0, w, h)
        gray.recycle()
        return output
    }

    /**
     * Apply a [ScanFilter] to [input], returning a new bitmap. [ORIGINAL]
     * returns a copy (caller may decide to skip allocation).
     */
    fun apply(input: Bitmap, filter: ScanFilter): Bitmap = when (filter) {
        ScanFilter.ORIGINAL -> input.copy(Bitmap.Config.ARGB_8888, false)
        ScanFilter.GRAYSCALE -> toGrayscale(input)
        ScanFilter.BINARY -> toBinaryAdaptive(input)
    }
}
