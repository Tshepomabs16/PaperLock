package com.mabsSD.toolbox.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.mabsSD.toolbox.tools.ToolInputType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** A file another app handed to Toolbox, already copied into our cache. */
data class SharedFile(val uri: Uri, val name: String, val type: ToolInputType)

/** What arrived from one share / "Open with": the usable files, and how many were not. */
data class IncomingBatch(val files: List<SharedFile>, val skipped: Int)

/**
 * What another app handed over, and where it is going. Lives in an
 * activity-scoped ViewModel so it survives rotation, and so a share opening a
 * fresh Toolbox window in the sender's task doesn't also redirect a Toolbox
 * window already open in the background.
 */
class IncomingViewModel : ViewModel() {
    private val _batch = MutableStateFlow<IncomingBatch?>(null)

    /** The latest share, shown by the chooser screen. */
    val batch: StateFlow<IncomingBatch?> = _batch

    private val _showChooser = MutableStateFlow(false)

    /** True once a share has been read and the chooser hasn't been opened for it yet. */
    val showChooser: StateFlow<Boolean> = _showChooser

    /** Shared files by the back stack entry of the tool screen they were handed to. */
    private val handoffs = mutableMapOf<String, List<Uri>>()

    fun receive(context: Context, intent: Intent) {
        viewModelScope.launch {
            val batch = IncomingFiles.read(context, intent) ?: return@launch
            _batch.value = batch
            _showChooser.value = true
        }
    }

    fun chooserShown() {
        _showChooser.value = false
    }

    fun handTo(backStackEntryId: String, files: List<SharedFile>) {
        handoffs[backStackEntryId] = files.map { it.uri }
    }

    /** Files handed to this tool screen; empty when the tool was opened normally. */
    fun filesFor(backStackEntryId: String): List<Uri> = handoffs[backStackEntryId].orEmpty()
}

/**
 * Reads files arriving through the share sheet (SEND / SEND_MULTIPLE) or
 * "Open with" (VIEW).
 *
 * Each file is copied into the cache straight away. The read grant that comes
 * with the intent lasts only as long as the receiving activity, so a rotation
 * or a long-running tool could otherwise lose access mid-job. A copy is also
 * just a file:// URI, which every tool already handles.
 */
object IncomingFiles {
    /** Whether [intent] is a share or "Open with" for Toolbox to take in. */
    fun isIncoming(intent: Intent?): Boolean =
        intent?.action in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE, Intent.ACTION_VIEW)

    /** Copies the intent's files into the cache; null if it carried none. */
    suspend fun read(context: Context, intent: Intent): IncomingBatch? {
        val uris = urisFrom(intent)
        if (uris.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "incoming/${UUID.randomUUID()}").apply { mkdirs() }
            val copied = uris.mapNotNull { copyIn(context, it, intent.type, dir) }
            IncomingBatch(copied, skipped = uris.size - copied.size)
        }
    }

    @Suppress("DEPRECATION")
    private fun urisFrom(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_VIEW -> listOfNotNull(intent.data)
        Intent.ACTION_SEND ->
            listOfNotNull(
                if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else intent.getParcelableExtra(Intent.EXTRA_STREAM)
            )
        Intent.ACTION_SEND_MULTIPLE ->
            (if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)).orEmpty()
        else -> emptyList()
    }.filter { it.scheme == "content" } // file:// would need storage permission, which Toolbox doesn't hold

    private fun copyIn(context: Context, uri: Uri, intentType: String?, dir: File): SharedFile? = try {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: intentType
        val rawName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        val type = when {
            mime == "application/pdf" || rawName?.endsWith(".pdf", ignoreCase = true) == true -> ToolInputType.PDF
            mime?.startsWith("image/") == true -> ToolInputType.IMAGE
            else -> null
        }
        type?.let {
            // The display name comes from another app; strip anything that could
            // act as a path separator so it can't escape [dir].
            val fallback = if (it == ToolInputType.PDF) "Shared document.pdf" else "Shared image.jpg"
            val cleaned = rawName?.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")?.trim()
                ?.takeIf { n -> n.isNotEmpty() && n != "." && n != ".." }
            // Tools tell PDFs from images by extension, and some apps share a
            // PDF named just "document". Make sure the copy says what it is.
            val name = when {
                cleaned == null -> fallback
                it == ToolInputType.PDF && !cleaned.endsWith(".pdf", ignoreCase = true) -> "$cleaned.pdf"
                it == ToolInputType.IMAGE && '.' !in cleaned ->
                    "$cleaned." + (mime?.substringAfter('/')?.takeIf { t -> t.all(Char::isLetterOrDigit) } // not "image/*"
                        ?.replace("jpeg", "jpg") ?: "jpg")
                else -> cleaned
            }
            // Two shared files can carry the same name; keep both.
            val stem = name.substringBeforeLast('.')
            val ext = name.substringAfterLast('.', "").let { e -> if (e.isEmpty()) "" else ".$e" }
            var dest = File(dir, name)
            var n = 1
            while (dest.exists()) dest = File(dir, "$stem (${n++})$ext")
            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            } ?: return null
            SharedFile(Uri.fromFile(dest), dest.name, it)
        }
    } catch (e: Exception) {
        // SecurityException (grant revoked), FileNotFound, IO — all mean "couldn't read it".
        null
    }
}
