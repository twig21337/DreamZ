package com.twig.dreamzversion3.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.twig.dreamzversion3.R

private fun solidBrush(color: Color): Brush = Brush.linearGradient(
    colors = listOf(color, color),
    start = Offset.Zero,
    end = Offset(600f, 900f)
)

enum class ColorCombo(
    @StringRes val displayNameRes: Int,
    val lightBackground: Color,
    val lightForeground: Color,
    val darkBackground: Color,
    val darkForeground: Color,
    private val lightBrush: Brush,
    private val darkBrush: Brush
) {
    AURORA(
        displayNameRes = R.string.color_combo_aurora,
        lightBackground = LightSurface,
        lightForeground = Midnight900,
        darkBackground = Midnight900,
        darkForeground = Starlight,
        lightBrush = AuroraGradient,
        darkBrush = MidnightGradient
    ),
    OCEAN_DUSK(
        displayNameRes = R.string.color_combo_ocean_dusk,
        lightBackground = OceanMist,
        lightForeground = DeepOcean,
        darkBackground = DeepOcean,
        darkForeground = OceanMist,
        lightBrush = solidBrush(OceanMist),
        darkBrush = solidBrush(DeepOcean)
    ),
    SUNSET_GLOW(
        displayNameRes = R.string.color_combo_sunset_glow,
        lightBackground = SunsetGlow,
        lightForeground = BurntSienna,
        darkBackground = BurntSienna,
        darkForeground = SunsetGlow,
        lightBrush = solidBrush(SunsetGlow),
        darkBrush = solidBrush(BurntSienna)
    ),
    FOREST_CANOPY(
        displayNameRes = R.string.color_combo_forest_canopy,
        lightBackground = ForestCanopy,
        lightForeground = DeepForest,
        darkBackground = DeepForest,
        darkForeground = ForestCanopy,
        lightBrush = solidBrush(ForestCanopy),
        darkBrush = solidBrush(DeepForest)
    ),
    DESERT_DAWN(
        displayNameRes = R.string.color_combo_desert_dawn,
        lightBackground = DesertDawn,
        lightForeground = DesertShadow,
        darkBackground = DesertShadow,
        darkForeground = DesertDawn,
        lightBrush = solidBrush(DesertDawn),
        darkBrush = solidBrush(DesertShadow)
    ),
    BLOSSOM_SKY(
        displayNameRes = R.string.color_combo_blossom_sky,
        lightBackground = BlossomPetal,
        lightForeground = PlumWine,
        darkBackground = PlumWine,
        darkForeground = BlossomPetal,
        lightBrush = solidBrush(BlossomPetal),
        darkBrush = solidBrush(PlumWine)
    ),
    SLATE_MORNING(
        displayNameRes = R.string.color_combo_slate_morning,
        lightBackground = SlateMorning,
        lightForeground = SlateMidnight,
        darkBackground = SlateMidnight,
        darkForeground = SlateMorning,
        lightBrush = solidBrush(SlateMorning),
        darkBrush = solidBrush(SlateMidnight)
    ),
    GOLDEN_HOUR(
        displayNameRes = R.string.color_combo_golden_hour,
        lightBackground = GoldenMist,
        lightForeground = AmberSun,
        darkBackground = AmberSun,
        darkForeground = GoldenMist,
        lightBrush = solidBrush(GoldenMist),
        darkBrush = solidBrush(AmberSun)
    ),
    STARRY_NIGHT(
        displayNameRes = R.string.color_combo_starry_night,
        lightBackground = Moonlight,
        lightForeground = StarlitSky,
        darkBackground = StarlitSky,
        darkForeground = Moonlight,
        lightBrush = solidBrush(Moonlight),
        darkBrush = solidBrush(StarlitSky)
    ),
    CYBER_CITY(
        displayNameRes = R.string.color_combo_cyber_city,
        lightBackground = NeonGlow,
        lightForeground = NeonPulse,
        darkBackground = NeonNight,
        darkForeground = NeonGlow,
        lightBrush = solidBrush(NeonGlow),
        darkBrush = solidBrush(NeonNight)
    );

    fun backgroundColor(darkTheme: Boolean): Color = if (darkTheme) darkBackground else lightBackground

    fun foregroundColor(darkTheme: Boolean): Color = if (darkTheme) darkForeground else lightForeground

    fun backgroundBrush(darkTheme: Boolean): Brush = if (darkTheme) darkBrush else lightBrush
}
