package com.jeet.dockermonitor.ui.theme

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

// Dark theme colors — deep dark with purple/teal accents
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C4DFF),           // purple
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4A148C),
    onPrimaryContainer = Color(0xFFEDE7F6),
    secondary = Color(0xFF00BCD4),         // teal
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF006064),
    onSecondaryContainer = Color(0xFFE0F7FA),
    tertiary = Color(0xFF4CAF50),          // green for running status
    onTertiary = Color(0xFF000000),
    background = Color(0xFF0D0D0D),        // near black
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF1A1A1A),           // dark card
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = Color(0xFFFF5252),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF333333),
)

// Light theme colors — clean white with purple/teal accents
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EA),           // deep purple
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFF0097A7),         // teal
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFF388E3C),          // green
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF666666),
    error = Color(0xFFD50000),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFFE0E0E0),
)

@Composable
fun DockerMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}