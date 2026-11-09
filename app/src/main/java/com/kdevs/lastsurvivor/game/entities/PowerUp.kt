package com.kdevs.lastsurvivor.game.entities

import com.kdevs.lastsurvivor.game.engine.GameConfig

/** A pickup lying on the ground. Pooled: call [spawn] to (re)activate, it expires on its own. */
class PowerUp : Entity(0f, 0f, GameConfig.POWERUP_RADIUS) {

    var type: PowerUpType = PowerUpType.MEDKIT
        private set

    /** Seconds since it dropped; drives the bobbing animation. */
    var age: Float = 0f
        private set

    /** Seconds until it disappears. */
    var life: Float = 0f
        private set

    /** True for the last few seconds, when the renderer makes it blink. */
    val expiring: Boolean get() = life < GameConfig.POWERUP_BLINK_SECONDS

    fun spawn(type: PowerUpType, x: Float, y: Float) {
        this.type = type
        this.x = x
        this.y = y
        age = 0f
        life = GameConfig.POWERUP_LIFETIME
        active = true
    }

    override fun update(dt: Float) {
        age += dt
        life -= dt
        if (life <= 0f) active = false
    }
}
