package com.mabsSD.toolbox

import android.content.Intent
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
import androidx.activity.viewModels
import com.mabsSD.toolbox.ui.IncomingFiles
import com.mabsSD.toolbox.ui.IncomingViewModel
import com.mabsSD.toolbox.ui.ToolboxApp
import com.mabsSD.toolbox.ui.theme.ThemeMode
import com.mabsSD.toolbox.ui.theme.ToolboxTheme
import com.mabsSD.toolbox.ui.toolboxContainer

class MainActivity : ComponentActivity() {
    private val incoming: IncomingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh start: after a rotation the same intent is redelivered
        // and the files have already been taken in.
        if (savedInstanceState == null) receive(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receive(intent)
    }

    /** Files shared to Toolbox or opened with it from another app. */
    private fun receive(intent: Intent?) {
        if (intent == null || !IncomingFiles.isIncoming(intent)) return
        incoming.receive(applicationContext, intent)
    }
}
