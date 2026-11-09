package com.kdevs.lastsurvivor.game.entities

import com.kdevs.lastsurvivor.game.engine.GameConfig
import kotlin.math.atan2
import kotlin.math.max
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

    // ---- Health (Phase 6) ----
    var maxHealth: Float = GameConfig.PLAYER_MAX_HEALTH
    var health: Float = maxHealth
    val alive: Boolean get() = health > 0f

    /** >0 while the red hit flash plays. */
    var hurtFlash: Float = 0f
        private set
    private var invulnerable: Float = 0f
    private var poisonTimer: Float = 0f
    private var poisonDps: Float = 0f
    val poisoned: Boolean get() = poisonTimer > 0f

    // ---- Derived state ----
    var moving: Boolean = false
        private set

    /** Feedback timers (seconds). The grenade flash is a placeholder until Phase 8. */
    var muzzleFlash: Float = 0f // >0 right after a shot; the renderer draws the flash
    var grenadeFlash: Float = 0f

    /** Returns true if the hit landed (false while invulnerable or already dead). */
    fun takeDamage(amount: Float): Boolean {
        if (!alive || invulnerable > 0f) return false
        health = max(0f, health - amount)
        invulnerable = GameConfig.PLAYER_INVULN_SECONDS
        hurtFlash = GameConfig.HURT_FLASH_SECONDS
        return true
    }

    /** Poison does not stack: the strongest damage rate and the longest duration win. */
    fun poison(dps: Float, seconds: Float) {
        poisonDps = max(poisonDps, dps)
        poisonTimer = max(poisonTimer, seconds)
    }

    fun reset(x: Float, y: Float) {
        this.x = x
        this.y = y
        maxHealth = GameConfig.PLAYER_MAX_HEALTH
        health = maxHealth
        hurtFlash = 0f
        invulnerable = 0f
        poisonTimer = 0f
        poisonDps = 0f
        rotation = 0f
        moving = false
        grenadeFlash = 0f
        muzzleFlash = 0f
    }

    override fun update(dt: Float) {
        if (invulnerable > 0f) invulnerable -= dt
        if (hurtFlash > 0f) hurtFlash -= dt
        if (poisonTimer > 0f) {
            poisonTimer -= dt
            health = max(0f, health - poisonDps * dt)
            if (poisonTimer <= 0f) poisonDps = 0f
        }

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

        if (muzzleFlash > 0f) muzzleFlash -= dt
        if (grenadeFlash > 0f) grenadeFlash -= dt
    }
}
