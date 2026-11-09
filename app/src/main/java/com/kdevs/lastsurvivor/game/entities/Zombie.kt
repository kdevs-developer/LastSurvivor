package com.kdevs.lastsurvivor.game.entities

import kotlin.math.atan2
import kotlin.math.sqrt

enum class ZombieState { IDLE, WALK, ATTACK, DEAD }

/**
 * One zombie. Instances are pooled: call [reset] to (re)activate one, never allocate in the loop.
 * Health, speed, damage and animation state are all per-instance.
 */
class Zombie : Entity(0f, 0f, 26f) {

    companion object {
        const val DEATH_SECONDS = 0.7f
        private const val ATTACK_ANIM_SECONDS = 0.3f
        private const val FIRST_SWING_DELAY = 0.4f
        private const val ATTACK_REACH = 12f // extra distance beyond touching at which it can swing
    }

    var type: ZombieType = ZombieType.WALKER
        private set

    var health: Float = 0f
    var maxHealth: Float = 0f
    var speed: Float = 0f
    var attackDamage: Float = 0f

    var state: ZombieState = ZombieState.IDLE
        private set

    // ---- animation / timing state ----
    var animTime: Float = 0f        // advances while walking; drives the step frame and sway
    var attackTimer: Float = 0f     // time until the next swing may land
    var attackAnim: Float = 0f      // >0 while the swing animation plays
    var hitFlash: Float = 0f        // Phase 5 draws a white flash while >0
    var deathTimer: Float = 0f      // counts down while the death animation plays

    val alive: Boolean get() = state != ZombieState.DEAD
    val finished: Boolean get() = state == ZombieState.DEAD && deathTimer <= 0f

    fun reset(type: ZombieType, x: Float, y: Float, healthScale: Float = 1f, speedScale: Float = 1f) {
        this.type = type
        this.x = x
        this.y = y
        radius = type.radius
        maxHealth = type.maxHealth * healthScale
        health = maxHealth
        speed = type.speed * speedScale
        attackDamage = type.attackDamage
        state = ZombieState.IDLE
        animTime = (x * 0.013f) % 1f      // de-synchronise walk cycles between zombies
        attackTimer = FIRST_SWING_DELAY
        attackAnim = 0f
        hitFlash = 0f
        deathTimer = 0f
        rotation = 0f
        active = true
    }

    /** Returns true if this hit killed the zombie. */
    fun takeDamage(amount: Float): Boolean {
        if (!alive) return false
        health -= amount
        hitFlash = 0.1f
        if (health <= 0f) {
            health = 0f
            state = ZombieState.DEAD
            deathTimer = DEATH_SECONDS
            return true
        }
        return false
    }

    /** Timers only. Movement and attacks are in [chase]. */
    override fun update(dt: Float) {
        if (state == ZombieState.DEAD) {
            deathTimer -= dt
            return
        }
        if (attackTimer > 0f) attackTimer -= dt
        if (attackAnim > 0f) attackAnim -= dt
        if (hitFlash > 0f) hitFlash -= dt
        if (state == ZombieState.WALK) animTime += dt
    }

    /**
     * Chase AI: face the player, walk straight at them, and swing when in reach.
     * Returns true on the tick a swing lands (Phase 6 applies the damage to the player).
     */
    fun chase(dt: Float, player: Player): Boolean {
        if (state == ZombieState.DEAD) return false

        val dx = player.x - x
        val dy = player.y - y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > 0.001f) rotation = atan2(dy, dx)

        var swung = false
        var moved = false
        val reach = radius + player.radius + ATTACK_REACH

        if (dist <= reach) {
            if (attackTimer <= 0f) {
                attackAnim = ATTACK_ANIM_SECONDS
                attackTimer = type.attackCooldown
                swung = true
            }
        } else {
            val inv = 1f / dist
            x += dx * inv * speed * dt
            y += dy * inv * speed * dt
            moved = true
        }

        state = when {
            attackAnim > 0f -> ZombieState.ATTACK
            moved -> ZombieState.WALK
            else -> ZombieState.IDLE
        }
        return swung
    }
}
