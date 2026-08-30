package com.mabsSD.toolbox.ui

import android.content.Context
import com.mabsSD.toolbox.AppContainer
import com.mabsSD.toolbox.ToolboxApplication

/**
 * Reach the app-wide service locator from a composable.
 *
 * A stopgap until Hilt: screens should take what they need as parameters, but
 * threading the container through every navigation callback is worse noise than
 * this one accessor.
 */
fun Context.toolboxContainer(): AppContainer =
    (applicationContext as ToolboxApplication).container
