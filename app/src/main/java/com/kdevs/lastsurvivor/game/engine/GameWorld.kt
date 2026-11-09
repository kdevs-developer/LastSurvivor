package com.kdevs.lastsurvivor.game.engine

import com.kdevs.lastsurvivor.game.entities.Bullet
import com.kdevs.lastsurvivor.game.entities.ImpactEffect
import com.kdevs.lastsurvivor.game.entities.Player
import com.kdevs.lastsurvivor.game.entities.Zombie
import com.kdevs.lastsurvivor.game.entities.ZombieType
import com.kdevs.lastsurvivor.game.weapons.WeaponState
import com.kdevs.lastsurvivor.game.weapons.WeaponStats
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * All simulation state. Knows nothing about Compose, Canvas or Android views.
 * update() is called only from the game thread with a fixed timestep.
 */
class GameWorld {

    companion object {
        const val WIDTH = 3200f
        const val HEIGHT = 3200f
        private const val FLASH_SECONDS = 0.6f
        private const val DEBUG_BLAST_RADIUS = 260f
        private const val DEBUG_BLAST_DAMAGE = 60f
    }

    val input = InputState()
    val camera = Camera()
    val player = Player(WIDTH / 2f, HEIGHT / 2f)

    /** Active zombies, including ones playing their death animation. Pooled instances. */
    val zombies = ArrayList<Zombie>(64)
    private val zombiePool = ObjectPool({ Zombie() }, initialSize = 32)
    private val waves = WaveManager(this)

    /** Bullets in flight. Pooled instances. */
    val bullets = ArrayList<Bullet>(64)
    private val bulletPool = ObjectPool({ Bullet() }, initialSize = 48)

    /** Hit puffs currently playing. Pooled instances. */
    val impacts = ArrayList<ImpactEffect>(32)
    private val impactPool = ObjectPool({ ImpactEffect() }, initialSize = 24)

    /** The equipped weapon: ammo, reload and fire cooldown. */
    val weapon = WeaponState()
    private val pendingWeapon = AtomicReference<WeaponStats?>(null)
    private val restartRequested = AtomicBoolean(false)

    /** What the Compose HUD shows. Written by the game thread, collected by the UI. */
    val hud = MutableStateFlow(HudState())

    /** Zombies that are not dead; recomputed every tick. */
    var aliveZombies: Int = 0
        private set

    /** Session stats. Phase 6 turns kills into score and coins. */
    var shotsFired: Int = 0
        private set
    var shotsHit: Int = 0
        private set
    var kills: Int = 0
        private set

    /** Coins earned this run (kills and wave-clear bonuses). Saved when the run ends. */
    var coins: Int = 0
        private set

    @Volatile var gameOver: Boolean = false
        private set

    @Volatile var viewportWidth: Float = 1f
    @Volatile var viewportHeight: Float = 1f

    /** Asks the game thread to start a fresh run on its next tick. Safe to call from any thread. */
    fun requestRestart() = restartRequested.set(true)

    fun addCoins(amount: Int) { coins += amount }

    /** Equips a weapon (with a full magazine) on the next tick. Safe to call from any thread. */
    fun requestEquip(stats: WeaponStats) = pendingWeapon.set(stats)

    private fun reset() {
        var i = 0
        while (i < zombies.size) { zombies[i].active = false; zombiePool.release(zombies[i]); i++ }
        zombies.clear()
        i = 0
        while (i < bullets.size) { bulletPool.release(bullets[i]); i++ }
        bullets.clear()
        i = 0
        while (i < impacts.size) { impactPool.release(impacts[i]); i++ }
        impacts.clear()

        player.reset(WIDTH / 2f, HEIGHT / 2f)
        waves.reset()
        aliveZombies = 0
        weapon.refill()
        shotsFired = 0
        shotsHit = 0
        kills = 0
        coins = 0
        gameOver = false
    }

    fun update(dt: Float) {
        pendingWeapon.getAndSet(null)?.let { weapon.equip(it) }
        if (restartRequested.getAndSet(false)) reset()
        if (gameOver) {
            publishHud()
            return
        }

        applyInput()
        player.update(dt)
        player.x = player.x.coerceIn(player.radius, WIDTH - player.radius)
        player.y = player.y.coerceIn(player.radius, HEIGHT - player.radius)

        updateShooting(dt)

        waves.update(dt)
        updateZombies(dt)
        separateZombies()
        keepZombiesOutOfPlayer()

        // Bullets move after zombies so they test against this tick's final zombie positions.
        updateBullets(dt)
        updateImpacts(dt)
        removeFinishedZombies()

        if (!player.alive) {
            gameOver = true
            player.firing = false
        }

        camera.follow(player.x, player.y, viewportWidth, viewportHeight, WIDTH, HEIGHT)
        publishHud()
    }

    fun spawnZombie(type: ZombieType, x: Float, y: Float, healthScale: Float = 1f, speedScale: Float = 1f): Zombie {
        val z = zombiePool.obtain()
        z.reset(type, x, y, healthScale, speedScale)
        zombies.add(z)
        return z
    }

    /** Fires one bullet whenever fire is held and the cooldown has run out. */
    private fun updateShooting(dt: Float) {
        val w = weapon
        w.update(dt)
        val p = player
        if (!p.firing) return
        if (!w.canFire) {
            if (w.ammo <= 0) w.startReload() // empty and still holding fire: reload
            return
        }
        val s = w.stats
        w.consumeShot()
        p.muzzleFlash = GameConfig.MUZZLE_FLASH_SECONDS
        shotsFired += s.pellets

        // Start at the player's centre: the sprite is drawn on top, so the bullet appears to
        // leave the barrel, and no zombie touching the player can be skipped.
        val spread = Math.toRadians(s.spreadDegrees.toDouble()).toFloat()
        var i = 0
        while (i < s.pellets) {
            val angle = p.rotation + if (spread > 0f) (Random.nextFloat() - 0.5f) * spread else 0f
            val b = bulletPool.obtain()
            b.fire(p.x, p.y, angle, s.bulletSpeed, s.damage, s.range, s.pierce)
            bullets.add(b)
            i++
        }
    }

    private fun updateBullets(dt: Float) {
        var i = 0
        while (i < bullets.size) {
            val b = bullets[i]
            b.update(dt)
            if (b.active) {
                if (b.x < 0f || b.x > WIDTH || b.y < 0f || b.y > HEIGHT) {
                    spawnImpact(b.x.coerceIn(0f, WIDTH), b.y.coerceIn(0f, HEIGHT), onZombie = false)
                    b.active = false
                } else {
                    hitFirstZombie(b)
                }
            }
            if (b.active) {
                i++
            } else {
                val last = bullets.size - 1
                bullets[i] = bullets[last]
                bullets.removeAt(last)
                bulletPool.release(b)
            }
        }
    }

    /** Applies the bullet to the first not-yet-hit zombie its path touched this tick. */
    private fun hitFirstZombie(b: Bullet) {
        var best: Zombie? = null
        var bestT = 2f
        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            i++
            if (!z.alive || b.hasHit(z)) continue
            val t = Collision.segmentHitsCircle(b.prevX, b.prevY, b.x, b.y, z.x, z.y, z.radius + b.radius)
            if (t >= 0f && t < bestT) {
                bestT = t
                best = z
            }
        }
        val z = best ?: return

        val hitX = b.prevX + (b.x - b.prevX) * bestT
        val hitY = b.prevY + (b.y - b.prevY) * bestT
        shotsHit++
        if (z.takeDamage(b.damage)) registerKill(z)
        z.x += b.dirX * GameConfig.HIT_KNOCKBACK
        z.y += b.dirY * GameConfig.HIT_KNOCKBACK
        spawnImpact(hitX, hitY, onZombie = true)
        b.registerHit(z) // the bullet ends here unless it can pierce
    }

    private fun spawnImpact(x: Float, y: Float, onZombie: Boolean) {
        val e = impactPool.obtain()
        e.start(x, y, onZombie)
        impacts.add(e)
    }

    private fun updateImpacts(dt: Float) {
        var i = 0
        while (i < impacts.size) {
            val e = impacts[i]
            e.update(dt)
            if (e.active) {
                i++
            } else {
                val last = impacts.size - 1
                impacts[i] = impacts[last]
                impacts.removeAt(last)
                impactPool.release(e)
            }
        }
    }

    private fun applyInput() {
        val p = player
        p.moveX = input.moveX
        p.moveY = input.moveY
        p.aimX = input.aimX
        p.aimY = input.aimY
        p.aiming = input.aimActive
        p.sprinting = input.sprintHeld
        p.firing = input.fireHeld ||
            (input.aimActive && input.aimStrength >= GameConfig.AIM_AUTOFIRE_THRESHOLD)

        if (input.consumeReload()) weapon.startReload()
        if (input.consumeGrenade()) {
            p.grenadeFlash = FLASH_SECONDS
            debugBlast() // TEMPORARY so you can test zombie death; real grenades arrive in Phase 5/8
        }
    }

    /** A zombie's swing landed: damage the player, and poison them if it is a Toxic. */
    private fun hitPlayer(z: Zombie) {
        if (player.takeDamage(z.attackDamage) && z.type.poisonDps > 0f) {
            player.poison(z.type.poisonDps, z.type.poisonSeconds)
        }
    }

    private fun registerKill(z: Zombie) {
        kills++
        coins += z.type.coinReward
    }

    private fun updateZombies(dt: Float) {
        var alive = 0
        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            z.update(dt)
            if (z.alive) {
                alive++
                if (z.chase(dt, player)) hitPlayer(z)
                z.x = z.x.coerceIn(z.radius, WIDTH - z.radius)
                z.y = z.y.coerceIn(z.radius, HEIGHT - z.radius)
            }
            i++
        }
        aliveZombies = alive
    }

    /** Pushes overlapping zombies apart so a crowd spreads around the player instead of stacking. */
    private fun separateZombies() {
        val n = zombies.size
        var i = 0
        while (i < n) {
            val a = zombies[i]
            if (a.alive) {
                var j = i + 1
                while (j < n) {
                    val b = zombies[j]
                    if (b.alive) {
                        val dx = b.x - a.x
                        val dy = b.y - a.y
                        val minDist = a.radius + b.radius
                        val d2 = dx * dx + dy * dy
                        if (d2 < minDist * minDist) {
                            if (d2 > 0.0001f) {
                                val d = sqrt(d2)
                                val push = (minDist - d) * 0.5f
                                val nx = dx / d
                                val ny = dy / d
                                a.x -= nx * push; a.y -= ny * push
                                b.x += nx * push; b.y += ny * push
                            } else {
                                a.x -= 1f // exactly stacked: nudge apart
                            }
                        }
                    }
                    j++
                }
            }
            i++
        }
    }

    /** Zombies stand at the player's edge instead of walking through them. */
    private fun keepZombiesOutOfPlayer() {
        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            if (z.alive) {
                val dx = z.x - player.x
                val dy = z.y - player.y
                val minDist = z.radius + player.radius
                val d2 = dx * dx + dy * dy
                if (d2 < minDist * minDist && d2 > 0.0001f) {
                    val d = sqrt(d2)
                    z.x = player.x + dx / d * minDist
                    z.y = player.y + dy / d * minDist
                }
            }
            i++
        }
    }

    /** Swap-remove finished zombies and return them to the pool. */
    private fun removeFinishedZombies() {
        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            if (z.finished) {
                val last = zombies.size - 1
                zombies[i] = zombies[last]
                zombies.removeAt(last)
                z.active = false
                zombiePool.release(z)
            } else {
                i++
            }
        }
    }

    /** Publishes a new HudState only when something visible changed. */
    private fun publishHud() {
        val cur = hud.value
        val health = ceil(player.health).toInt()
        val maxHealth = ceil(player.maxHealth).toInt()
        val countdown = if (waves.phase == WavePhase.COUNTDOWN) ceil(waves.countdown).toInt().coerceAtLeast(0) else 0
        val left = waves.zombiesLeft
        val reloadPercent = if (weapon.reloading) (weapon.reloadProgress * 20f).toInt() * 5 else 0
        if (cur.health == health && cur.maxHealth == maxHealth && cur.poisoned == player.poisoned &&
            cur.wave == waves.wave && cur.phase == waves.phase && cur.countdownSeconds == countdown &&
            cur.clearedBonus == waves.clearedBonus && cur.zombiesLeft == left &&
            cur.kills == kills && cur.coins == coins && cur.gameOver == gameOver &&
            cur.weapon == weapon.stats.type && cur.ammo == weapon.ammo && cur.magazine == weapon.stats.magazine &&
            cur.reloading == weapon.reloading && cur.reloadPercent == reloadPercent
        ) return
        hud.value = HudState(
            health = health, maxHealth = maxHealth, poisoned = player.poisoned,
            wave = waves.wave, phase = waves.phase, countdownSeconds = countdown,
            clearedBonus = waves.clearedBonus, zombiesLeft = left,
            kills = kills, coins = coins, gameOver = gameOver,
            weapon = weapon.stats.type, ammo = weapon.ammo, magazine = weapon.stats.magazine,
            reloading = weapon.reloading, reloadPercent = reloadPercent
        )
    }

    private fun debugBlast() {
        val r2 = DEBUG_BLAST_RADIUS * DEBUG_BLAST_RADIUS
        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            val dx = z.x - player.x
            val dy = z.y - player.y
            if (z.alive && dx * dx + dy * dy <= r2 && z.takeDamage(DEBUG_BLAST_DAMAGE)) registerKill(z)
            i++
        }
    }
}
