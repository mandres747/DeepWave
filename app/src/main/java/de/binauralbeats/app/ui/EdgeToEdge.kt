package de.binauralbeats.app.ui

import android.app.Activity
import androidx.core.view.WindowCompat

/**
 * Lets the content draw behind the system bars. The bar colours come from
 * Theme.BinauralBeats (values, values-v29, values-v30) and the icon contrast
 * from BinauralBeatsTheme, so this is all that is left to do at runtime.
 *
 * Replaces androidx.activity's enableEdgeToEdge(), whose calls to
 * Window.setStatusBarColor/setNavigationBarColor Play reports as deprecated
 * on every release even though they only run below Android 15.
 */
fun Activity.drawBehindSystemBars() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
}
