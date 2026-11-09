package com.kdevs.lastsurvivor.game.entities

import com.kdevs.lastsurvivor.game.engine.GameConfig
import kotlin.math.cos
import kotlin.math.sin

/**
 * One projectile. Pooled: call [fire] to (re)activate, never allocate in the loop.
 * [prevX]/[prevY] hold the position before the latest move so collision can test the whole
 * path of the tick (a swept segment), which means fast bullets can never tunnel through a zombie.
 */
class Bullet : Entity(0f, 0f, GameConfig.BULLET_RADIUS) {

    var dirX: Float = 1f
        private set
    var dirY: Float = 0f
        private set
    var prevX: Float = 0f
        private set
    var prevY: Float = 0f
        private set
    var damage: Float = 0f
        private set

    /** Distance flown so far; the renderer uses it to size the tracer tail. */
    var travelled: Float = 0f
        private set

    private var speed: Float = 0f
    private var maxRange: Float = 0f

    fun fire(x: Float, y: Float, angle: Float, speed: Float, damage: Float, range: Float) {
        this.x = x
        this.y = y
        prevX = x
        prevY = y
        rotation = angle
        dirX = cos(angle)
        dirY = sin(angle)
        this.speed = speed
        this.damage = damage
        maxRange = range
        travelled = 0f
        radius = GameConfig.BULLET_RADIUS
        active = true
    }

    override fun update(dt: Float) {
        prevX = x
        prevY = y
        val step = speed * dt
        x += dirX * step
        y += dirY * step
        travelled += step
        if (travelled >= maxRange) active = false
    }
}
