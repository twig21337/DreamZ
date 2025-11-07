package com.twig.dreamzversion3.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

val MidnightGradient = Brush.linearGradient(
    colors = listOf(Midnight900, Twilight, Midnight700),
    start = Offset.Zero,
    end = Offset(600f, 900f)
)

val AuroraGradient = Brush.linearGradient(
    colors = listOf(DeepPlum, AuroraViolet, AuroraTeal, AuroraRose.copy(alpha = 0.6f)),
    start = Offset.Zero,
    end = Offset(800f, 1200f)
)
