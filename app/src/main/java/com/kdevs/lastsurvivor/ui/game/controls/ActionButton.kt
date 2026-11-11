package com.kdevs.lastsurvivor.ui.game.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kdevs.lastsurvivor.R

/**
 * Round on-screen button. [onPressChanged] is called with true on touch-down and false on
 * release (or cancellation), which supports both hold buttons and tap buttons (reload, grenade
 * and the sprint toggle act on `true`). The finger may drift outside the button while held.
 * [active] lights the button up while a toggle it controls is switched on (sprint).
 */
@Composable
fun ActionButton(
    icon: ImageVector,
    description: String,
    diameter: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onPressChanged: (pressed: Boolean) -> Unit
) {
    val currentCallback by rememberUpdatedState(onPressChanged)
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(color.copy(alpha = if (pressed || active) 0.8f else 0.3f))
            .border(2.dp, color.copy(alpha = 0.9f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    currentCallback(true)
                    try {
                        trackUntilRelease(down.id) { }
                    } finally {
                        pressed = false
                        currentCallback(false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = colorResource(R.color.control_icon),
            modifier = Modifier.size(diameter * 0.5f)
        )
    }
}