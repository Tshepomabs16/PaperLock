package com.mabsSD.toolbox.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * The B&W filter was rewritten from a direct window sum to a summed-area
 * table for speed. These tests pin it to the original algorithm so the
 * rewrite can't quietly change what a scanned page looks like.
 */
@RunWith(AndroidJUnit4::class)
class ImageProcessorTest {

    /** A page-like image: light noisy background with dark strokes. */
    private fun pageLike(w: Int, h: Int, seed: Int = 7): Bitmap {
        val rnd = Random(seed)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (y in 0 until h) for (x in 0 until w) {
            val ink = (y % 23 in 9..12) && (x % 37 < 30)
            val base = if (ink) 30 else 215 - (x * 40 / w) // uneven lighting
            val v = (base + rnd.nextInt(-18, 19)).coerceIn(0, 255)
            bmp.setPixel(x, y, Color.rgb(v, v, v))
        }
        return bmp
    }

    /** The pre-rewrite implementation, kept here as the reference. */
    private fun referenceAdaptive(input: Bitmap, window: Int, k: Double = 0.2): IntArray {
        val w = input.width
        val h = input.height
        val px = IntArray(w * h)
        input.getPixels(px, 0, w, 0, 0, w, h)
        val gray = IntArray(w * h) {
            val p = px[it]
            (299 * Color.red(p) + 587 * Color.green(p) + 114 * Color.blue(p)) / 1000
        }
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var sum = 0
            var count = 0
            for (yy in maxOf(0, y - window)..minOf(h - 1, y + window)) {
                for (xx in maxOf(0, x - window)..minOf(w - 1, x + window)) {
                    sum += gray[yy * w + xx]; count++
                }
            }
            val mean = sum.toDouble() / count
            out[y * w + x] = if (gray[y * w + x] <= mean * (1.0 - k)) Color.BLACK else Color.WHITE
        }
        return out
    }

    private fun pixelsOf(bmp: Bitmap) = IntArray(bmp.width * bmp.height).also {
        bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height)
    }

    @Test
    fun binary_matchesReferenceImplementationExactly() {
        // The window scales with the long side (8px at 2400px), so a 360px
        // image uses max(2, round(8 * 360 / 2400)) = 2.
        val src = pageLike(360, 240)
        val window = maxOf(2, Math.round(8 * 360 / 2400f))
        val expected = referenceAdaptive(src, window)

        val actual = src.copy(Bitmap.Config.ARGB_8888, true)
        ImageProcessor.applyInPlace(actual, ScanFilter.BINARY)

        val got = pixelsOf(actual)
        val mismatches = got.indices.count { got[it] != expected[it] }
        assertEquals("pixels differing from the reference algorithm", 0, mismatches)
    }

    @Test
    fun binary_atFullScanWindow_matchesReference() {
        // Exercise the export-size window (8) on a cropped strip so the slow
        // reference stays fast: same long side as a real page, short height.
        val src = pageLike(2400, 40, seed = 3)
        val expected = referenceAdaptive(src, 8)
        val actual = src.copy(Bitmap.Config.ARGB_8888, true)
        ImageProcessor.applyInPlace(actual, ScanFilter.BINARY)
        val got = pixelsOf(actual)
        assertEquals(0, got.indices.count { got[it] != expected[it] })
    }

    @Test
    fun binary_outputIsStrictlyBlackAndWhite() {
        val bmp = pageLike(200, 120).copy(Bitmap.Config.ARGB_8888, true)
        ImageProcessor.applyInPlace(bmp, ScanFilter.BINARY)
        assertTrue(pixelsOf(bmp).all { it == Color.BLACK || it == Color.WHITE })
    }

    @Test
    fun grayscale_producesNeutralGreys() {
        val bmp = Bitmap.createBitmap(4, 1, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, Color.RED); setPixel(1, 0, Color.GREEN)
            setPixel(2, 0, Color.BLUE); setPixel(3, 0, Color.WHITE)
        }
        ImageProcessor.applyInPlace(bmp, ScanFilter.GRAYSCALE)
        pixelsOf(bmp).forEach {
            assertEquals(Color.red(it), Color.green(it))
            assertEquals(Color.green(it), Color.blue(it))
        }
        assertEquals(255, Color.red(bmp.getPixel(3, 0)))
    }

    @Test
    fun original_leavesPixelsUntouched() {
        val src = pageLike(50, 50)
        val before = pixelsOf(src)
        ImageProcessor.applyInPlace(src, ScanFilter.ORIGINAL)
        assertTrue(before.contentEquals(pixelsOf(src)))
    }

    @Test
    fun apply_doesNotModifyTheSource() {
        val src = pageLike(80, 60)
        val before = pixelsOf(src)
        val out = ImageProcessor.apply(src, ScanFilter.BINARY)
        assertTrue(before.contentEquals(pixelsOf(src)))
        assertTrue(!before.contentEquals(pixelsOf(out)))
    }
}
