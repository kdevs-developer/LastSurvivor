package com.kdevs.lastsurvivor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.kdevs.lastsurvivor.R

/** Material 3 colour scheme built from res/values/colors.xml (see also res/values/themes.xml). */
@Composable
fun LastSurvivorTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = colorResource(R.color.blood_red),
        onPrimary = colorResource(R.color.bone),
        secondary = colorResource(R.color.toxic_green),
        tertiary = colorResource(R.color.coin_gold),
        background = colorResource(R.color.night),
        onBackground = colorResource(R.color.bone),
        surface = colorResource(R.color.night_surface),
        onSurface = colorResource(R.color.bone),
        surfaceVariant = colorResource(R.color.night_surface),
        onSurfaceVariant = colorResource(R.color.bone)
    )
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
