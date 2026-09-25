package de.binauralbeats.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import de.binauralbeats.app.R
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, DARK, LIGHT }

/**
 * "Schwebung" (docs/GESTALTUNG.md, 25.09.2026): plum night and orchid instead of
 * navy and mint, which sat on Kopfkarte's navy and Nenne drei's green. The token
 * names predate that and describe roles, not hues: primaryDark/primaryMid/
 * surfaceVariant are the background gradient stops, surfaceDark the sheet ground.
 * accentPrimary also colours text, so the light theme uses the darker orchid step
 * (5.4 : 1 on the lightest gradient stop, white on it 7.4 : 1).
 */
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
    /** Hints that need doing something (exact alarms, full screen). Not an accent. */
    val warning: Color,
    val isDark: Boolean
)

/** Titles and the big frequency figures. One cut: Fraunces SOFT 100, opsz 72, weight 600. */
val TitleFont = FontFamily(Font(R.font.fraunces_soft_semibold, FontWeight.SemiBold))

private val DarkBinauralColors = BinauralColors(
    primaryDark = Color(0xFF2F182F),
    primaryMid = Color(0xFF3A1D3A),
    accentPrimary = Color(0xFFE07BDE),
    accentSecondary = Color(0xFFF1E9F4),
    surfaceDark = Color(0xFF1D0E1D),
    surfaceVariant = Color(0xFF2A1529),
    onSurface = Color(0xFFF1E9F4),
    onSurfaceMuted = Color(0xFFBFA9C2),
    onAccent = Color(0xFF1D0E1D),
    overlay = Color(0xFFF1E9F4),
    warning = Color(0xFFF2B35B),
    isDark = true
)

private val LightBinauralColors = BinauralColors(
    primaryDark = Color(0xFFF1E4EF),
    primaryMid = Color(0xFFEAD7E7),
    accentPrimary = Color(0xFF8A2E8A),
    accentSecondary = Color(0xFFB35AB0),
    surfaceDark = Color(0xFFF8F0F7),
    surfaceVariant = Color(0xFFF4E9F2),
    onSurface = Color(0xFF2A1530),
    onSurfaceMuted = Color(0xFF6E5670),
    onAccent = Color.White,
    overlay = Color(0xFF2A1530),
    warning = Color(0xFF9A5B00),
    isDark = false
)

val LocalBinauralColors = staticCompositionLocalOf { DarkBinauralColors }

private val BaseTypography = Typography()

/** Material's own titles (dialogs, sheets built from M3 parts) in the title face too. */
private val DeepWaveTypography = BaseTypography.copy(
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = TitleFont),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = TitleFont),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = TitleFont),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = TitleFont),
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
    onBackground = DarkBinauralColors.onSurface,
    onSurface = DarkBinauralColors.onSurface,
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
            // No statusBarColor/navigationBarColor here: the theme sets them
            // (see themes.xml), Android 15 ignores both, and Play reports the
            // calls as deprecated. Only the icon contrast still needs saying.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                // Below Android 10 the navigation bar keeps a dark scrim, so
                // its buttons stay light even in the light theme.
                isAppearanceLightNavigationBars =
                    !isDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            }
        }
    }

    CompositionLocalProvider(LocalBinauralColors provides binauralColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = DeepWaveTypography,
            content = content
        )
    }
}
