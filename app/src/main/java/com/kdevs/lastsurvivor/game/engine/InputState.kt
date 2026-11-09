package com.kdevs.lastsurvivor.game.engine

/**
 * Written by the UI thread, read by the game thread.
 * Plain volatile floats are enough because each field is independent.
 */
class InputState {
    @Volatile var moveX: Float = 0f
    @Volatile var moveY: Float = 0f
}
