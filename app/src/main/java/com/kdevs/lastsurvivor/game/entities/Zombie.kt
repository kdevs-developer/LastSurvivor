package com.kdevs.lastsurvivor.game.entities

import com.kdevs.lastsurvivor.game.engine.GameConfig
import com.kdevs.lastsurvivor.game.engine.MapLayout
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
    // Boss tuning lives in GameConfig (BOSS_*); the charge is the Brute's special.

    var type: ZombieType = ZombieType.WALKER
        private set

    var health: Float = 0f
    var maxHealth: Float = 0f
    var speed: Float = 0f
    var attackDamage: Float = 0f

    /** Damage of the swing that just landed: normal, or boosted while a boss is charging. */
    var swingDamage: Float = 0f
        private set

    var state: ZombieState = ZombieState.IDLE
        private set

    // ---- animation / timing state ----
    var animTime: Float = 0f        // advances while walking; drives the step frame and sway
    var attackTimer: Float = 0f     // time until the next swing may land
    var attackAnim: Float = 0f      // >0 while the swing animation plays
    var hitFlash: Float = 0f        // Phase 5 draws a white flash while >0
    var deathTimer: Float = 0f      // counts down while the death animation plays

    // ---- Phase 9: effects state ----
    /** Seconds since spawning; the renderer fades and grows the zombie in over the first moments. */
    var age: Float = 0f
        private set
    /** Damage taken since the last floating number was shown (GameWorld shows and clears it). */
    var pendingDamage: Float = 0f
    /** While >0 the zombie may not show another damage number, so rapid hits are summed. */
    var numberCooldown: Float = 0f

    /** >0 while stunned (the Tank's shockwave): the zombie neither walks nor attacks. */
    var stunTimer: Float = 0f
    /** One-shot cues for the sound and shake system; GameWorld reads them and sets them back to false. */
    var windupJustStarted: Boolean = false
    var chargeJustStarted: Boolean = false
    var enrageJustStarted: Boolean = false

    // ---- boss state (unused by normal zombies) ----
    /** >0 while a boss telegraphs a charge: it stands still and the renderer draws a warning. */
    var windup: Float = 0f
        private set
    /** >0 while a boss is mid-charge. */
    var charging: Float = 0f
        private set
    var enraged: Boolean = false
        private set
    /** Set by a Brood Mother when it wants minions; GameWorld consumes it. */
    var summonReady: Boolean = false
    /** Direction of the current or last charge, so the renderer can draw the warning line. */
    var chargeDirX: Float = 1f
        private set
    var chargeDirY: Float = 0f
        private set
    private var abilityTimer: Float = 0f
    private var chargeHit: Boolean = false

    // ---- pathing: which way round a blocking prop this zombie has chosen (-1, 0 = none, +1) ----
    private var steerSide: Int = 0
    private var steerMemory: Float = 0f

    val isBoss: Boolean get() = type.isBoss

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
        swingDamage = attackDamage
        windup = 0f
        charging = 0f
        enraged = false
        summonReady = false
        chargeHit = false
        abilityTimer = type.abilityInterval
        state = ZombieState.IDLE
        animTime = (x * 0.013f) % 1f      // de-synchronise walk cycles between zombies
        attackTimer = FIRST_SWING_DELAY
        attackAnim = 0f
        hitFlash = 0f
        deathTimer = 0f
        age = 0f
        pendingDamage = 0f
        stunTimer = 0f
        numberCooldown = 0f
        steerSide = 0
        steerMemory = 0f
        windupJustStarted = false
        chargeJustStarted = false
        enrageJustStarted = false
        rotation = 0f
        active = true
    }

    /** Returns true if this hit killed the zombie. */
    fun takeDamage(amount: Float): Boolean {
        if (!alive) return false
        pendingDamage += if (amount < health) amount else health // overkill is not shown
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
        age += dt
        if (numberCooldown > 0f) numberCooldown -= dt
        if (stunTimer > 0f) stunTimer -= dt
        if (attackTimer > 0f) attackTimer -= dt
        if (attackAnim > 0f) attackAnim -= dt
        if (hitFlash > 0f) hitFlash -= dt
        if (state == ZombieState.WALK) animTime += dt
    }

    /**
     * Chase AI: face the player, walk at them (going round any prop in the way when a [map] is given), and
     * swing when in reach.
     * Returns true on the tick a swing lands (Phase 6 applies the damage to the player).
     */
    fun chase(dt: Float, player: Player, map: MapLayout? = null): Boolean {
        if (state == ZombieState.DEAD) return false
        if (stunTimer > 0f) {
            state = ZombieState.IDLE
            return false
        }

        val dx = player.x - x
        val dy = player.y - y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > 0.001f) rotation = atan2(dy, dx)

        var swung = false
        var moved = false
        var busy = false // a boss ability (windup or charge) is controlling this tick
        val reach = radius + player.radius + ATTACK_REACH
        swingDamage = attackDamage

        if (type.isBoss) {
            updateBossAbility(dt)
            if (windup > 0f) {
                busy = true
                windup -= dt
                if (windup <= 0f) startCharge(dx, dy, dist)
            } else if (charging > 0f) {
                busy = true
                charging -= dt
                x += chargeDirX * speed * GameConfig.BOSS_CHARGE_SPEED_MULT * dt
                y += chargeDirY * speed * GameConfig.BOSS_CHARGE_SPEED_MULT * dt
                rotation = atan2(chargeDirY, chargeDirX)
                moved = true
                if (!chargeHit && dist <= reach) {
                    chargeHit = true
                    swung = true
                    attackAnim = ATTACK_ANIM_SECONDS
                    swingDamage = attackDamage * GameConfig.BOSS_CHARGE_DAMAGE_MULT
                }
            }
        }

        if (busy) {
            // ability in control: no normal walking or swinging this tick
        } else if (dist <= reach) {
            if (attackTimer <= 0f) {
                attackAnim = ATTACK_ANIM_SECONDS
                attackTimer = type.attackCooldown
                swung = true
            }
        } else {
            val inv = 1f / dist
            var mx = dx * inv
            var my = dy * inv
            if (map != null) {
                val side = map.steer(x, y, radius, mx, my, minOf(GameConfig.ZOMBIE_STEER_LOOKAHEAD + radius, dist), steerSide)
                if (side != 0) {
                    steerSide = side
                    steerMemory = GameConfig.ZOMBIE_STEER_MEMORY
                    mx = map.steerX
                    my = map.steerY
                    rotation = atan2(my, mx) // look where it is walking while going round
                } else if (steerMemory > 0f) {
                    steerMemory -= dt
                    if (steerMemory <= 0f) steerSide = 0
                }
            }
            x += mx * speed * dt
            y += my * speed * dt
            moved = true
        }

        state = when {
            attackAnim > 0f -> ZombieState.ATTACK
            moved -> ZombieState.WALK
            else -> ZombieState.IDLE
        }
        return swung
    }

    /** Boss only: enrage at low health, count down to the next special ability and trigger it. */
    private fun updateBossAbility(dt: Float) {
        if (!enraged && health <= maxHealth * GameConfig.BOSS_ENRAGE_FRACTION) {
            enraged = true
            enrageJustStarted = true
            speed *= GameConfig.BOSS_ENRAGE_SPEED_MULT
        }
        if (windup > 0f || charging > 0f) return
        abilityTimer -= dt
        if (abilityTimer > 0f) return
        abilityTimer = type.abilityInterval * (if (enraged) GameConfig.BOSS_ENRAGE_ABILITY_MULT else 1f)
        when (type) {
            ZombieType.BRUTE -> { windup = GameConfig.BOSS_CHARGE_WINDUP; windupJustStarted = true }
            ZombieType.MOTHER -> summonReady = true
            else -> Unit
        }
    }

    /** Locks the charge direction at the moment the warning ends, so the player can dodge sideways. */
    private fun startCharge(dx: Float, dy: Float, dist: Float) {
        val inv = if (dist > 0.001f) 1f / dist else 0f
        chargeDirX = if (dist > 0.001f) dx * inv else 1f
        chargeDirY = dy * inv
        charging = GameConfig.BOSS_CHARGE_SECONDS
        chargeJustStarted = true
        chargeHit = false
    }
}
