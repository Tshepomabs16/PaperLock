package com.mabsSD.toolbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Signal,
    onPrimary = Paper,
    primaryContainer = Signal,
    onPrimaryContainer = Paper,

    secondary = Ink,
    onSecondary = Paper,
    secondaryContainer = Bone,
    onSecondaryContainer = Ink,

    tertiary = Ink,
    onTertiary = Paper,

    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Bone,
    onSurfaceVariant = Muted,

    error = Danger,
    onError = Paper,
    errorContainer = Paper,
    onErrorContainer = Danger,

    outline = Ink,
    outlineVariant = Faint,
)

private val DarkColors = darkColorScheme(
    primary = SignalDark,
    onPrimary = InkDark,
    primaryContainer = SignalDark,
    onPrimaryContainer = InkDark,

    secondary = Chalk,
    onSecondary = InkDark,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = Chalk,

    tertiary = Chalk,
    onTertiary = InkDark,

    background = InkDark,
    onBackground = Chalk,
    surface = InkDark,
    onSurface = Chalk,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = MutedDark,

    error = DangerDark,
    onError = InkDark,
    errorContainer = InkDark,
    onErrorContainer = DangerDark,

    outline = Chalk,
    outlineVariant = FaintDark,
)

/**
 * Dynamic colour is off by default, deliberately.
 *
 * On Android 12+ it replaces the palette above with tones derived from the user's
 * wallpaper, which is why the app previously rendered as flat grey on a Samsung
 * with a monochrome wallpaper. A privacy tool should look the same on every
 * device: the identity is part of the trust claim, not a per-phone accident.
 */
@Composable
fun ToolboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ToolboxTypography,
        shapes = ToolboxShapes,
        content = content
    )
}
