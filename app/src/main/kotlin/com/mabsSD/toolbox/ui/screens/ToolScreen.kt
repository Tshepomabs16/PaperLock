package com.mabsSD.toolbox.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mabsSD.toolbox.tools.Tool
import com.mabsSD.toolbox.tools.ToolInput
import com.mabsSD.toolbox.tools.ToolInputType
import com.mabsSD.toolbox.tools.ToolProgress
import com.mabsSD.toolbox.tools.ToolRegistry
import com.mabsSD.toolbox.tools.ToolRunner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScreen(
    toolId: String,
    onNavigateToResult: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val tool: Tool? = remember(toolId) { ToolRegistry.get(toolId) }
    val runner = remember { ToolRunner() }
    val runnerState by runner.state.collectAsState()

    val pendingInputType = remember { mutableStateOf<ToolInputType?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val type = pendingInputType.value
        if (uri != null && type != null && tool != null) {
            runner.runTool(tool, listOf(ToolInput(uri, type)))
        }
        pendingInputType.value = null
    }

    DisposableEffect(runner) {
        onDispose { runner.dispose() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool?.title ?: "Tool") },
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
        when {
            tool == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Unknown tool: $toolId")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onBack) { Text("Go back") }
                }
            }

            runnerState is ToolProgress.Complete -> {
                // Navigate once the tool finishes.
                androidx.compose.runtime.LaunchedEffect(Unit) { onNavigateToResult() }
            }

            runnerState is ToolProgress.Error -> {
                ErrorState(
                    message = (runnerState as ToolProgress.Error).message,
                    onRetry = { runner.reset() },
                    onBack = onBack,
                    modifier = Modifier.padding(padding)
                )
            }

            runnerState is ToolProgress.Running -> {
                RunningState(
                    message = (runnerState as ToolProgress.Running).message,
                    progress = (runnerState as ToolProgress.Running).progress,
                    onCancel = { runner.cancel() },
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                // Idle: show the input picker.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tool.description,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text("Select a file, then run the tool.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))

                    tool.acceptedInputs.forEach { inputType ->
                        val label = when (inputType) {
                            ToolInputType.PDF -> "Pick a PDF"
                            ToolInputType.IMAGE -> "Pick an image"
                            ToolInputType.ANY -> "Pick a file"
                        }
                        Button(
                            onClick = {
                                pendingInputType.value = inputType
                                when (inputType) {
                                    ToolInputType.PDF -> picker.launch(arrayOf("application/pdf"))
                                    ToolInputType.IMAGE -> picker.launch(arrayOf("image/*"))
                                    ToolInputType.ANY -> picker.launch(arrayOf("*/*"))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningState(
    message: String,
    progress: Float,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}
