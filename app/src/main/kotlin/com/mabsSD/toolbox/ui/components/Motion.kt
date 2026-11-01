package com.mabsSD.toolbox.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

/**
 * Motion in the iOS manner: springs instead of fixed-duration curves, a
 * slight squish under the finger, and pushed screens that slide over the
 * previous one while it drifts back. Compose follows the system "Remove
 * animations" setting, so all of this switches off for people who ask.
 */
object Motion {
    /** Interactive feedback: quick, with a small overshoot on release. */
    val press = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)

    /**
     * Page slides: a fixed 320 ms ease-out. A critically damped spring was
     * tried first, but its long, slow tail kept a strip of the previous screen
     * visible at the edge for a noticeable moment, wide on landscape tablets.
     * A tween always finishes on time.
     */
    val page = tween<IntOffset>(durationMillis = 320, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))

    /** Things appearing or moving into place, e.g. the dock indicator. */
    val settle = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
}

/**
 * Scales an element down while it's pressed and springs it back on release.
 * Pass the same [source] given to the Card or Button so it tracks that press:
 * `Modifier.then(rememberPressScale(source))`.
 */
@Composable
fun rememberPressScale(source: InteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, Motion.press, label = "pressScale")
    return Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** A light tick, for selection changes such as switching tabs. */
fun View.tick() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

// Screen transitions -------------------------------------------------------

/** Pushed screen slides in from the trailing edge, over the current one. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.pushEnter(): EnterTransition =
    slideInHorizontally(Motion.page) { it }

/**
 * The screen underneath drifts a third of the way back. No fade: fading the
 * whole outgoing screen renders it to an offscreen layer every frame, which
 * cost visible frames on mid-range tablets for little visual gain.
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.pushExit(): ExitTransition =
    slideOutHorizontally(Motion.page) { -it / 3 }

fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    slideInHorizontally(Motion.page) { -it / 3 }

fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    slideOutHorizontally(Motion.page) { it }

/** Switching between tabs: a quick cross-fade, as a tab bar does. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnter(): EnterTransition = fadeIn(tween(180))
fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExit(): ExitTransition = fadeOut(tween(120))
