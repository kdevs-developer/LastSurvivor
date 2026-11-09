package com.kdevs.lastsurvivor.ui.game.controls

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId

/**
 * Follows one specific pointer until it lifts. Because each control tracks its own pointer id,
 * the joysticks and buttons work simultaneously (true multi-touch).
 */
internal suspend fun AwaitPointerEventScope.trackUntilRelease(
    pointerId: PointerId,
    onMove: (Offset) -> Unit
) {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointerId } ?: return
        if (!change.pressed) return
        onMove(change.position)
        change.consume()
    }
}
