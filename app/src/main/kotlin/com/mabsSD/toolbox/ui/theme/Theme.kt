package com.mabsSD.toolbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Signal,
    onPrimary = Surface,
    primaryContainer = SignalWash,
    onPrimaryContainer = Ink,

    secondary = Slate,
    onSecondary = Surface,
    secondaryContainer = Paper,
    onSecondaryContainer = Ink,

    tertiary = InkSoft,
    onTertiary = Surface,

    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = SlateMuted,

    error = Danger,
    onError = Surface,
    errorContainer = DangerWash,
    onErrorContainer = Danger,

    outline = Line,
    outlineVariant = Line,
)

private val DarkColors = darkColorScheme(
    primary = SignalDark,
    onPrimary = InkDark,
    primaryContainer = SignalWashDark,
    onPrimaryContainer = ChalkDark,

    secondary = SlateDark,
    onSecondary = InkDark,
    secondaryContainer = SurfaceDarkRaised,
    onSecondaryContainer = ChalkDark,

    tertiary = ChalkDark,
    onTertiary = InkDark,

    background = InkDark,
    onBackground = ChalkDark,
    surface = SurfaceDark,
    onSurface = ChalkDark,
    surfaceVariant = SurfaceDarkRaised,
    onSurfaceVariant = SlateDark,

    error = DangerDark,
    onError = InkDark,
    errorContainer = DangerWashDark,
    onErrorContainer = DangerDark,

    outline = LineDark,
    outlineVariant = LineDark,
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
        content = content
    )
}
