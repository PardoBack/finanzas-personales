package com.example.finanzaspersonales.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun FinanzasPersonalesTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    ThemeManager.init(context)

    val currentMode = ThemeManager.currentTheme
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (currentMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }

    val appColors = if (isDark) DarkAppColors else LightAppColors

    val colorScheme = if (isDark) {
        darkColorScheme(
            background = appColors.background,
            surface = appColors.cardBackground,
            surfaceVariant = appColors.cardElevated,
            primary = appColors.cyan,
            secondary = appColors.emerald,
            tertiary = appColors.amber,
            error = appColors.rose,
            onBackground = appColors.textPrimary,
            onSurface = appColors.textPrimary,
            onSurfaceVariant = appColors.textSecondary
        )
    } else {
        lightColorScheme(
            background = appColors.background,
            surface = appColors.cardBackground,
            surfaceVariant = appColors.cardElevated,
            primary = appColors.cyan,
            secondary = appColors.emerald,
            tertiary = appColors.amber,
            error = appColors.rose,
            onBackground = appColors.textPrimary,
            onSurface = appColors.textPrimary,
            onSurfaceVariant = appColors.textSecondary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.background.toArgb()
            window.navigationBarColor = appColors.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
