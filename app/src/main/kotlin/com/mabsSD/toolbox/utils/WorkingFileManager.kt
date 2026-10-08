package com.mabsSD.toolbox.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

/**
 * Owns where the app's files live.
 *
 * Two places, deliberately separate:
 *  - [outputsDir] (`filesDir/outputs`) holds every tool result. History rows
 *    point at these, so they are never swept automatically — they leave only
 *    when the user deletes them from Files.
 *  - `cacheDir` is scratch space. Anything there older than a day is swept on
 *    launch. Results used to live here too, which meant the Files tab filled
 *    up with entries whose files had been silently deleted a day later.
 */
class WorkingFileManager(private val context: Context) {

    private val cacheDir: File
        get() = context.cacheDir

    val outputsDir: File
        get() = File(context.filesDir, OUTPUTS_DIR).apply { mkdirs() }

    fun init() {
        cacheDir.mkdirs()
        outputsDir.mkdirs()
        cleanupOrphanedFiles()
    }

    /** A new, uniquely named file for a tool result. */
    fun createTempFile(prefix: String = "toolbox_", extension: String = ""): Uri {
        val file = File.createTempFile(
            prefix,
            if (extension.isNotEmpty()) ".$extension" else null,
            outputsDir
        )
        return Uri.fromFile(file)
    }

    /** Sweeps scratch files only; results in [outputsDir] are never touched. */
    fun cleanupOrphanedFiles() {
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && isOrphaned(file)) {
                file.delete()
            }
        }
    }

    private fun isOrphaned(file: File): Boolean {
        val age = System.currentTimeMillis() - file.lastModified()
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours
        return age > maxAge
    }

    fun deleteFile(uri: Uri): Boolean {
        if (uri.scheme != "file") return false
        return try {
            File(uri.path!!).delete()
        } catch (e: Exception) {
            false
        }
    }

    /** True if the result still exists on disk. Content URIs are assumed present. */
    fun exists(uri: Uri): Boolean =
        uri.scheme != "file" || uri.path?.let { File(it).exists() } == true

    /**
     * A URI another app is allowed to read.
     *
     * Results are addressed internally as file:// paths in app-private
     * storage. Handing one of those to another app throws
     * FileUriExposedException on Android 7+, which is what crashed Share and
     * Open. FileProvider serves the same file as a content:// URI that the
     * receiving app can read once granted.
     */
    fun shareableUri(uri: Uri): Uri {
        if (uri.scheme != "file") return uri
        val file = File(uri.path ?: return uri)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun getFileName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }

    /**
     * Size in bytes, for both content:// and file:// URIs.
     *
     * Tool outputs come from createTempFile, which returns Uri.fromFile, and
     * ContentResolver.query returns null for the file:// scheme. Querying alone
     * therefore reported 0 B for every result the app produced, even when the
     * bytes were on disk. Check the filesystem first, then fall back to the
     * resolver for content:// inputs.
     */
    fun getFileSize(uri: Uri): Long {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it).length() } ?: 0L
        }
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
            } else 0L
        } ?: 0L
    }

    companion object {
        const val OUTPUTS_DIR = "outputs"
    }
}
