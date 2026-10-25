package com.laralbir.japantraveltools.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

val NipponDarkColorScheme = darkColorScheme(
    primary = AkabeniDark,
    onPrimary = OnAkabeniDark,
    primaryContainer = AkabeniContainerDark,
    onPrimaryContainer = OnAkabeniContainerDark,
    secondary = AiiroDark,
    onSecondary = OnAiiroDark,
    secondaryContainer = AiiroContainerDark,
    onSecondaryContainer = OnAiiroContainerDark,
    tertiary = MatchaDark,
    onTertiary = OnMatchaDark,
    tertiaryContainer = MatchaContainerDark,
    onTertiaryContainer = OnMatchaContainerDark,
    background = SumiBackgroundDark,
    onBackground = WashiTextDark,
    surface = SumiSurfaceDark,
    onSurface = WashiTextDark,
    surfaceVariant = SumiSurfaceVariantDark,
    onSurfaceVariant = WashiSubtextDark,
    outline = SumiOutlineDark
)

val NipponLightColorScheme = lightColorScheme(
    primary = AkabeniLight,
    onPrimary = OnAkabeniLight,
    primaryContainer = AkabeniContainerLight,
    onPrimaryContainer = OnAkabeniContainerLight,
    secondary = AiiroLight,
    onSecondary = OnAiiroLight,
    secondaryContainer = AiiroContainerLight,
    onSecondaryContainer = OnAiiroContainerLight,
    tertiary = MatchaLight,
    onTertiary = OnMatchaLight,
    tertiaryContainer = MatchaContainerLight,
    onTertiaryContainer = OnMatchaContainerLight,
    background = WashiBackgroundLight,
    onBackground = SumiTextLight,
    surface = WashiSurfaceLight,
    onSurface = SumiTextLight,
    surfaceVariant = WashiSurfaceVariantLight,
    onSurfaceVariant = SumiSubtextLight,
    outline = WashiOutlineLight
)

@Composable
fun JapanTravelToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NipponDarkColorScheme
        else -> NipponLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
