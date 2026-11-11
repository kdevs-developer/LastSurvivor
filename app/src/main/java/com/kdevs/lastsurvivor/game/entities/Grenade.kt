package com.kdevs.lastsurvivor.game.entities

import com.kdevs.lastsurvivor.game.engine.GameConfig
import kotlin.math.cos
import kotlin.math.sin

/**
 * A thrown grenade. It leaves the player's hand fast, slows to a stop over the fuse time and then
 * goes inactive; GameWorld sees that and explodes it. The throw distance is speed x fuse / 2, so
 * [launch] picks the speed that makes it land exactly [distance] away (auto-aimed throws).
 * Pooled like bullets.
 */
class Grenade : Entity(0f, 0f, GameConfig.GRENADE_RADIUS) {

    private var dirX = 1f
    private var dirY = 0f
    private var speed = GameConfig.GRENADE_THROW_SPEED

    var age: Float = 0f
        private set

    /** 0 when thrown, 1 at the moment of detonation (the renderer blinks faster near 1). */
    val fuseProgress: Float get() = (age / GameConfig.GRENADE_FUSE_SECONDS).coerceIn(0f, 1f)

    /** Starts the throw from ([x], [y]) in direction [angle]; it comes to rest [distance] further on. */
    fun launch(x: Float, y: Float, angle: Float, distance: Float = GameConfig.GRENADE_MAX_RANGE) {
        this.x = x
        this.y = y
        rotation = angle
        dirX = cos(angle)
        dirY = sin(angle)
        speed = 2f * distance / GameConfig.GRENADE_FUSE_SECONDS
        age = 0f
        active = true
    }

    override fun update(dt: Float) {
        age += dt
        val slow = (1f - age / GameConfig.GRENADE_FUSE_SECONDS).coerceAtLeast(0f)
        x += dirX * speed * slow * dt
        y += dirY * speed * slow * dt
        if (age >= GameConfig.GRENADE_FUSE_SECONDS) active = false
    }
}