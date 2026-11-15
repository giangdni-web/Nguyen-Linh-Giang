package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = JudicialNavy,
    onPrimary = Color.White,
    primaryContainer = JudicialNavyContainer,
    onPrimaryContainer = OnJudicialNavyContainer,
    secondary = BurnishedGold,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = SlateTeal,
    onTertiary = Color.White,
    tertiaryContainer = SlateTealContainer,
    onTertiaryContainer = JudicialNavy,
    background = IvoryBackground,
    onBackground = InkPrimary,
    surface = IvorySurface,
    onSurface = InkPrimary,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = OutlineStone,
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = CrimsonAlertContainer,
    onErrorContainer = CrimsonAlert
)

private val DarkColorScheme = darkColorScheme(
    primary = BurnishedGoldBright,
    onPrimary = JudicialNavy,
    primaryContainer = JudicialNavyLight,
    onPrimaryContainer = Color.White,
    secondary = BurnishedGold,
    onSecondary = JudicialNavy,
    secondaryContainer = Color(0xFF423010),
    onSecondaryContainer = GoldContainer,
    tertiary = Color(0xFF7CB8C9),
    onTertiary = JudicialNavy,
    background = DarkNavyBackground,
    onBackground = DarkInkPrimary,
    surface = DarkNavySurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkNavySurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    outline = DarkOutline,
    error = Color(0xFFF28B82),
    onError = Color(0xFF601410)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MrGiangLawTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
