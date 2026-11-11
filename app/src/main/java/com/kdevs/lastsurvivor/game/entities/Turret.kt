package com.kdevs.lastsurvivor.game.entities

import com.kdevs.lastsurvivor.game.engine.GameConfig

/**
 * The Engineer's turret: stands where it was dropped, shoots the nearest zombie in range and
 * disappears when [life] runs out. Aiming and firing are done by GameWorld (it owns the bullet pool).
 */
class Turret : Entity(0f, 0f, GameConfig.TURRET_RADIUS) {

    /** Seconds until it disappears. */
    var life: Float = 0f
        private set

    /** Seconds since it was dropped; drives the drop-in animation. */
    var age: Float = 0f
        private set

    /** Seconds until the next shot. */
    var cooldown: Float = 0f

    /** True for the last seconds, when the renderer makes it blink. */
    val expiring: Boolean get() = life < 2f

    fun place(x: Float, y: Float) {
        this.x = x
        this.y = y
        life = GameConfig.TURRET_SECONDS
        age = 0f
        cooldown = 0.3f
        rotation = 0f
        active = true
    }

    override fun update(dt: Float) {
        age += dt
        life -= dt
        if (cooldown > 0f) cooldown -= dt
        if (life <= 0f) active = false
    }
}
