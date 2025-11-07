package com.twig.dreamzversion3.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AuroraTeal,
    secondary = AuroraViolet,
    tertiary = AuroraRose,
    background = Midnight900,
    surface = Midnight700,
    onPrimary = Midnight900,
    onSecondary = Starlight,
    onTertiary = Midnight900,
    onBackground = Starlight,
    onSurface = Starlight,
    surfaceVariant = Midnight700,
    onSurfaceVariant = Starlight.copy(alpha = 0.72f)
)

private val LightSurface = Color(0xFFFBF9FF)

private val LightColorScheme = lightColorScheme(
    primary = AuroraViolet,
    secondary = AuroraTeal,
    tertiary = AuroraRose,
    background = LightSurface,
    surface = Color.White,
    onPrimary = Starlight,
    onSecondary = Midnight900,
    onTertiary = Midnight900,
    onBackground = Midnight900,
    onSurface = Midnight900,
    surfaceVariant = LightSurface,
    onSurfaceVariant = Midnight700
)

@Composable
fun DreamZVersion3Theme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}