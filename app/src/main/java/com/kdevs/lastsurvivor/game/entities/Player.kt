package com.kdevs.lastsurvivor.game.entities

import com.kdevs.lastsurvivor.game.engine.GameConfig
import kotlin.math.atan2
import kotlin.math.sqrt

class Player(x: Float, y: Float) : Entity(x, y, radius = 28f) {

    // ---- Input snapshot, written by GameWorld before update() ----
    var moveX: Float = 0f
    var moveY: Float = 0f
    var aimX: Float = 0f
    var aimY: Float = 0f
    var aiming: Boolean = false
    var firing: Boolean = false
    var sprinting: Boolean = false

    // ---- Stats ----
    var speed: Float = 260f // world units per second

    // ---- Derived state ----
    var moving: Boolean = false
        private set

    /** Placeholder feedback timers (seconds). Phases 5-7 replace these with real reload/grenade logic. */
    var reloadFlash: Float = 0f
    var muzzleFlash: Float = 0f // >0 right after a shot; the renderer draws the flash
    var grenadeFlash: Float = 0f

    override fun update(dt: Float) {
        val len = sqrt(moveX * moveX + moveY * moveY)
        moving = len > 0.01f
        if (moving) {
            val scale = if (len > 1f) 1f / len else 1f
            val mult = if (sprinting) GameConfig.SPRINT_MULTIPLIER else 1f
            x += moveX * scale * speed * mult * dt
            y += moveY * scale * speed * mult * dt
        }

        // Aim stick wins; otherwise face the movement direction; otherwise keep facing.
        if (aiming && (aimX * aimX + aimY * aimY) > 0.0001f) {
            rotation = atan2(aimY, aimX)
        } else if (moving) {
            rotation = atan2(moveY, moveX)
        }

        if (reloadFlash > 0f) reloadFlash -= dt
        if (muzzleFlash > 0f) muzzleFlash -= dt
        if (grenadeFlash > 0f) grenadeFlash -= dt
    }
}
