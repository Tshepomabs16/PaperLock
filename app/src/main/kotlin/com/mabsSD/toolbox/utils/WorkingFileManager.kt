package com.mabsSD.toolbox.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

class WorkingFileManager(private val context: Context) {

    private val cacheDir: File
        get() = context.cacheDir

    fun init() {
        cacheDir.mkdirs()
        cleanupOrphanedFiles()
    }

    fun createTempFile(prefix: String = "toolbox_", extension: String = ""): Uri {
        val file = File.createTempFile(
            prefix,
            if (extension.isNotEmpty()) ".$extension" else null,
            cacheDir
        )
        return Uri.fromFile(file)
    }

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
        return try {
            val file = File(uri.path!!)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun getFileName(uri: Uri): String? {
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
}
