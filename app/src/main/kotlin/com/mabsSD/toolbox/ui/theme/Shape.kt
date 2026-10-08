package com.mabsSD.toolbox.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Rounded corner scale, replacing the earlier stark-square Brutalist system.
 * Values match the design brief's radius tokens.
 */
val RadiusXs = 6.dp
val RadiusSm = 10.dp
val RadiusMd = 14.dp
val RadiusLg = 20.dp
val RadiusXl = 28.dp
val RadiusFull = 999.dp

val ToolboxShapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusXs),
    small = RoundedCornerShape(RadiusSm),
    medium = RoundedCornerShape(RadiusMd),
    large = RoundedCornerShape(RadiusLg),
    extraLarge = RoundedCornerShape(RadiusXl),
)

/**
 * Dark mode favours a visible hairline over elevation shadows, which barely
 * read against a near-black background. Light mode uses Card's own tonal
 * elevation instead of a border.
 */
val BorderWidthThin = 1.dp
