package com.mabsSD.toolbox

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
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
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // enableEdgeToEdge() alone picks status-bar icon colour from the
            // *phone's* theme. With an in-app override (Dark while the phone is
            // Light, or the reverse) that left dark icons on a dark background
            // — the clock and battery were effectively invisible. Re-apply it
            // whenever the resolved theme changes.
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            ToolboxTheme(themeMode = themeMode) {
                ToolboxApp()
            }
        }
    }
}
