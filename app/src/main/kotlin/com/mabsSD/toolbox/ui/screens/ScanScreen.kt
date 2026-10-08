package com.mabsSD.toolbox.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.mabsSD.toolbox.ui.components.PrimaryButton
import com.mabsSD.toolbox.ui.components.ToolboxTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.mabsSD.toolbox.tools.ResultStore
import com.mabsSD.toolbox.tools.ToolResult
import com.mabsSD.toolbox.utils.ImageProcessor
import com.mabsSD.toolbox.utils.PdfExporter
import com.mabsSD.toolbox.utils.PdfPageSpec
import com.mabsSD.toolbox.utils.ScanFilter
import com.mabsSD.toolbox.utils.WorkingFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One scanned page. Only a small thumbnail is held in memory; the full page
 * stays on disk at [uri] (the scanner's output file) and is decoded when it
 * is actually needed — the on-screen preview of the selected page, or one at
 * a time during export.
 *
 * This used to hold every page as a full-resolution bitmap (~16 MB each at
 * 2400px), plus a full-size filtered copy per thumbnail: a 30-page scan asked
 * for close to 1 GB, on a phone that also has Play services' scanner running.
 */
private data class ScanPage(
    val uri: Uri,
    val thumb: Bitmap,
    val rotation: Int = 0,
    val filter: ScanFilter = ScanFilter.BINARY
)

private const val THUMB_MAX_PX = 360
private const val PREVIEW_MAX_PX = 1400
private const val EXPORT_MAX_PX = 2400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateToResult: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var pages by remember { mutableStateOf<List<ScanPage>>(emptyList()) }
    var selectedPageIndex by remember { mutableStateOf(0) }
    var exporting by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf("Writing PDF…") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(30)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .build()
        )
    }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val pageUris: List<Uri> = scanResult?.pages?.map { it.imageUri } ?: emptyList()
                if (pageUris.isNotEmpty()) {
                    loadThumbnails(scope, context, pageUris) { added ->
                        // Append: "Add pages" used to replace every page
                        // already in the tray with the new scan.
                        val firstNew = pages.size
                        pages = pages + added
                        selectedPageIndex = firstNew.coerceAtMost(pages.lastIndex.coerceAtLeast(0))
                        errorMessage = if (added.size < pageUris.size) {
                            "${pageUris.size - added.size} page(s) couldn't be read and were skipped."
                        } else null
                    }
                } else {
                    // Success with nothing attached. Happens when ML Kit's delegate
                    // logs "Failed to handle result" and still returns RESULT_OK.
                    errorMessage = "The scanner finished but returned no pages. Please try again."
                }
            }
            // Backing out of the camera is a normal exit, not a failure.
            Activity.RESULT_CANCELED -> Unit
            else -> errorMessage = "The scanner stopped unexpectedly (result code ${result.resultCode})."
        }
    }

    fun startScan() {
        val activity = context as? Activity
            ?: run {
                errorMessage = "Scanner needs an activity context"
                return
            }
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                val send = IntentSenderRequest.Builder(intentSender).build()
                scanLauncher.launch(send)
            }
            .addOnFailureListener {
                errorMessage = "Could not launch the scanner"
            }
    }

    if (pages.isEmpty() && !exporting) {
        Scaffold(
            topBar = {
                ToolboxTopBar(title = "Scan to PDF", onBack = onBack)
            },
            modifier = modifier.fillMaxSize()
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Photograph pages with your camera or import from your gallery. Everything stays on this device.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Start scanning", { startScan() })
                errorMessage?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
        }
        return
    }

    val currentPage = pages.getOrNull(selectedPageIndex)

    Scaffold(
        topBar = {
            ToolboxTopBar(title = "Pages (${pages.size})", onBack = onBack)
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        if (exporting) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(exportStatus)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = currentPage?.filter == filter,
                        onClick = { setFilter(pages, selectedPageIndex, filter) { pages = it } },
                        label = { Text(filter.uiName) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                currentPage?.let { page ->
                    val preview by produceState<ImageBitmap?>(null, page.uri, page.filter, page.rotation) {
                        value = withContext(Dispatchers.Default) {
                            renderPage(context, page.uri, page.filter, page.rotation, PREVIEW_MAX_PX)
                                ?.asImageBitmap()
                        }
                    }
                    preview?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Page ${selectedPageIndex + 1} preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: CircularProgressIndicator()
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { rotateSelected(pages, selectedPageIndex, 90) { pages = it } }) {
                    Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate 90°")
                }
                IconButton(onClick = {
                    val removed = pages.filterIndexed { i, _ -> i != selectedPageIndex }
                    pages = removed
                    if (selectedPageIndex >= removed.size) {
                        selectedPageIndex = (removed.size - 1).coerceAtLeast(0)
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete page")
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(pages) { index, page ->
                    PageThumbnail(
                        page = page,
                        index = index,
                        selected = index == selectedPageIndex,
                        onClick = { selectedPageIndex = index }
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 88.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { startScan() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Add pages")
                        }
                    }
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(12.dp))

            PrimaryButton(
                text = if (pages.size == 1) "Export PDF (1 page)" else "Export PDF (${pages.size} pages)",
                onClick = {
                    exporting = true
                    exportStatus = "Writing PDF…"
                    errorMessage = null
                    doExport(scope, context, pages, onStatus = { exportStatus = it }) { result, error ->
                        exporting = false
                        if (result != null) {
                            ResultStore.set(result, "scan")
                            onNavigateToResult()
                        } else {
                            errorMessage = error ?: "Export failed"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PageThumbnail(
    page: ScanPage,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = modifier.padding(4.dp)) {
            // Filtered off the main thread from the small thumbnail only.
            val preview by produceState<ImageBitmap?>(null, page.thumb, page.filter, page.rotation) {
                value = withContext(Dispatchers.Default) {
                    val copy = page.thumb.copy(Bitmap.Config.ARGB_8888, true)
                    ImageProcessor.applyInPlace(copy, page.filter)
                    rotateAndRecycle(copy, page.rotation).asImageBitmap()
                }
            }
            Box(modifier = Modifier.size(width = 56.dp, height = 72.dp), contentAlignment = Alignment.Center) {
                preview?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Page ${index + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Text(
                text = (index + 1).toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

private fun setFilter(
    pages: List<ScanPage>,
    index: Int,
    filter: ScanFilter,
    onChanged: (List<ScanPage>) -> Unit
) {
    if (index in pages.indices) {
        val newList = pages.toMutableList()
        newList[index] = newList[index].copy(filter = filter)
        onChanged(newList)
    }
}

private fun rotateSelected(
    pages: List<ScanPage>,
    index: Int,
    degrees: Int,
    onChanged: (List<ScanPage>) -> Unit
) {
    if (index in pages.indices) {
        val newList = pages.toMutableList()
        val p = newList[index]
        newList[index] = p.copy(rotation = (p.rotation + degrees) % 360)
        onChanged(newList)
    }
}

/** Rotate [input] by [degrees], recycling it if a new bitmap was made. */
private fun rotateAndRecycle(input: Bitmap, degrees: Int): Bitmap {
    if (degrees % 360 == 0) return input
    val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
    if (rotated !== input) input.recycle()
    return rotated
}

/** Decode one page at [maxPx], filter it in place, rotate. Null if unreadable. */
private fun renderPage(context: Context, uri: Uri, filter: ScanFilter, rotation: Int, maxPx: Int): Bitmap? {
    val bitmap = PdfExporter.decodePage(context, uri, maxPx, mutable = true) ?: return null
    return try {
        ImageProcessor.applyInPlace(bitmap, filter)
        rotateAndRecycle(bitmap, rotation)
    } catch (e: OutOfMemoryError) {
        bitmap.recycle()
        null
    }
}

private fun loadThumbnails(
    scope: CoroutineScope,
    context: Context,
    uris: List<Uri>,
    onLoaded: (List<ScanPage>) -> Unit
) {
    scope.launch {
        val loaded = withContext(Dispatchers.IO) {
            uris.mapNotNull { uri ->
                PdfExporter.decodePage(context, uri, THUMB_MAX_PX)?.let { ScanPage(uri = uri, thumb = it) }
            }
        }
        onLoaded(loaded)
    }
}

private fun doExport(
    scope: CoroutineScope,
    context: Context,
    pages: List<ScanPage>,
    onStatus: (String) -> Unit,
    onResult: (ToolResult?, String?) -> Unit
) {
    scope.launch {
        var failure = "Could not write the PDF"
        val result = withContext(Dispatchers.IO) {
            val workingFileManager = WorkingFileManager(context)
            val outputUri = workingFileManager.createTempFile(prefix = "scan_", extension = "pdf")
            try {
                context.contentResolver.openOutputStream(outputUri)?.use { stream ->
                    // One page decoded, filtered, encoded and freed at a time.
                    PdfExporter.exportPages(pages.size, stream) { i ->
                        onStatus("Writing page ${i + 1} of ${pages.size}…")
                        val page = pages[i]
                        val bitmap = PdfExporter.decodePage(context, page.uri, EXPORT_MAX_PX, mutable = true)
                            ?: throw java.io.IOException("Page ${i + 1} couldn't be read.")
                        ImageProcessor.applyInPlace(bitmap, page.filter)
                        PdfPageSpec(bitmap = bitmap, rotationDegrees = page.rotation)
                    }
                } ?: throw java.io.IOException("Couldn't create the output file.")

                // Dated rather than a fixed "scan.pdf": every scan used to share
                // one name, so History and Save-to filled up with identical
                // entries the user couldn't tell apart.
                val stamp = java.text.SimpleDateFormat("yyyy-MM-dd HH.mm", java.util.Locale.US)
                    .format(java.util.Date())
                ToolResult(
                    outputUri = outputUri,
                    outputName = "Scan $stamp.pdf",
                    outputSize = workingFileManager.getFileSize(outputUri),
                )
            } catch (e: OutOfMemoryError) {
                workingFileManager.deleteFile(outputUri)
                failure = "The phone ran out of memory writing this PDF. Try exporting fewer pages at once."
                null
            } catch (e: Exception) {
                workingFileManager.deleteFile(outputUri)
                failure = e.message ?: failure
                null
            }
        }
        if (result != null) onResult(result, null) else onResult(null, failure)
    }
}
