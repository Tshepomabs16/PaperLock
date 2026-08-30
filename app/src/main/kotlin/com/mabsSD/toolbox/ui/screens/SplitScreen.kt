package com.mabsSD.toolbox.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
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
import com.mabsSD.toolbox.pdf.PageRange
import com.mabsSD.toolbox.pdf.PdfError
import com.mabsSD.toolbox.tools.SplitTool
import com.mabsSD.toolbox.tools.ToolProgress
import com.mabsSD.toolbox.tools.ToolRunner
import com.mabsSD.toolbox.tools.pdfInput
import com.mabsSD.toolbox.ui.components.BorderedBlock
import com.mabsSD.toolbox.ui.components.PrimaryButton
import com.mabsSD.toolbox.ui.components.SectionLabel
import com.mabsSD.toolbox.ui.components.SecondaryButton
import com.mabsSD.toolbox.ui.components.ToolboxTopBar
import com.mabsSD.toolbox.ui.toolboxContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri

@Composable
fun SplitScreen(
    onNavigateToResult: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = remember { context.toolboxContainer() }
    val runner = remember { ToolRunner() }
    val runnerState by runner.state.collectAsState()

    var source by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var rangeText by remember { mutableStateOf("") }

    DisposableEffect(runner) { onDispose { runner.dispose() } }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            source = uri
            rangeText = ""
            loadError = null
            pageCount = 0
        }
    }

    // Reading the page count needs to open the document, so it happens off the
    // main thread and reports the same typed errors the engine raises.
    LaunchedEffect(source) {
        val uri = source ?: return@LaunchedEffect
        val outcome = withContext(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure<Int>(PdfError.Damaged())
                Result.success(container.pdfEngine.readInfo(stream).pageCount)
            } catch (e: PdfError) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(PdfError.Damaged())
            }
        }
        outcome.onSuccess {
            pageCount = it
            sourceName = container.workingFileManager.getFileName(uri)
            rangeText = if (it > 1) "1-$it" else "1"
        }.onFailure {
            loadError = it.message
            source = null
        }
    }

    LaunchedEffect(runnerState) {
        if (runnerState is ToolProgress.Complete) onNavigateToResult()
    }

    val parsed = remember(rangeText, pageCount) {
        if (pageCount == 0) null else PageRange.parse(rangeText, pageCount)
    }
    val selected = (parsed as? PageRange.Result.Pages)?.indices

    Scaffold(
        topBar = { ToolboxTopBar(title = "Split", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        val running = runnerState as? ToolProgress.Running
        if (running != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
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
                    "Pull selected pages out of a PDF into a new file. The original is left untouched.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Spacer(Modifier.height(24.dp))

            val uri = source
            if (uri == null) {
                loadError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                PrimaryButton(
                    text = "Choose a PDF",
                    onClick = { picker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Column
            }

            SectionLabel("Document")
            Spacer(Modifier.height(6.dp))
            Text(
                sourceName ?: "Selected PDF",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (pageCount == 1) "1 page" else "$pageCount pages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            SectionLabel("Pages to extract")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = rangeText,
                onValueChange = { rangeText = it },
                singleLine = true,
                shape = RectangleShape,
                placeholder = { Text("e.g. 1-3, 5") },
                isError = parsed is PageRange.Result.Invalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            when (val p = parsed) {
                is PageRange.Result.Invalid -> Text(
                    p.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                is PageRange.Result.Pages -> Text(
                    PageRange.describe(p.indices),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                null -> Unit
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
                text = "Extract pages",
                onClick = {
                    val pages = selected ?: return@PrimaryButton
                    runner.runTool(
                        container.splitTool,
                        listOf(pdfInput(uri)),
                        mapOf(SplitTool.PARAM_PAGES to pages),
                    )
                },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            SecondaryButton(
                text = "Choose another PDF",
                onClick = { picker.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
