package com.mabsSD.toolbox.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Toolbox palette: ink neutrals with a single signal blue.
 *
 * The blue is an accent, not a background. It marks the one primary action on a
 * screen and nothing else, so "this is the thing to tap" never needs explaining.
 */

// Neutrals
val Ink = Color(0xFF0F172A)
val InkSoft = Color(0xFF1E293B)
val Slate = Color(0xFF334155)
val SlateMuted = Color(0xFF64748B)
val Line = Color(0xFFE2E8F0)
val Paper = Color(0xFFF8FAFC)
val Surface = Color(0xFFFFFFFF)

// Accent
val Signal = Color(0xFF2563EB)
val SignalPressed = Color(0xFF1D4ED8)
val SignalWash = Color(0xFFEFF6FF)

// Status
val Danger = Color(0xFFDC2626)
val DangerWash = Color(0xFFFEF2F2)

// Dark-mode neutrals.
// Lifted off pure black: a true #000 surface makes elevation impossible to read
// and haloes hard against white text on OLED.
val InkDark = Color(0xFF0B1120)
val SurfaceDark = Color(0xFF151C2C)
val SurfaceDarkRaised = Color(0xFF1E2739)
val LineDark = Color(0xFF2A3550)
val ChalkDark = Color(0xFFE8EDF5)
val SlateDark = Color(0xFF94A3B8)

// Desaturated for dark surfaces; the light-mode blue vibrates against dark ink.
val SignalDark = Color(0xFF60A5FA)
val SignalWashDark = Color(0xFF16243D)
val DangerDark = Color(0xFFF87171)
val DangerWashDark = Color(0xFF2C1618)
