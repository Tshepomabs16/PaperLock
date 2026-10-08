package com.mabsSD.toolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mabsSD.toolbox.ui.ToolboxApp
import com.mabsSD.toolbox.ui.theme.ThemeMode
import com.mabsSD.toolbox.ui.theme.ToolboxTheme
import com.mabsSD.toolbox.ui.toolboxContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = toolboxContainer()
            val themeMode by container.preferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            ToolboxTheme(themeMode = themeMode) {
                ToolboxApp()
            }
        }
    }
}
