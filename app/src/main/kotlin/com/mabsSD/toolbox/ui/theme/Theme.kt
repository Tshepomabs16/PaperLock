package com.mabsSD.toolbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = OnInk,
    primaryContainer = AmberContainer,
    onPrimaryContainer = OnAmberContainer,

    // secondaryContainer paints selected chips, segmented buttons and the
    // bottom-nav indicator: amber, like the icon, not a separate accent.
    secondary = Teal,
    onSecondary = PaperSurface,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,

    tertiary = Amber,
    onTertiary = OnAmber,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmber,

    background = PaperBackground,
    onBackground = TextPrimary,
    surface = PaperSurface,
    onSurface = TextPrimary,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    // The surfaceContainer family is what NavigationBar, menus and sheets
    // paint with. Left unset, Material falls back to its own purple-tinted
    // baseline, which is where the lavender bottom bar came from.
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = PaperSurface,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurfaceVariant,
    surfaceContainerHighest = PaperSurfaceVariant,
    surfaceBright = PaperSurface,
    surfaceDim = PaperSurfaceVariant,

    error = Danger,
    onError = PaperSurface,
    errorContainer = DangerContainer,
    onErrorContainer = Danger,

    outline = Border,
    outlineVariant = Border,
)

private val DarkColors = darkColorScheme(
    primary = AmberLight,
    onPrimary = OnAmberLight,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = OnAmberContainerDark,

    secondary = TealLight,
    onSecondary = OnAmberLight,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = OnAmberContainerDark,

    tertiary = AmberLight,
    onTertiary = OnAmberLight,
    tertiaryContainer = InkSurfaceVariant,
    onTertiaryContainer = AmberLight,

    background = InkBackground,
    onBackground = TextPrimaryDark,
    surface = InkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = InkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = InkBackground,
    surfaceContainerLow = InkSurface,
    surfaceContainer = InkSurface,
    surfaceContainerHigh = InkSurfaceVariant,
    surfaceContainerHighest = InkSurfaceVariant,
    surfaceBright = InkSurfaceVariant,
    surfaceDim = InkBackground,

    error = DangerDark,
    onError = OnAmberLight,
    errorContainer = InkSurfaceVariant,
    onErrorContainer = DangerDark,

    outline = BorderDark,
    outlineVariant = BorderDark,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Dynamic colour stays off. A privacy-focused tool should look the same on
 * every device — the palette above is part of the trust claim, not something
 * that should shift with the user's wallpaper.
 */
@Composable
fun ToolboxTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ToolboxTypography,
        shapes = ToolboxShapes,
        content = content
    )
}
