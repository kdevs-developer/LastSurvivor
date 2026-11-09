package com.kdevs.lastsurvivor.game.engine

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Written by the UI thread (Compose controls), read by the game thread.
 * Held inputs are volatile fields; one-shot taps (reload, grenade) are atomic flags that the
 * game thread consumes exactly once.
 */
class InputState {
    // Left stick: movement, each axis -1..1 (screen space, +Y is down)
    @Volatile var moveX: Float = 0f
    @Volatile var moveY: Float = 0f

    // Right stick: aim direction, each axis -1..1
    @Volatile var aimX: Float = 0f
    @Volatile var aimY: Float = 0f
    @Volatile var aimActive: Boolean = false
    @Volatile var aimStrength: Float = 0f

    // Held buttons
    @Volatile var fireHeld: Boolean = false
    @Volatile var sprintHeld: Boolean = false

    // One-shot buttons
    private val reloadRequested = AtomicBoolean(false)
    private val grenadeRequested = AtomicBoolean(false)

    fun requestReload() = reloadRequested.set(true)
    fun requestGrenade() = grenadeRequested.set(true)
    fun consumeReload(): Boolean = reloadRequested.getAndSet(false)
    fun consumeGrenade(): Boolean = grenadeRequested.getAndSet(false)

    /** Clears everything, e.g. on pause, so no input stays "stuck". */
    fun reset() {
        moveX = 0f; moveY = 0f
        aimX = 0f; aimY = 0f; aimActive = false; aimStrength = 0f
        fireHeld = false; sprintHeld = false
        reloadRequested.set(false); grenadeRequested.set(false)
    }
}
