package com.mabsSD.toolbox.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * PaperLock palette, taken from the launcher icon: ink, paper and amber.
 *
 * Warm paper surfaces, ink for primary actions in light mode (amber in dark,
 * where ink would vanish), amber for highlights and the capture accent, teal
 * kept for success states. Per the design brief: never use pure #000000 or
 * #FFFFFF as a large surface — both themes below sit slightly off those extremes.
 */

// Light
val PaperBackground = Color(0xFFF7F5F2)
val PaperSurface = Color(0xFFFFFFFF)
val PaperSurfaceVariant = Color(0xFFF1EFEA)
val Ink = Color(0xFF16181F)
val OnInk = Color(0xFFFFFFFF)
val Teal = Color(0xFF0D9488)
val Amber = Color(0xFFF59E0B)
val OnAmber = Color(0xFF451A03)
val AmberContainer = Color(0xFFFEF3C7)
val OnAmberContainer = Color(0xFF78350F)
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)
val Border = Color(0xFFE5E7EB)
val Danger = Color(0xFFDC2626)
val DangerContainer = Color(0xFFFEE2E2)
val Success = Color(0xFF16A34A)

// Dark
val InkBackground = Color(0xFF0B0F14)
val InkSurface = Color(0xFF121820)
val InkSurfaceVariant = Color(0xFF1C2430)
val TealLight = Color(0xFF5EEAD4)
val AmberLight = Color(0xFFFBBF24)
val OnAmberLight = Color(0xFF1F1300)
val AmberContainerDark = Color(0xFF3D2C0A)
val OnAmberContainerDark = Color(0xFFFDE68A)
val TextPrimaryDark = Color(0xFFF9FAFB)
val TextSecondaryDark = Color(0xFF9CA3AF)
val BorderDark = Color(0xFF2A3441)
val DangerDark = Color(0xFFF87171)
val SuccessDark = Color(0xFF4ADE80)
