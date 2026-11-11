package com.kdevs.lastsurvivor.game.entities

import com.kdevs.lastsurvivor.game.engine.GameConfig
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class Player(x: Float, y: Float) : Entity(x, y, radius = 28f) {

    companion object {
        /** Walking speed in world units per second, before the character's multiplier. */
        const val BASE_SPEED = 260f
    }

    // ---- Input snapshot, written by GameWorld before update() ----
    var moveX: Float = 0f
    var moveY: Float = 0f
    var aimX: Float = 0f
    var aimY: Float = 0f
    var aiming: Boolean = false
    var firing: Boolean = false
    var sprinting: Boolean = false

    // ---- Character (stats come from the chosen character) ----
    var character: GameCharacter = GameCharacter.SURVIVOR
        private set

    // ---- Stats ----
    var speed: Float = BASE_SPEED // world units per second

    // ---- Health (Phase 6) ----
    var maxHealth: Float = GameConfig.PLAYER_MAX_HEALTH * character.healthMult
    var health: Float = maxHealth
    val alive: Boolean get() = health > 0f

    /** >0 while the red hit flash plays. */
    var hurtFlash: Float = 0f
        private set
    private var invulnerable: Float = 0f
    private var poisonTimer: Float = 0f
    private var poisonDps: Float = 0f
    val poisoned: Boolean get() = poisonTimer > 0f

    // ---- Power-up buffs (Phase 8): seconds remaining ----
    var rapidFireTimer: Float = 0f
        private set
    var damageBoostTimer: Float = 0f
        private set
    var shieldTimer: Float = 0f
        private set
    val rapidFire: Boolean get() = rapidFireTimer > 0f
    val damageBoost: Boolean get() = damageBoostTimer > 0f
    val shielded: Boolean get() = shieldTimer > 0f

    // ---- Character abilities: seconds remaining ----
    /** Adrenaline: extra running speed. */
    var adrenalineTimer: Float = 0f
        private set
    /** Heal pulse: health restored over time. */
    var healTimer: Float = 0f
        private set
    private var healPerSecond: Float = 0f
    /** Dash: while >0 the player shoots forward in (dashDirX, dashDirY) and nothing can hurt them. */
    var dashTimer: Float = 0f
        private set
    private var dashDirX: Float = 1f
    private var dashDirY: Float = 0f
    val adrenaline: Boolean get() = adrenalineTimer > 0f
    val dashing: Boolean get() = dashTimer > 0f

    // ---- Derived state ----
    var moving: Boolean = false
        private set

    /** Feedback timers (seconds). */
    var muzzleFlash: Float = 0f // >0 right after a shot; the renderer draws the flash
    var grenadeFlash: Float = 0f

    /** Returns true if the hit landed (false while invulnerable or already dead). */
    fun takeDamage(amount: Float): Boolean {
        if (!alive || invulnerable > 0f || shielded || dashing) return false
        health = max(0f, health - amount * character.damageTakenMult)
        invulnerable = GameConfig.PLAYER_INVULN_SECONDS
        hurtFlash = GameConfig.HURT_FLASH_SECONDS
        return true
    }

    /** Restores health up to the maximum. */
    fun heal(amount: Float) {
        health = min(maxHealth, health + amount)
    }

    /** Removes poison. */
    fun cure() {
        poisonTimer = 0f
        poisonDps = 0f
    }

    /** Buffs do not stack: picking one up again refreshes it to the full duration. */
    fun grantRapidFire(seconds: Float) { rapidFireTimer = max(rapidFireTimer, seconds) }
    fun grantDamageBoost(seconds: Float) { damageBoostTimer = max(damageBoostTimer, seconds) }
    fun grantShield(seconds: Float) { shieldTimer = max(shieldTimer, seconds) }

    /** Applies the character's stats and starts with full health. Used at the start of a run. */
    fun applyCharacter(c: GameCharacter) {
        character = c
        maxHealth = GameConfig.PLAYER_MAX_HEALTH * c.healthMult
        health = maxHealth
        speed = BASE_SPEED * c.speedMult
    }

    fun startAdrenaline(seconds: Float) { adrenalineTimer = max(adrenalineTimer, seconds) }

    /** Restores [total] health evenly over [seconds] and cures poison. */
    fun startHealOverTime(total: Float, seconds: Float) {
        healTimer = seconds
        healPerSecond = total / seconds
        cure()
    }

    /** Shoots the player in the given direction (need not be normalised) for [seconds]. */
    fun startDash(dirX: Float, dirY: Float, seconds: Float) {
        val len = sqrt(dirX * dirX + dirY * dirY)
        dashDirX = if (len > 0.001f) dirX / len else 1f
        dashDirY = if (len > 0.001f) dirY / len else 0f
        dashTimer = seconds
    }

    /** Poison does not stack: the strongest damage rate and the longest duration win. */
    fun poison(dps: Float, seconds: Float) {
        poisonDps = max(poisonDps, dps)
        poisonTimer = max(poisonTimer, seconds)
    }

    fun reset(x: Float, y: Float) {
        this.x = x
        this.y = y
        maxHealth = GameConfig.PLAYER_MAX_HEALTH * character.healthMult
        health = maxHealth
        speed = BASE_SPEED * character.speedMult
        adrenalineTimer = 0f
        healTimer = 0f
        healPerSecond = 0f
        dashTimer = 0f
        hurtFlash = 0f
        invulnerable = 0f
        poisonTimer = 0f
        poisonDps = 0f
        rapidFireTimer = 0f
        damageBoostTimer = 0f
        shieldTimer = 0f
        rotation = 0f
        moving = false
        grenadeFlash = 0f
        muzzleFlash = 0f
    }

    override fun update(dt: Float) {
        if (invulnerable > 0f) invulnerable -= dt
        if (hurtFlash > 0f) hurtFlash -= dt
        if (rapidFireTimer > 0f) rapidFireTimer -= dt
        if (damageBoostTimer > 0f) damageBoostTimer -= dt
        if (shieldTimer > 0f) shieldTimer -= dt
        if (adrenalineTimer > 0f) adrenalineTimer -= dt
        if (healTimer > 0f) {
            healTimer -= dt
            heal(healPerSecond * dt)
        }
        if (alive && character.regenPerSecond > 0f && !poisoned) heal(character.regenPerSecond * dt)
        if (poisonTimer > 0f) {
            poisonTimer -= dt
            if (!shielded) health = max(0f, health - poisonDps * dt)
            if (poisonTimer <= 0f) poisonDps = 0f
        }

        val len = sqrt(moveX * moveX + moveY * moveY)
        moving = len > 0.01f
        if (dashTimer > 0f) {
            dashTimer -= dt
            x += dashDirX * GameConfig.DASH_SPEED * dt
            y += dashDirY * GameConfig.DASH_SPEED * dt
            moving = true
        } else if (moving) {
            val scale = if (len > 1f) 1f / len else 1f
            var mult = if (sprinting) GameConfig.SPRINT_MULTIPLIER else 1f
            if (adrenaline) mult *= GameConfig.ADRENALINE_SPEED_MULT
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
