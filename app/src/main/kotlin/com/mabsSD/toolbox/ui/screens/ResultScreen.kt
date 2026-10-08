package com.mabsSD.toolbox.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.mabsSD.toolbox.ui.components.BorderedBlock
import com.mabsSD.toolbox.ui.components.SectionLabel
import com.mabsSD.toolbox.ui.components.PrimaryButton
import com.mabsSD.toolbox.ui.components.SecondaryButton
import com.mabsSD.toolbox.ui.components.ToolboxTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mabsSD.toolbox.tools.ResultStore
import com.mabsSD.toolbox.tools.ToolResult
import com.mabsSD.toolbox.utils.formatFileSize
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private fun mimeTypeFor(fileName: String): String = when {
    fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
    fileName.endsWith(".jpg") || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
    fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
    fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
    else -> "application/octet-stream"
}

private fun isImageFile(fileName: String): Boolean = when {
    fileName.endsWith(".png", ignoreCase = true) -> true
    fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> true
    fileName.endsWith(".webp", ignoreCase = true) -> true
    else -> false
}

private fun isTextFile(fileName: String): Boolean = fileName.endsWith(".txt", ignoreCase = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    outputUriArg: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val result: ToolResult? = remember(outputUriArg) { ResultStore.lastResult }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var textContent by remember { mutableStateOf<String?>(null) }
    var savedToast by remember { mutableStateOf(false) }
    var copiedToast by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(result?.outputUri) {
        preview = null
        textContent = null
        if (result == null) return@LaunchedEffect

        if (isImageFile(result.outputName)) {
            preview = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(result.outputUri)?.let { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }
        } else if (isTextFile(result.outputName)) {
            textContent = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(result.outputUri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { targetUri ->
        if (targetUri != null && result != null) {
            val source = result.outputUri
            coroutineScope.launch {
                savedToast = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(source)?.use { input ->
                            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        true
                    } catch (e: IOException) {
                        false
                    }
                }
                if (savedToast) {
                    ResultStore.updateLastResult(result.copy(outputUri = targetUri))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ToolboxTopBar(title = "Result", onBack = onBack)
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        if (result == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No result to show.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Go back", onBack)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            BorderedBlock {
                SectionLabel("Done — ${formatFileSize(result.outputSize)}")
                Text(
                    text = result.outputName,
                    style = MaterialTheme.typography.headlineSmall,
                )
                result.note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            preview?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Preview of ${result.outputName}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            textContent?.let { text ->
                BorderedBlock {
                    // Capped rather than independently scrollable: a scroll
                    // region nested inside this screen's own scroll would fight
                    // it for gestures. Copy and Share still act on the full
                    // text regardless of what is visually truncated here.
                    Text(
                        text = text.ifBlank { "(no text found)" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 14,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = if (copiedToast) "Copied" else "Copy text",
                    onClick = {
                        clipboard.setText(AnnotatedString(text))
                        copiedToast = true
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            // Save is the primary action and gets the full width. Three equal
            // buttons in one row could not fit their labels and truncated to
            // "Shar e" / "Ope n" on a 393dp screen.
            PrimaryButton(
                text = "Save to…",
                onClick = { saveLauncher.launch(result.outputName) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = "Share",
                    onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = mimeTypeFor(result.outputName)
                            putExtra(Intent.EXTRA_STREAM, result.outputUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, "Share ${result.outputName}"))
                    },
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "Open",
                    onClick = {
                        val open = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(result.outputUri, mimeTypeFor(result.outputName))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching {
                            context.startActivity(open)
                            Unit
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (savedToast) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Saved",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
