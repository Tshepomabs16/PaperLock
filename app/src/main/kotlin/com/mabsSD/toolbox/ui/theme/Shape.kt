package com.mabsSD.toolbox.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Square corners throughout.
 *
 * Rounded cards read as friendly and generic; hard edges are what make an
 * oversized-type layout look deliberate rather than unfinished. The only
 * concession is 2dp on the smallest chips, which stops them looking like a
 * rendering error at that size.
 */
val ToolboxShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

/** Border weight for the bordered-block language used across every surface. */
val BorderWidth = 2.dp
val BorderWidthThin = 1.dp
