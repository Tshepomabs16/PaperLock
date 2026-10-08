package com.mabsSD.toolbox.utils

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/**
 * Compresses a single bitmap to a target size via downscale-then-requality
 * search: the same two-knob idea as [com.mabsSD.toolbox.pdf.PdfCompressor],
 * applied to one image instead of summing across pages.
 *
 * Kept as a separate, independently written search rather than sharing code
 * with PdfCompressor: that one aggregates a *sum* across pages, this one a
 * single value, and forcing both through one generic abstraction for two
 * call sites would cost more than the ~40 lines it would save.
 */
object ImageCompressor {

    /** Below this, JPEG artifacts start eating into legibility. */
    private const val MIN_JPEG_QUALITY = 30
    private const val MAX_JPEG_QUALITY = 92

    /** Tried in order; the first that hits the target at MAX quality wins outright. */
    private val SCALE_STEPS = listOf(1.0f, 0.8f, 0.65f, 0.5f, 0.35f)

    private const val MAX_BISECT_ITERATIONS = 7

    data class Outcome(val bytes: ByteArray, val hitTarget: Boolean)

    fun compressToTarget(
        source: Bitmap,
        targetBytes: Long,
        onProgress: (Float, String) -> Unit = { _, _ -> },
    ): Outcome {
        var smallestSeen: ByteArray? = null
        var winner: ByteArray? = null

        searchScale@ for ((stepIndex, scale) in SCALE_STEPS.withIndex()) {
            val scaled = scaledCopyOf(source, scale)
            try {
                fun progressAt(withinStep: Float, message: String) =
                    onProgress((stepIndex + withinStep) / SCALE_STEPS.size, message)

                val atMax = encode(scaled, MAX_JPEG_QUALITY)
                if (atMax.size <= targetBytes) {
                    winner = atMax
                    break@searchScale
                }
                if (smallestSeen == null || atMax.size < smallestSeen!!.size) smallestSeen = atMax

                val atMin = encode(scaled, MIN_JPEG_QUALITY)
                progressAt(0.4f, "Trying ${(scale * 100).toInt()}% size...")
                if (atMin.size < smallestSeen!!.size) smallestSeen = atMin

                if (atMin.size > targetBytes) {
                    // Even the floor quality doesn't fit at this scale; only a
                    // smaller image can help, so don't bisect quality here.
                    continue@searchScale
                }

                var lo = MIN_JPEG_QUALITY
                var hi = MAX_JPEG_QUALITY
                var candidate = atMin
                var iteration = 0
                while (hi - lo > 4 && iteration < MAX_BISECT_ITERATIONS) {
                    val mid = (lo + hi) / 2
                    val trial = encode(scaled, mid)
                    iteration++
                    progressAt(
                        0.5f + (iteration.toFloat() / MAX_BISECT_ITERATIONS) * 0.5f,
                        "Trying quality $mid...",
                    )
                    if (trial.size <= targetBytes) {
                        candidate = trial
                        lo = mid
                    } else {
                        hi = mid
                    }
                }
                winner = candidate
                break@searchScale
            } finally {
                if (scaled !== source) scaled.recycle()
            }
        }

        val chosen = winner ?: smallestSeen ?: error("SCALE_STEPS must not be empty")
        return Outcome(bytes = chosen, hitTarget = winner != null)
    }

    private fun scaledCopyOf(source: Bitmap, scale: Float): Bitmap {
        if (scale >= 1.0f) return source
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun encode(bitmap: Bitmap, quality: Int): ByteArray =
        ByteArrayOutputStream().use { buffer ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, buffer)
            buffer.toByteArray()
        }
}
