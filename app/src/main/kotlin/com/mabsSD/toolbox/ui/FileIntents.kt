package com.mabsSD.toolbox.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/** MIME type from a result's file name; results always carry a real extension. */
fun mimeTypeFor(fileName: String): String = when {
    fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
    fileName.endsWith(".jpg", ignoreCase = true) ||
        fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
    fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
    fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
    else -> "application/octet-stream"
}

/**
 * Opens the system share sheet for a result.
 *
 * The file:// URI is converted to a FileProvider content:// URI first — handing
 * a file:// URI to another app throws FileUriExposedException, which is what
 * crashed this button. The grant is attached via ClipData as well as the flag
 * so it survives the trip through the chooser to whichever app is picked.
 */
fun shareResult(context: Context, uri: Uri, fileName: String) {
    val shareable = context.toolboxContainer().workingFileManager.shareableUri(uri)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeTypeFor(fileName)
        putExtra(Intent.EXTRA_STREAM, shareable)
        clipData = ClipData.newRawUri(fileName, shareable)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, "Share $fileName").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

/** Opens a result in another app. Returns false if nothing on the device can. */
fun openResult(context: Context, uri: Uri, fileName: String): Boolean {
    val shareable = context.toolboxContainer().workingFileManager.shareableUri(uri)
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(shareable, mimeTypeFor(fileName))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(view)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/**
 * "Save to…" with a MIME type chosen per file.
 *
 * The stock CreateDocument contract fixes its MIME type at construction. It
 * was built with application/octet-stream, so the system picker didn't know
 * `.pdf` was the extension and de-duplicated names by appending after it —
 * producing files like "scan.pdf (1)" that won't open as PDFs.
 */
class SaveAs : ActivityResultContract<SaveAs.Request, Uri?>() {
    data class Request(val fileName: String, val mimeType: String)

    override fun createIntent(context: Context, input: Request): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}
