package de.binauralbeats.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PrimaryDark = Color(0xFF1E3C72)
val PrimaryMid = Color(0xFF2A5298)
val AccentPrimary = Color(0xFFA8E6CF)
val AccentSecondary = Color(0xFF7FCDCD)
val SurfaceDark = Color(0xFF0F1F3D)
val SurfaceVariant = Color(0xFF1A3561)
val OnSurfaceMuted = Color(0xB3FFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    secondary = AccentSecondary,
    tertiary = PrimaryMid,
    background = SurfaceDark,
    surface = PrimaryDark,
    surfaceVariant = SurfaceVariant,
    onPrimary = PrimaryDark,
    onSecondary = PrimaryDark,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = OnSurfaceMuted
)

@Composable
fun BinauralBeatsTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceDark.toArgb()
            window.navigationBarColor = SurfaceDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
