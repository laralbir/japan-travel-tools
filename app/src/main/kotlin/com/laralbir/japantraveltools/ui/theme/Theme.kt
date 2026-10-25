package com.laralbir.japantraveltools.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val NipponDarkColorScheme = darkColorScheme(
    primary = AiiroPrimaryDark,
    onPrimary = OnAiiroPrimaryDark,
    primaryContainer = AiiroContainerDark,
    onPrimaryContainer = OnAiiroContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = OnSageSecondaryDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = BambooTertiaryDark,
    onTertiary = OnBambooTertiaryDark,
    tertiaryContainer = BambooContainerDark,
    onTertiaryContainer = OnBambooContainerDark,
    background = SumiBackgroundDark,
    onBackground = WashiTextDark,
    surface = SumiSurfaceDark,
    onSurface = WashiTextDark,
    surfaceVariant = SumiSurfaceVariantDark,
    onSurfaceVariant = WashiSubtextDark,
    outline = SumiOutlineDark
)

val NipponLightColorScheme = lightColorScheme(
    primary = AiiroPrimaryLight,
    onPrimary = OnAiiroPrimaryLight,
    primaryContainer = AiiroContainerLight,
    onPrimaryContainer = OnAiiroContainerLight,
    secondary = SageSecondaryLight,
    onSecondary = OnSageSecondaryLight,
    secondaryContainer = SageContainerLight,
    onSecondaryContainer = OnSageContainerLight,
    tertiary = BambooTertiaryLight,
    onTertiary = OnBambooTertiaryLight,
    tertiaryContainer = BambooContainerLight,
    onTertiaryContainer = OnBambooContainerLight,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
