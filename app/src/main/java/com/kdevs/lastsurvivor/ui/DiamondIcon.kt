package com.kdevs.lastsurvivor.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A cut gem, the icon for diamonds. Drawn here so it does not depend on an icon set; tint it with `Icon(tint = ...)`. */
val DiamondIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Diamond",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 3f)
            lineTo(18f, 3f)
            lineTo(22f, 9f)
            lineTo(12f, 22f)
            lineTo(2f, 9f)
            close()
        }
    }.build()
}
