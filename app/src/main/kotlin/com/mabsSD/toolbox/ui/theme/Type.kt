package com.mabsSD.toolbox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mabsSD.toolbox.R

/**
 * Plus Jakarta Sans, bundled in res/font rather than pulled from Google Fonts.
 *
 * The downloadable-fonts provider needs Play services and a network round trip,
 * which this app cannot make. Bundling costs ~645 KB and keeps the type identical
 * offline, on first launch, and on a de-Googled device.
 */
val JakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

/**
 * Two registers, deliberately far apart.
 *
 * Display type is huge, ExtraBold and tracked tight to -2sp; labels are small,
 * uppercase and tracked wide to +1.5sp. The gap between them is what carries the
 * hierarchy, so mid-weight filler sizes are kept to a minimum.
 */
val ToolboxTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 54.sp,
        letterSpacing = (-2.4).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.8).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        letterSpacing = (-1.2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 30.sp,
        letterSpacing = (-1).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.6).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    // Uppercase micro-labels. Wide tracking is what stops small caps reading as
    // cramped; these are always paired with .uppercase() at the call site.
    labelLarge = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = JakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.6.sp,
    ),
)
