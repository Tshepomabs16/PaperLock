package com.mabsSD.toolbox.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mabsSD.toolbox.BuildConfig
import com.mabsSD.toolbox.ui.components.BorderedBlock
import com.mabsSD.toolbox.ui.components.SectionLabel
import com.mabsSD.toolbox.ui.theme.ThemeMode
import com.mabsSD.toolbox.ui.toolboxContainer
import kotlinx.coroutines.launch

private const val PRIVACY_SUMMARY = "Toolbox has no INTERNET permission — it has no way to send " +
    "your files anywhere. Scanning, splitting, merging, compressing, and text recognition all " +
    "run on this device using models bundled inside the app. There is no account and no cloud " +
    "service to send data to even if the app wanted to.\n\n" +
    "Toolbox keeps a local, on-device history of files you've processed so you can find them " +
    "again from the Files tab. That history never leaves your device.\n\n" +
    "The camera permission is used only for the Scan feature; photos are processed on-device " +
    "and are never transmitted anywhere."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container = remember { context.toolboxContainer() }
    val scope = rememberCoroutineScope()
    val themeMode by container.preferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = inner.calculateTopPadding(),
                bottom = inner.calculateBottomPadding() + 24.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 24.dp, bottom = 20.dp),
                )
            }

            item {
                SectionLabel("Appearance")
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { scope.launch { container.preferences.setThemeMode(mode) } },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) {
                            Text(mode.label())
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                SectionLabel("Privacy")
                Spacer(Modifier.height(8.dp))
                BorderedBlock {
                    Text(PRIVACY_SUMMARY, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                SectionLabel("About")
                Spacer(Modifier.height(8.dp))
                BorderedBlock {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Toolbox", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Set in Plus Jakarta Sans (SIL Open Font License).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
