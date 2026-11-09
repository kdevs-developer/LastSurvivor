package com.kdevs.lastsurvivor.game.engine

import kotlin.math.max
import kotlin.math.min

/** Top-left corner of the visible area in world coordinates. */
class Camera {
    var x: Float = 0f
    var y: Float = 0f

    fun follow(targetX: Float, targetY: Float, viewW: Float, viewH: Float, worldW: Float, worldH: Float) {
        x = (targetX - viewW / 2f).coerceIn(0f, max(0f, worldW - viewW))
        y = (targetY - viewH / 2f).coerceIn(0f, max(0f, worldH - viewH))
    }
}
