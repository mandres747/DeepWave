package de.binauralbeats.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class BinauralColors(
    val primaryDark: Color,
    val primaryMid: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val surfaceDark: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val onAccent: Color,
    val overlay: Color,
    val isDark: Boolean
)

val LocalBinauralColors = staticCompositionLocalOf {
    BinauralColors(
        primaryDark = Color(0xFF1E3C72),
        primaryMid = Color(0xFF2A5298),
        accentPrimary = Color(0xFFA8E6CF),
        accentSecondary = Color(0xFF7FCDCD),
        surfaceDark = Color(0xFF0F1F3D),
        surfaceVariant = Color(0xFF1A3561),
        onSurface = Color.White,
        onSurfaceMuted = Color(0xB3FFFFFF),
        onAccent = Color(0xFF1E3C72),
        overlay = Color.White,
        isDark = true
    )
}

private val DarkBinauralColors = BinauralColors(
    primaryDark = Color(0xFF1E3C72),
    primaryMid = Color(0xFF2A5298),
    accentPrimary = Color(0xFFA8E6CF),
    accentSecondary = Color(0xFF7FCDCD),
    surfaceDark = Color(0xFF0F1F3D),
    surfaceVariant = Color(0xFF1A3561),
    onSurface = Color.White,
    onSurfaceMuted = Color(0xB3FFFFFF),
    onAccent = Color(0xFF1E3C72),
    overlay = Color.White,
    isDark = true
)

private val LightBinauralColors = BinauralColors(
    primaryDark = Color(0xFFD8E2F3),
    primaryMid = Color(0xFFC5D3EC),
    accentPrimary = Color(0xFF2E8B6A),
    accentSecondary = Color(0xFF4FA8A0),
    surfaceDark = Color(0xFFF2F5FA),
    surfaceVariant = Color(0xFFDDE6F3),
    onSurface = Color(0xFF1A1A2E),
    onSurfaceMuted = Color(0x99000000),
    onAccent = Color.White,
    overlay = Color(0xFF1A1A2E),
    isDark = false
)

// Backward-compatible vals — still used in non-Composable contexts (notification, Canvas maps)
val PrimaryDark = DarkBinauralColors.primaryDark
val SurfaceDark = DarkBinauralColors.surfaceDark

private val DarkColorScheme = darkColorScheme(
    primary = DarkBinauralColors.accentPrimary,
    secondary = DarkBinauralColors.accentSecondary,
    tertiary = DarkBinauralColors.primaryMid,
    background = DarkBinauralColors.surfaceDark,
    surface = DarkBinauralColors.primaryDark,
    surfaceVariant = DarkBinauralColors.surfaceVariant,
    onPrimary = DarkBinauralColors.onAccent,
    onSecondary = DarkBinauralColors.onAccent,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = DarkBinauralColors.onSurfaceMuted
)

private val LightColorScheme = lightColorScheme(
    primary = LightBinauralColors.accentPrimary,
    secondary = LightBinauralColors.accentSecondary,
    tertiary = LightBinauralColors.primaryMid,
    background = LightBinauralColors.surfaceDark,
    surface = LightBinauralColors.primaryDark,
    surfaceVariant = LightBinauralColors.surfaceVariant,
    onPrimary = LightBinauralColors.onAccent,
    onSecondary = LightBinauralColors.onAccent,
    onBackground = LightBinauralColors.onSurface,
    onSurface = LightBinauralColors.onSurface,
    onSurfaceVariant = LightBinauralColors.onSurfaceMuted
)

@Composable
fun BinauralBeatsTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val binauralColors = if (isDark) DarkBinauralColors else LightBinauralColors
    val materialScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = binauralColors.surfaceDark.toArgb()
            window.navigationBarColor = binauralColors.surfaceDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalBinauralColors provides binauralColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            content = content
        )
    }
}
