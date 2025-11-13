package com.twig.dreamzversion3.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

private val LightColorScheme = lightColorScheme(
    primary = AuroraViolet,
    secondary = AuroraTeal,
    tertiary = AuroraRose,
    background = LightSurface,
    surface = androidx.compose.ui.graphics.Color.White,
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
    colorCombo: ColorCombo,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val backgroundColor = colorCombo.backgroundColor(darkTheme)
    val foregroundColor = colorCombo.foregroundColor(darkTheme)
    val scheme = baseScheme.copy(
        background = backgroundColor,
        surface = backgroundColor,
        surfaceVariant = backgroundColor,
        onBackground = foregroundColor,
        onSurface = foregroundColor,
        onSurfaceVariant = foregroundColor.copy(alpha = 0.72f)
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
