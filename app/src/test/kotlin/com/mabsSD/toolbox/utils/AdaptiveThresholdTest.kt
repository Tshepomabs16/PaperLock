package com.mabsSD.toolbox.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The B&W filter's threshold, on plain luminance arrays so it runs on the JVM.
 * The bold-heading case is the bug found on a Galaxy Tab: thick strokes came
 * out as hollow outlines because the local window was narrower than the ink.
 */
class AdaptiveThresholdTest {

    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    /** A w×h page of [paper] luminance, with [ink] rectangles painted on. */
    private fun page(w: Int, h: Int, paper: (x: Int) -> Int, vararg ink: IntArray, inkLevel: Int = 25): IntArray {
        val px = IntArray(w * h) { paper(it % w) }
        for ((x0, y0, x1, y1) in ink.map { it.toList() }) {
            for (y in y0 until y1) for (x in x0 until x1) px[y * w + x] = inkLevel
        }
        return px
    }

    private fun fraction(px: IntArray, w: Int, x0: Int, y0: Int, x1: Int, y1: Int, value: Int): Double {
        var n = 0; var hit = 0
        for (y in y0 until y1) for (x in x0 until x1) { n++; if (px[y * w + x] == value) hit++ }
        return hit.toDouble() / n
    }

    @Test
    fun boldHeading_staysSolidBlack() {
        val w = 1200; val h = 800
        // An 80px-thick stroke, like a large bold heading.
        val px = page(w, h, { 225 }, intArrayOf(300, 300, 700, 380))
        ImageProcessor.adaptiveThreshold(px, w, h, ImageProcessor.windowFor(w, h))
        assertEquals(1.0, fraction(px, w, 300, 300, 700, 380, black), 0.0)
    }

    @Test
    fun boldHeading_wasHollowWithTheOldWindow() {
        // Documents the bug: the old 8px-at-2400px window (4px here) leaves the
        // middle of the stroke white. If this ever passes, the test above is
        // no longer exercising anything.
        val w = 1200; val h = 800
        val px = page(w, h, { 225 }, intArrayOf(300, 300, 700, 380))
        ImageProcessor.adaptiveThreshold(px, w, h, window = 4)
        assertTrue(fraction(px, w, 320, 320, 680, 360, white) > 0.9)
    }

    @Test
    fun thinText_isBlack_andPaperIsWhite() {
        val w = 1000; val h = 600
        val lines = (0 until 10).map { intArrayOf(100, 100 + it * 40, 900, 103 + it * 40) }.toTypedArray()
        val px = page(w, h, { 230 }, *lines, inkLevel = 40)
        ImageProcessor.adaptiveThreshold(px, w, h, ImageProcessor.windowFor(w, h))
        lines.forEach { (x0, y0, x1, y1) -> assertEquals(1.0, fraction(px, w, x0, y0, x1, y1, black), 0.0) }
        assertEquals(1.0, fraction(px, w, 0, 0, w, 90, white), 0.0)   // margin above the text
        assertEquals(1.0, fraction(px, w, 0, 520, w, h, white), 0.0)  // and below it
    }

    @Test
    fun unevenLighting_noShadowTurnsBlack() {
        // Paper darkening from 240 on the left to 140 on the right, as under a
        // lamp, with text at both ends.
        val w = 1200; val h = 400
        val px = page(w, h, { x -> 240 - 100 * x / w }, intArrayOf(80, 180, 300, 186), intArrayOf(900, 180, 1120, 186), inkLevel = 45)
        ImageProcessor.adaptiveThreshold(px, w, h, ImageProcessor.windowFor(w, h))
        assertEquals(1.0, fraction(px, w, 80, 180, 300, 186, black), 0.0)
        assertEquals(1.0, fraction(px, w, 900, 180, 1120, 186, black), 0.0)
        assertEquals(1.0, fraction(px, w, 0, 0, w, 150, white), 0.0)
        assertEquals(1.0, fraction(px, w, 0, 220, w, h, white), 0.0)
    }

    @Test
    fun output_isStrictlyBlackOrWhite() {
        val w = 300; val h = 200
        val px = IntArray(w * h) { (it * 7919) % 256 }
        ImageProcessor.adaptiveThreshold(px, w, h, ImageProcessor.windowFor(w, h))
        assertTrue(px.all { it == black || it == white })
    }

    @Test
    fun window_scalesWithTheLongSide() {
        assertEquals(150, ImageProcessor.windowFor(2400, 1800))
        assertEquals(150, ImageProcessor.windowFor(1800, 2400))
        assertEquals(22, ImageProcessor.windowFor(360, 270))
        assertEquals(2, ImageProcessor.windowFor(20, 10)) // never degenerate
    }
}
