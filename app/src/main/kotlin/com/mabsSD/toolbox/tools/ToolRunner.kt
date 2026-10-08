package com.mabsSD.toolbox.tools

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ToolRunner {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentJob: Job? = null

    private val _state = MutableStateFlow<ToolProgress>(ToolProgress.Idle)
    val state: StateFlow<ToolProgress> = _state.asStateFlow()

    fun runTool(
        tool: Tool,
        inputs: List<ToolInput>,
        params: Map<String, Any> = emptyMap()
    ) {
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                _state.value = ToolProgress.Running(0f, "Starting ${tool.title}...")
                val result = tool.run(inputs, params) { progress ->
                    _state.value = progress
                }
                ResultStore.set(result, tool.id)
                _state.value = ToolProgress.Complete(result)
            } catch (e: CancellationException) {
                _state.value = ToolProgress.Idle
                throw e
            } catch (e: Exception) {
                _state.value = ToolProgress.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        _state.value = ToolProgress.Idle
    }

    fun reset() {
        cancel()
    }

    fun dispose() {
        scope.cancel()
    }
}
