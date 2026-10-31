package com.jarvis.fileserver.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = CyanNeon,
    secondary = EmeraldLive,
    onSecondary = Slate950,
    background = Slate950,
    onBackground = Slate200,
    surface = Slate900,
    onSurface = Slate200,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    error = RoseError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = EmeraldLive,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = RoseError,
    onError = Color.White
)

@Composable
fun JarvisFileServerTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
