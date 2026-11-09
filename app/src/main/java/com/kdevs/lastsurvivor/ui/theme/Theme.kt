package com.kdevs.lastsurvivor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = BloodRed,
    onPrimary = Bone,
    secondary = ToxicGreen,
    tertiary = CoinGold,
    background = Night,
    onBackground = Bone,
    surface = NightSurface,
    onSurface = Bone,
    surfaceVariant = NightSurface,
    onSurfaceVariant = Bone
)

@Composable
fun LastSurvivorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content
    )
}
