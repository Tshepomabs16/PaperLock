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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ScanPage(
    val original: Bitmap,
    var rotation: Int = 0,
    var filter: ScanFilter = ScanFilter.BINARY
)

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
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    decodePages(context, pageUris) { decoded ->
                        pages = decoded
                        selectedPageIndex = 0
                        errorMessage = null
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
                TopAppBar(
                    title = { Text("Scan to PDF") },
                    navigationIcon = {
                        OutlinedButton(onClick = onBack) { Text("Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
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
                OutlinedButton(onClick = { startScan() }) {
                    Text("Start scanning")
                }
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
            TopAppBar(
                title = { Text("Pages (${pages.size})") },
                navigationIcon = {
                    OutlinedButton(onClick = onBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
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
                Text("Writing PDF…")
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

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                currentPage?.let { page ->
                    val preview = remember(page, page.filter, page.rotation) {
                        val filtered = ImageProcessor.apply(page.original, page.filter)
                        rotateBitmap(filtered, page.rotation)
                    }
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "Page ${selectedPageIndex + 1} preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
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

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    exporting = true
                    doExport(context, pages) { result, error ->
                        exporting = false
                        if (result != null) {
                            ResultStore.set(result)
                            onNavigateToResult()
                        } else {
                            errorMessage = error ?: "Export failed"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export PDF (${pages.size} pages)")
            }
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
            val preview = remember(page, page.filter) {
                rotateBitmap(ImageProcessor.apply(page.original, page.filter), page.rotation)
            }
            Image(
                bitmap = preview.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 56.dp, height = 72.dp)
            )
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

private fun rotateBitmap(input: Bitmap, degrees: Int): Bitmap {
    if (degrees % 360 == 0) return input
    val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
}

private fun decodePages(
    context: Context,
    uris: List<Uri>,
    onDecoded: (List<ScanPage>) -> Unit
) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.Main + Job()).launch {
        val decoded = withContext(Dispatchers.IO) {
            uris.mapNotNull { uri ->
                PdfExporter.decodePage(context, uri)?.let { ScanPage(it) }
            }
        }
        onDecoded(decoded)
    }
}

private fun doExport(
    context: Context,
    pages: List<ScanPage>,
    onResult: (ToolResult?, String?) -> Unit
) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.Main + Job()).launch {
        val result = withContext(Dispatchers.IO) {
            try {
                val workingFileManager = WorkingFileManager(context)
                val outputUri = workingFileManager.createTempFile(prefix = "scan_", extension = "pdf")
                context.contentResolver.openOutputStream(outputUri)?.use { stream ->
                    val specs = pages.map { page ->
                        val bitmap = ImageProcessor.apply(page.original, page.filter)
                        PdfPageSpec(bitmap = bitmap, rotationDegrees = page.rotation)
                    }
                    PdfExporter.exportToPdf(specs, stream)
                    specs.forEach { it.bitmap.recycle() }
                }
                val name = workingFileManager.getFileName(outputUri) ?: "scan.pdf"
                val size = workingFileManager.getFileSize(outputUri)
                ToolResult(outputUri = outputUri, outputName = name, outputSize = size)
            } catch (e: Exception) {
                null
            }
        }
        if (result != null) onResult(result, null) else onResult(null, "Could not write the PDF")
    }
}
