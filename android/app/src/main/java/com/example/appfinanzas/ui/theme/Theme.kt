package com.example.appfinanzas.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    secondary = Violet500,
    tertiary = CyberCyan,
    background = OledBackground,
    surface = Slate900,
    surfaceVariant = Slate800,
    onBackground = Color.White,
    onSurface = Slate200,
    onSurfaceVariant = Slate400,
    error = RedAlert
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald500,
    secondary = Violet500,
    tertiary = CyberCyan,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Slate900,
    onSurface = Slate900,
    onSurfaceVariant = Slate600,
    error = RedAlert
)

@Composable
fun FinanzasProTheme(
    darkTheme: Boolean = true, // Force Dark OLED theme by default for FinTech aesthetic
    dynamicColor: Boolean = false, // Disable dynamic wallpaper overlay to preserve custom glassmorphism OLED
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
