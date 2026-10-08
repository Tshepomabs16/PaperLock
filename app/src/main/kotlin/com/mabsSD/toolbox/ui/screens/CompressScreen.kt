package com.mabsSD.toolbox.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mabsSD.toolbox.tools.CompressTool
import com.mabsSD.toolbox.tools.ToolInput
import com.mabsSD.toolbox.tools.ToolInputType
import com.mabsSD.toolbox.tools.ToolProgress
import com.mabsSD.toolbox.tools.ToolRunner
import com.mabsSD.toolbox.ui.components.BorderedBlock
import com.mabsSD.toolbox.ui.components.PrimaryButton
import com.mabsSD.toolbox.ui.components.SectionLabel
import com.mabsSD.toolbox.ui.components.SecondaryButton
import com.mabsSD.toolbox.ui.components.ToolboxTopBar
import com.mabsSD.toolbox.ui.toolboxContainer
import com.mabsSD.toolbox.utils.formatFileSize

/** P3-04: fixed presets plus a custom field, sized in MB since that is what upload portals quote. */
private val PRESETS_MB = listOf(1, 2, 5)

@Composable
fun CompressScreen(
    onNavigateToResult: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = remember { context.toolboxContainer() }
    val runner = remember { ToolRunner() }
    val runnerState by runner.state.collectAsState()

    var source by remember { mutableStateOf<Uri?>(null) }
    var sourceType by remember { mutableStateOf<ToolInputType?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var sourceSize by remember { mutableStateOf(0L) }

    var selectedPresetMb by remember { mutableStateOf<Int?>(2) }
    var customMb by remember { mutableStateOf("") }

    DisposableEffect(runner) { onDispose { runner.dispose() } }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) loadSource(context, uri, ToolInputType.PDF) { s, t, n, sz ->
        source = s; sourceType = t; sourceName = n; sourceSize = sz
    } }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) loadSource(context, uri, ToolInputType.IMAGE) { s, t, n, sz ->
        source = s; sourceType = t; sourceName = n; sourceSize = sz
    } }

    LaunchedEffect(runnerState) {
        if (runnerState is ToolProgress.Complete) onNavigateToResult()
    }

    val targetMb: Int? = selectedPresetMb ?: customMb.toIntOrNull()?.takeIf { it > 0 }
    val targetBytes = targetMb?.let { it * 1024L * 1024L }

    Scaffold(
        topBar = { ToolboxTopBar(title = "Compress", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        val running = runnerState as? ToolProgress.Running
        if (running != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(running.message, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                SecondaryButton("Cancel", { runner.cancel() })
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            BorderedBlock {
                SectionLabel("What this does")
                Text(
                    "Shrink a PDF or image toward a target size. Quality is never pushed " +
                        "past the point where text stops being readable.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Spacer(Modifier.height(24.dp))

            val uri = source
            if (uri == null) {
                SectionLabel("Choose a file")
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    text = "Choose a PDF",
                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = "Choose an image",
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Column
            }

            SectionLabel("File")
            Spacer(Modifier.height(6.dp))
            Text(sourceName ?: "Selected file", style = MaterialTheme.typography.titleMedium)
            Text(
                formatFileSize(sourceSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            SectionLabel("Target size")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESETS_MB.forEach { mb ->
                    PresetChip(
                        label = "${mb} MB",
                        selected = selectedPresetMb == mb,
                        onClick = { selectedPresetMb = mb; customMb = "" },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = customMb,
                onValueChange = { value ->
                    customMb = value.filter { it.isDigit() }
                    if (customMb.isNotEmpty()) selectedPresetMb = null
                },
                singleLine = true,
                shape = RectangleShape,
                label = { Text("Custom size, in MB") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            if (sourceSize > 0 && targetBytes != null && targetBytes >= sourceSize) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "That target is already bigger than the source file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            (runnerState as? ToolProgress.Error)?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = "Compress",
                onClick = {
                    val bytes = targetBytes ?: return@PrimaryButton
                    val type = sourceType ?: return@PrimaryButton
                    runner.runTool(
                        container.compressTool,
                        listOf(ToolInput(uri, type)),
                        mapOf(CompressTool.PARAM_TARGET_BYTES to bytes),
                    )
                },
                enabled = targetMb != null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            SecondaryButton(
                text = "Choose another file",
                onClick = { source = null },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .background(if (selected) scheme.primary else scheme.background)
            .border(2.dp, scheme.onBackground, RectangleShape)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.onPrimary else scheme.onBackground,
        )
    }
}

/** Reads the picked file's display name and size before any tool touches it. */
private fun loadSource(
    context: android.content.Context,
    uri: Uri,
    type: ToolInputType,
    onLoaded: (Uri, ToolInputType, String?, Long) -> Unit,
) {
    val container = context.toolboxContainer()
    val name = container.workingFileManager.getFileName(uri)
    val size = container.workingFileManager.getFileSize(uri)
    onLoaded(uri, type, name, size)
}
