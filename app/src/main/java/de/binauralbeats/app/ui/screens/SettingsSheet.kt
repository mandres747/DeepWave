package de.binauralbeats.app.ui.screens

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import de.binauralbeats.app.R
import de.binauralbeats.app.ui.theme.LocalBinauralColors
import de.binauralbeats.app.ui.theme.ThemeMode

@Composable
fun SettingsSheet(
    currentTheme: ThemeMode,
    currentLanguage: String,
    storeUrl: String?,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (String) -> Unit,
    onRateApp: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalBinauralColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceDark.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings_header),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentPrimary,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, stringResource(R.string.close), tint = colors.onSurface)
                }
            }

            HorizontalDivider(color = colors.overlay.copy(0.06f))
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.settings_language),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceMuted
            )
            Spacer(Modifier.height(8.dp))

            SettingsOptionRow(
                options = listOf(
                    "" to stringResource(R.string.lang_system),
                    "de" to stringResource(R.string.lang_de),
                    "en" to stringResource(R.string.lang_en)
                ),
                selected = currentLanguage,
                onSelect = onLanguageChange
            )

            Spacer(Modifier.height(20.dp))

            Text(
                stringResource(R.string.settings_theme),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceMuted
            )
            Spacer(Modifier.height(8.dp))

            SettingsOptionRow(
                options = listOf(
                    ThemeMode.SYSTEM.name to stringResource(R.string.theme_system),
                    ThemeMode.DARK.name to stringResource(R.string.theme_dark),
                    ThemeMode.LIGHT.name to stringResource(R.string.theme_light)
                ),
                selected = currentTheme.name,
                onSelect = { onThemeChange(ThemeMode.valueOf(it)) }
            )

            // Only builds with a store listing can be rated - the FOSS flavour
            // ships under a different application id and is not on Play.
            if (storeUrl != null) {
                Spacer(Modifier.height(20.dp))

                Text(
                    stringResource(R.string.settings_feedback),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceMuted
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    onClick = onRateApp,
                    color = colors.overlay.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.StarRate,
                            contentDescription = null,
                            tint = colors.accentPrimary
                        )
                        Column {
                            Text(
                                stringResource(R.string.rate_app),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                            Text(
                                stringResource(R.string.rate_app_desc),
                                fontSize = 11.sp,
                                color = colors.onSurfaceMuted
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                versionLabel(),
                fontSize = 11.sp,
                color = colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * "Version 1.3.1 (8)", read from the installed package at runtime -
 * BuildConfig is not enabled in this project. Debug builds say so, because
 * they install next to the Play version under the same name.
 */
@Composable
private fun versionLabel(): String {
    val context = LocalContext.current
    return remember(context) {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = PackageInfoCompat.getLongVersionCode(info)
        val debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        context.getString(R.string.settings_version, info.versionName, code) + if (debug) " · Debug" else ""
    }
}

@Composable
private fun SettingsOptionRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = LocalBinauralColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = key == selected
            Surface(
                onClick = { onSelect(key) },
                color = if (isSelected) colors.accentPrimary.copy(alpha = 0.15f)
                else colors.overlay.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = colors.accentPrimary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) colors.accentPrimary else colors.onSurfaceMuted
                    )
                }
            }
        }
    }
}
