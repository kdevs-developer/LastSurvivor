package com.kdevs.lastsurvivor.game.entities

import com.kdevs.lastsurvivor.game.engine.GameConfig
import kotlin.math.cos
import kotlin.math.sin

/**
 * One projectile. Pooled: call [fire] to (re)activate, never allocate in the loop.
 * [prevX]/[prevY] hold the position before the latest move so collision can test the whole
 * path of the tick (a swept segment), which means fast bullets can never tunnel through a zombie.
 * A piercing bullet remembers the zombies it already hit so it cannot hit one twice.
 */
class Bullet : Entity(0f, 0f, GameConfig.BULLET_RADIUS) {

    companion object {
        const val MAX_PIERCE = 4
    }

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

    /** True for a turret's bullets: they do not count towards the player's shots and accuracy. */
    var fromTurret: Boolean = false

    /** Distance flown so far; the renderer uses it to size the tracer tail. */
    var travelled: Float = 0f
        private set

    private var speed: Float = 0f
    private var maxRange: Float = 0f
    private var hitsLeft: Int = 1
    private val hitZombies = arrayOfNulls<Zombie>(MAX_PIERCE)
    private var hitCount = 0

    fun fire(x: Float, y: Float, angle: Float, speed: Float, damage: Float, range: Float, pierce: Int) {
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
        hitsLeft = pierce.coerceIn(1, MAX_PIERCE)
        hitCount = 0
        radius = GameConfig.BULLET_RADIUS
        fromTurret = false
        active = true
    }

    fun hasHit(z: Zombie): Boolean {
        var i = 0
        while (i < hitCount) {
            if (hitZombies[i] === z) return true
            i++
        }
        return false
    }

    /** Records a hit; the bullet is used up once it has hit as many zombies as it can pierce. */
    fun registerHit(z: Zombie) {
        if (hitCount < MAX_PIERCE) hitZombies[hitCount++] = z
        hitsLeft--
        if (hitsLeft <= 0) active = false
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
