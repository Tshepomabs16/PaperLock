package com.mabsSD.toolbox.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

class SafManager(private val context: Context) {

    fun createOpenDocumentIntent(mimeTypes: Array<String>, allowMultiple: Boolean = false): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
            if (mimeTypes.size > 1) {
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        }
    }

    fun createCreateDocumentIntent(fileName: String, mimeType: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
    }

    fun createPickImageIntent(allowMultiple: Boolean = false): Intent {
        return createOpenDocumentIntent(
            mimeTypes = arrayOf("image/*"),
            allowMultiple = allowMultiple
        )
    }

    fun createPickPdfIntent(allowMultiple: Boolean = false): Intent {
        return createOpenDocumentIntent(
            mimeTypes = arrayOf("application/pdf"),
            allowMultiple = allowMultiple
        )
    }

    fun getSelectedUris(data: Intent?): List<Uri> {
        if (data == null) return emptyList()

        val clipData = data.clipData
        if (clipData != null) {
            return (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
        }

        return data.data?.let { listOf(it) } ?: emptyList()
    }
}
