package com.kdevs.lastsurvivor.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.sqrt

private const val TRAVEL_FRACTION = 0.6f // how far the knob can move, as a fraction of the radius
private const val DEADZONE = 0.12f

/**
 * Fixed-position virtual joystick. Reports (x, y) in -1..1 (screen space, +Y down) and whether
 * it is currently touched. The knob position lives in Compose state that is read only in the
 * draw phase, so moving the stick redraws this canvas without recomposing anything.
 */
@Composable
fun VirtualJoystick(
    diameter: Dp,
    accent: Color,
    modifier: Modifier = Modifier,
    onChange: (x: Float, y: Float, active: Boolean) -> Unit
) {
    val currentOnChange by rememberUpdatedState(onChange)
    var knob by remember { mutableStateOf(Offset.Zero) }   // normalized, length <= 1
    var active by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .size(diameter)
            .pointerInput(Unit) {
                val radius = this.size.width / 2f
                val travel = radius * TRAVEL_FRACTION

                fun apply(pos: Offset) {
                    var dx = (pos.x - radius) / travel
                    var dy = (pos.y - radius) / travel
                    val len = sqrt(dx * dx + dy * dy)
                    if (len > 1f) { dx /= len; dy /= len }
                    knob = Offset(dx, dy)
                    if (min(len, 1f) < DEADZONE) currentOnChange(0f, 0f, true)
                    else currentOnChange(dx, dy, true)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    active = true
                    try {
                        apply(down.position)
                        down.consume()
                        trackUntilRelease(down.id) { apply(it) }
                    } finally {
                        active = false
                        knob = Offset.Zero
                        currentOnChange(0f, 0f, false)
                    }
                }
            }
    ) {
        val r = this.size.minDimension / 2f
        val c = center
        drawCircle(Color.Black.copy(alpha = 0.28f), radius = r, center = c)
        drawCircle(
            accent.copy(alpha = if (active) 0.9f else 0.5f),
            radius = r, center = c, style = Stroke(3.dp.toPx())
        )
        drawCircle(
            accent.copy(alpha = 0.2f),
            radius = r * TRAVEL_FRACTION, center = c, style = Stroke(1.5.dp.toPx())
        )
        val k = c + Offset(knob.x, knob.y) * (r * TRAVEL_FRACTION)
        drawCircle(accent.copy(alpha = if (active) 0.85f else 0.45f), radius = r * 0.38f, center = k)
    }
}
