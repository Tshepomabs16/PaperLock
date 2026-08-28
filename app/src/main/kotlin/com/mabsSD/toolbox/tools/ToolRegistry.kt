package com.mabsSD.toolbox.tools

object ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        tools[tool.id] = tool
    }

    fun get(id: String): Tool? = tools[id]

    fun getAll(): List<Tool> = tools.values.toList()

    fun getByInputType(type: ToolInputType): List<Tool> {
        return tools.values.filter { tool ->
            tool.acceptedInputs.contains(type) || tool.acceptedInputs.contains(ToolInputType.ANY)
        }
    }
}
