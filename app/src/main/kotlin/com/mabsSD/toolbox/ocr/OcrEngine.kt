package com.mabsSD.toolbox.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bundled ML Kit text recognition, wrapped as a suspend function.
 *
 * "Bundled" is the point: the Latin model ships inside the APK
 * (mlkit-google-ocr-models/, ~4 MB) rather than downloading on first use, so
 * this runs with the device in airplane mode (P4-01's own acceptance test).
 *
 * ML Kit's own client class is `com.google.mlkit.vision.text.TextRecognition`,
 * not a Play-services download shim — the module descriptor class alongside it
 * is a Play-services compatibility artifact ML Kit ships regardless of variant,
 * not evidence that this call reaches the network.
 */
object OcrEngine {

    suspend fun recognize(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        return try {
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result -> cont.resume(result.text) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
                cont.invokeOnCancellation { recognizer.close() }
            }
        } finally {
            recognizer.close()
        }
    }
}
