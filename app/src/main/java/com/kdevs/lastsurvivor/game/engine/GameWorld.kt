package com.kdevs.lastsurvivor.game.engine

import com.kdevs.lastsurvivor.game.audio.GameAudio
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.effects.DecalField
import com.kdevs.lastsurvivor.game.effects.FloatingTexts
import com.kdevs.lastsurvivor.game.effects.ParticleSystem
import com.kdevs.lastsurvivor.game.entities.AbilityType
import com.kdevs.lastsurvivor.game.entities.Bullet
import com.kdevs.lastsurvivor.game.entities.Entity
import com.kdevs.lastsurvivor.game.entities.GameCharacter
import com.kdevs.lastsurvivor.game.entities.ExplosionEffect
import com.kdevs.lastsurvivor.game.entities.Grenade
import com.kdevs.lastsurvivor.game.entities.PassiveType
import com.kdevs.lastsurvivor.game.entities.ImpactEffect
import com.kdevs.lastsurvivor.game.entities.Player
import com.kdevs.lastsurvivor.game.entities.Turret
import com.kdevs.lastsurvivor.game.entities.PowerUp
import com.kdevs.lastsurvivor.game.entities.PowerUpType
import com.kdevs.lastsurvivor.game.entities.Zombie
import com.kdevs.lastsurvivor.game.entities.ZombieType
import com.kdevs.lastsurvivor.game.weapons.WeaponState
import com.kdevs.lastsurvivor.game.weapons.WeaponStats
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
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
        private const val GRENADE_SPAWN_OFFSET = 36f // where in front of the player a grenade appears
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

    /** Power-ups lying on the ground. Pooled instances. */
    val powerUps = ArrayList<PowerUp>(16)
    private val powerUpPool = ObjectPool({ PowerUp() }, initialSize = 8)

    /** Grenades in flight. Pooled instances. */
    val grenades = ArrayList<Grenade>(8)
    private val grenadePool = ObjectPool({ Grenade() }, initialSize = 4)

    /** Blast rings currently playing. Pooled instances. */
    val explosions = ArrayList<ExplosionEffect>(8)
    private val explosionPool = ObjectPool({ ExplosionEffect() }, initialSize = 4)

    /** Grenades the player is carrying. Refilled each run; Grenade pickups add more. */
    var grenadeCount: Int = GameConfig.GRENADE_START_COUNT
        private set

    /** The living boss, if there is one; recomputed every tick. */
    var boss: Zombie? = null
        private set

    /** The equipped weapon: ammo, reload and fire cooldown. */
    val weapon = WeaponState()
    private val pendingWeapon = AtomicReference<WeaponStats?>(null)
    private val restartRequested = AtomicBoolean(false)

    /** What is being played (campaign level or endless area). Changed only through [requestSetup]. */
    @Volatile var setup: RunSetup = RunSetup.DEFAULT
        private set
    private val pendingSetup = AtomicReference<RunSetup?>(null)

    /** Obstacles and hazards of the current map; rebuilt whenever the level or area changes. */
    val map = MapLayout().also { it.build(RunSetup.DEFAULT.area, RunSetup.DEFAULT.layoutSeed) }

    /** The map theme the renderer draws. */
    val area: GameArea get() = setup.area

    /** True when the last wave of a campaign level has been cleared. */
    val levelComplete: Boolean get() = waves.levelComplete

    /** Switches to another level or area and starts it from wave 1 on the next tick. Safe to call from any thread. */
    fun requestSetup(newSetup: RunSetup) = pendingSetup.set(newSetup)
    private val pendingContinue = AtomicReference<ContinueRequest?>(null)

    /** A request to start a run at a saved wave instead of wave 1. */
    private class ContinueRequest(val wave: Int, val health: Int, val grenades: Int)

    // ---- Phase 10: options, outfit and totals for saving ----
    /** Player options. Replaced as a whole (the object is immutable), so the game thread always sees a consistent copy. */
    @Volatile var settings: GameSettings = GameSettings()

    /**
     * The character being played: it sets the stats, the ability and the outfit the renderer draws.
     * Stats are applied at the start of the next tick. Safe to set from any thread.
     */
    @Volatile var character: GameCharacter = GameCharacter.SURVIVOR
        set(value) {
            field = value
            pendingCharacter.set(value)
        }
    private val pendingCharacter = AtomicReference<GameCharacter?>(null)

    /** The equipped weapon before the character's magazine and reload bonuses. */
    private var baseWeaponStats: WeaponStats? = null

    /** Turrets standing in the world (the Engineer's ability). At most one at a time. */
    val turrets = ArrayList<Turret>(2)

    /** Seconds until the character's ability can be used again; 0 = ready. */
    var abilityCooldown: Float = 0f
        private set

    /** Diamonds collected in this stretch of play. Saved with the run. */
    var diamonds: Int = 0
        private set

    fun addDiamonds(amount: Int) { diamonds += amount }

    /** Bosses killed in this stretch of play. */
    var bossKills: Int = 0
        private set

    /** Seconds spent alive in this stretch of play (not counting pauses or the game over screen). */
    var runSeconds: Float = 0f
        private set

    // ---- Phase 9: sound and visual effects ----
    /** Receives sound requests. Set by GameSurfaceView; null means silent (for tests). */
    @Volatile var audio: GameAudio? = null

    val particles = ParticleSystem(GameConfig.PARTICLE_CAPACITY)
    val decals = DecalField(GameConfig.DECAL_CAPACITY)
    val floatingTexts = FloatingTexts(GameConfig.FLOATING_TEXT_CAPACITY)

    /** Simulated seconds since the world was created; drives looping animations in the renderer. */
    var time: Float = 0f
        private set

    /** Camera shake offset in pixels for this tick; the renderer adds it to the camera position. */
    var shakeX: Float = 0f
        private set
    var shakeY: Float = 0f
        private set
    private var trauma = 0f
    private var groanTimer = GameConfig.GROAN_MAX_INTERVAL
    private var heartbeatTimer = 0f
    private var wasReloading = false

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

    /**
     * Asks the game thread to start a run at [wave] (the Continue button). Health is restored to at
     * least [GameConfig.CHECKPOINT_MIN_HEALTH_FRACTION] of the maximum. Safe to call from any thread.
     */
    fun requestContinue(wave: Int, health: Int, grenades: Int) =
        pendingContinue.set(ContinueRequest(wave, health, grenades))

    /** Everything worth saving about this stretch of play. Read from the UI thread; plain ints, so a slightly stale value is harmless. */
    fun runTotals() = RunTotals(
        coins = coins, kills = kills, shotsFired = shotsFired, shotsHit = shotsHit,
        bossKills = bossKills, seconds = runSeconds.toInt(), diamonds = diamonds
    )

    /** Equips a weapon (with a full magazine) on the next tick. Safe to call from any thread. */
    fun requestEquip(stats: WeaponStats) = pendingWeapon.set(stats)

    /** Equips [base] with the character's magazine and reload bonuses applied. */
    private fun equipFor(base: WeaponStats) {
        baseWeaponStats = base
        val c = player.character
        val adjusted = if (c.magazineMult == 1f && c.reloadTimeMult == 1f) base else base.copy(
            magazine = (base.magazine * c.magazineMult).roundToInt().coerceAtLeast(1),
            reloadSeconds = base.reloadSeconds * c.reloadTimeMult
        )
        weapon.equip(adjusted)
        wasReloading = false
    }

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
        i = 0
        while (i < powerUps.size) { powerUpPool.release(powerUps[i]); i++ }
        powerUps.clear()
        i = 0
        while (i < grenades.size) { grenadePool.release(grenades[i]); i++ }
        grenades.clear()
        i = 0
        while (i < explosions.size) { explosionPool.release(explosions[i]); i++ }
        explosions.clear()
        grenadeCount = GameConfig.GRENADE_START_COUNT
        turrets.clear()
        abilityCooldown = 0f
        strikesLeft = 0
        strikeTimer = 0f
        boss = null
        particles.clear()
        decals.clear()
        floatingTexts.clear()
        trauma = 0f
        shakeX = 0f
        shakeY = 0f
        groanTimer = GameConfig.GROAN_MAX_INTERVAL
        heartbeatTimer = 0f
        wasReloading = false

        map.restore()
        player.reset(WIDTH / 2f, HEIGHT / 2f)
        waves.reset()
        aliveZombies = 0
        weapon.refill()
        shotsFired = 0
        shotsHit = 0
        kills = 0
        coins = 0
        bossKills = 0
        diamonds = 0
        runSeconds = 0f
        gameOver = false
    }

    private fun applyContinue(c: ContinueRequest) {
        reset()
        waves.restoreWave(c.wave)
        val minHealth = player.maxHealth * GameConfig.CHECKPOINT_MIN_HEALTH_FRACTION
        player.health = c.health.toFloat().coerceIn(minHealth, player.maxHealth)
        grenadeCount = c.grenades.coerceIn(0, GameConfig.GRENADE_MAX_COUNT)
    }

    fun update(dt: Float) {
        pendingWeapon.getAndSet(null)?.let { equipFor(it) }
        pendingCharacter.getAndSet(null)?.let {
            player.applyCharacter(it)
            baseWeaponStats?.let { base -> equipFor(base) } // the new character's magazine and reload bonuses
        }
        pendingSetup.getAndSet(null)?.let {
            setup = it
            waves.configure(it)
            map.build(it.area, it.layoutSeed)
            reset()
        }
        if (restartRequested.getAndSet(false)) reset()
        pendingContinue.getAndSet(null)?.let { applyContinue(it) }
        if (gameOver || waves.levelComplete) {
            updateEffects(dt) // blood, smoke and stains keep moving behind the game over screen
            publishHud()
            return
        }

        runSeconds += dt
        if (abilityCooldown > 0f) abilityCooldown = max(0f, abilityCooldown - dt)
        updateAirstrike(dt)
        applyInput()
        val startX = player.x
        val startY = player.y
        player.update(dt)
        applyTerrain(player, startX, startY)
        player.x = player.x.coerceIn(player.radius, WIDTH - player.radius)
        player.y = player.y.coerceIn(player.radius, HEIGHT - player.radius)
        updatePoisonPools()

        updateShooting(dt)
        updateTurrets(dt)
        syncReloadSounds()

        waves.update(dt)
        updateZombies(dt)
        separateZombies()
        keepZombiesOutOfPlayer()

        // Bullets move after zombies so they test against this tick's final zombie positions.
        updateBullets(dt)
        updateImpacts(dt)
        updateGrenades(dt)
        updateExplosions(dt)
        updatePowerUps(dt)
        flushDamageNumbers()
        removeFinishedZombies()

        if (!player.alive && !waves.levelComplete) {
            gameOver = true
            player.firing = false
            onPlayerDied()
        } else {
            updateAmbience(dt)
        }

        updateEffects(dt)
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
        w.consumeShot(if (p.rapidFire) GameConfig.RAPID_FIRE_COOLDOWN_SCALE else 1f)
        var damage = if (p.damageBoost) s.damage * GameConfig.DOUBLE_DAMAGE_MULT else s.damage
        damage *= p.character.damageMult                                   // Commando: Veteran
        if (p.deadeye) damage *= GameConfig.DEADEYE_DAMAGE_MULT            // Marksman: Deadeye
        if (p.character.passive == PassiveType.SHARPSHOOTER && Random.nextFloat() < GameConfig.CRIT_CHANCE) {
            damage *= GameConfig.CRIT_DAMAGE_MULT                          // Marksman: Sharpshooter crit
        }
        val pierce = s.pierce + if (p.deadeye) GameConfig.DEADEYE_EXTRA_PIERCE else 0
        p.muzzleFlash = GameConfig.MUZZLE_FLASH_SECONDS
        shotsFired += s.pellets
        onShotFired(s)

        // Start at the player's centre: the sprite is drawn on top, so the bullet appears to
        // leave the barrel, and no zombie touching the player can be skipped.
        val spread = Math.toRadians(s.spreadDegrees.toDouble()).toFloat()
        var i = 0
        while (i < s.pellets) {
            val angle = p.rotation + if (spread > 0f) (Random.nextFloat() - 0.5f) * spread else 0f
            val b = bulletPool.obtain()
            b.fire(p.x, p.y, angle, s.bulletSpeed, damage, s.range, pierce)
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
                    val hx = b.x.coerceIn(0f, WIDTH)
                    val hy = b.y.coerceIn(0f, HEIGHT)
                    spawnImpact(hx, hy, onZombie = false)
                    wallHitEffects(hx, hy, b.dirX, b.dirY)
                    b.active = false
                } else if (!hitObstacle(b)) {
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

    /** Stops the bullet at the first obstacle its path touched; a barrel takes the damage and may blow up. */
    private fun hitObstacle(b: Bullet): Boolean {
        var best: Obstacle? = null
        var bestT = 2f
        val list = map.obstacles
        var i = 0
        while (i < list.size) {
            val o = list[i]
            i++
            if (!o.active || !o.kind.blocksBullets) continue
            val reach = o.radius + 70f // cheap reject before the exact test
            if (b.x - o.x > reach || b.x - o.x < -reach || b.y - o.y > reach || b.y - o.y < -reach) continue
            val t = Collision.segmentHitsCircle(b.prevX, b.prevY, b.x, b.y, o.x, o.y, o.radius)
            if (t >= 0f && t < bestT) {
                bestT = t
                best = o
            }
        }
        val o = best ?: return false
        val hx = b.prevX + (b.x - b.prevX) * bestT
        val hy = b.prevY + (b.y - b.prevY) * bestT
        spawnImpact(hx, hy, onZombie = false)
        wallHitEffects(hx, hy, b.dirX, b.dirY)
        b.active = false
        if (o.kind == ObstacleKind.BARREL) {
            o.health -= b.damage
            if (o.health <= 0f) detonateBarrel(o)
        }
        return true
    }

    /** A barrel blows up like a grenade (it hurts zombies, not the player) and sets off barrels next to it. */
    private fun detonateBarrel(o: Obstacle) {
        if (!o.active) return
        o.active = false
        explode(o.x, o.y)
        addShake(0.35f)
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
        if (!b.fromTurret) shotsHit++ // turret bullets do not count towards accuracy
        val killed = z.takeDamage(b.damage)
        if (killed) {
            registerKill(z, b.dirX, b.dirY)
        } else if (!b.fromTurret && !z.isBoss && player.character.passive == PassiveType.FROSTBITE &&
            Random.nextFloat() < GameConfig.FROSTBITE_CHANCE
        ) {
            z.stunTimer = max(z.stunTimer, GameConfig.FROSTBITE_SECONDS) // Cryo: Frostbite freezes it briefly
        }
        z.x += b.dirX * GameConfig.HIT_KNOCKBACK
        z.y += b.dirY * GameConfig.HIT_KNOCKBACK
        spawnImpact(hitX, hitY, onZombie = true)
        hitEffects(z, hitX, hitY, b.dirX, b.dirY, killed)
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
        if (input.consumeGrenade()) throwGrenade()
        if (input.consumeAbility()) useAbility()
    }

    // ---------------------------------------------------------------------------------------
    // Character abilities
    // ---------------------------------------------------------------------------------------

    /** Fires the character's ability if it is ready, then starts the cooldown. */
    private fun useAbility() {
        if (!player.alive) return
        if (abilityCooldown > 0f) {
            playSound(SoundEffect.UI_DENIED)
            return
        }
        val ability = player.character.ability
        when (ability) {
            AbilityType.ADRENALINE -> {
                player.startAdrenaline(GameConfig.ADRENALINE_SECONDS)
                player.heal(GameConfig.ADRENALINE_HEAL)
                abilityBurst(PowerUpType.RAPID_FIRE)
            }
            AbilityType.DASH -> startDash()
            AbilityType.SHOCKWAVE -> shockwave()
            AbilityType.HEAL_PULSE -> {
                player.startHealOverTime(GameConfig.HEAL_PULSE_TOTAL, GameConfig.HEAL_PULSE_SECONDS)
                abilityBurst(PowerUpType.MEDKIT)
                playSound(SoundEffect.PICKUP_HEAL)
            }
            AbilityType.TURRET -> placeTurret()
            AbilityType.RAGE -> {
                player.grantRapidFire(GameConfig.RAGE_SECONDS)
                player.grantDamageBoost(GameConfig.RAGE_SECONDS)
                abilityBurst(PowerUpType.DOUBLE_DAMAGE)
                addShake(0.2f)
            }
            AbilityType.DEADEYE -> {
                player.startDeadeye(GameConfig.DEADEYE_SECONDS)
                abilityBurst(PowerUpType.DOUBLE_DAMAGE)
            }
            AbilityType.FREEZE -> freeze()
            AbilityType.AIRSTRIKE -> startAirstrike()
            AbilityType.JACKPOT -> jackpot()
        }
        abilityCooldown = player.character.abilityCooldownSeconds
    }

    /** Cryo's Freeze: every zombie in a wide circle is stunned (bosses for a shorter time). Never hurts the player. */
    private fun freeze() {
        val p = player
        playSound(SoundEffect.SHIELD_BLOCK, pitch = 0.8f)
        addShake(0.2f)
        radialBurst(ParticleSystem.PICKUP, p.x, p.y, 22, 460f, 0.7f, 4f, tag = PowerUpType.SHIELD.ordinal)
        radialBurst(ParticleSystem.SMOKE, p.x, p.y, 10, 300f, 0.8f, 10f)
        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            i++
            if (!z.alive) continue
            val dx = z.x - p.x
            val dy = z.y - p.y
            val reach = GameConfig.FREEZE_RADIUS + z.radius
            if (dx * dx + dy * dy > reach * reach) continue
            val stun = GameConfig.FREEZE_SECONDS * (if (z.isBoss) GameConfig.FREEZE_BOSS_STUN_FRACTION else 1f)
            z.stunTimer = max(z.stunTimer, stun)
        }
    }

    /** Demolitionist's Airstrike: [GameConfig.AIRSTRIKE_BLASTS] blasts in quick succession (see [updateAirstrike]). */
    private var strikesLeft = 0
    private var strikeTimer = 0f

    private fun startAirstrike() {
        strikesLeft = GameConfig.AIRSTRIKE_BLASTS
        strikeTimer = 0f
        playSound(SoundEffect.GRENADE_THROW, pitch = 0.7f)
        addShake(0.15f)
    }

    /** Drops the next airstrike blast, if one is due, on a random living zombie in range (or near the player if none). */
    private fun updateAirstrike(dt: Float) {
        if (strikesLeft <= 0) return
        strikeTimer -= dt
        if (strikeTimer > 0f) return
        strikeTimer = GameConfig.AIRSTRIKE_INTERVAL
        strikesLeft--

        val p = player
        val range2 = GameConfig.AIRSTRIKE_RANGE * GameConfig.AIRSTRIKE_RANGE
        var inRange = 0
        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            i++
            if (!z.alive) continue
            val dx = z.x - p.x
            val dy = z.y - p.y
            if (dx * dx + dy * dy <= range2) inRange++
        }
        var tx: Float
        var ty: Float
        if (inRange > 0) {
            var pick = Random.nextInt(inRange)
            tx = p.x
            ty = p.y
            i = 0
            while (i < zombies.size) {
                val z = zombies[i]
                i++
                if (!z.alive) continue
                val dx = z.x - p.x
                val dy = z.y - p.y
                if (dx * dx + dy * dy > range2) continue
                if (pick == 0) {
                    tx = z.x
                    ty = z.y
                    break
                }
                pick--
            }
        } else {
            val a = Random.nextFloat() * (2f * Math.PI.toFloat())
            val d = 200f + Random.nextFloat() * 220f
            tx = p.x + cos(a) * d
            ty = p.y + sin(a) * d
        }
        explode(tx.coerceIn(0f, WIDTH), ty.coerceIn(0f, HEIGHT))
    }

    /** Scavenger's Jackpot: random pickups (medkits more likely when hurt) land in a ring around the player. */
    private fun jackpot() {
        val p = player
        var i = 0
        while (i < GameConfig.JACKPOT_DROPS) {
            val a = i * (2f * Math.PI.toFloat() / GameConfig.JACKPOT_DROPS) + Random.nextFloat() * 0.6f
            dropPowerUp(
                pickDropType(),
                p.x + cos(a) * GameConfig.JACKPOT_SCATTER,
                p.y + sin(a) * GameConfig.JACKPOT_SCATTER
            )
            i++
        }
        playSound(SoundEffect.PICKUP_POWERUP)
        radialBurst(ParticleSystem.PICKUP, p.x, p.y, 16, 300f, 0.6f, 4f, tag = PowerUpType.GRENADE.ordinal)
    }

    /** A ring of sparkles around the player in the colour of [type], with the pickup sound. */
    private fun abilityBurst(type: PowerUpType) {
        playSound(SoundEffect.PICKUP_POWERUP)
        radialBurst(ParticleSystem.PICKUP, player.x, player.y, 14, 280f, 0.6f, 4f, tag = type.ordinal)
    }

    /** Dash along the movement stick, or the way the player faces if the stick is idle. */
    private fun startDash() {
        val p = player
        val len = sqrt(p.moveX * p.moveX + p.moveY * p.moveY)
        val dx = if (len > 0.1f) p.moveX else cos(p.rotation)
        val dy = if (len > 0.1f) p.moveY else sin(p.rotation)
        p.startDash(dx, dy, GameConfig.DASH_SECONDS)
        playSound(SoundEffect.GRENADE_THROW, pitch = 1.4f)
        radialBurst(ParticleSystem.DUST, p.x, p.y, 10, 180f, 0.5f, 7f)
    }

    /** A blast around the player: damages, shoves back and stuns every zombie in range. Never hurts the player. */
    private fun shockwave() {
        val p = player
        val e = explosionPool.obtain()
        e.start(p.x, p.y, GameConfig.SHOCKWAVE_RADIUS)
        explosions.add(e)
        sound(SoundEffect.EXPLOSION, p.x, p.y, 0.8f)
        addShake(0.35f)
        radialBurst(ParticleSystem.DUST, p.x, p.y, 16, 320f, 0.6f, 8f)

        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            i++
            if (!z.alive) continue
            val dx = z.x - p.x
            val dy = z.y - p.y
            val reach = GameConfig.SHOCKWAVE_RADIUS + z.radius
            val d2 = dx * dx + dy * dy
            if (d2 > reach * reach) continue
            val d = sqrt(d2)
            val nx = if (d > 0.001f) dx / d else 1f
            val ny = if (d > 0.001f) dy / d else 0f
            if (z.takeDamage(GameConfig.SHOCKWAVE_DAMAGE)) {
                registerKill(z, nx, ny)
            } else {
                z.x += nx * GameConfig.SHOCKWAVE_KNOCKBACK
                z.y += ny * GameConfig.SHOCKWAVE_KNOCKBACK
                val stun = GameConfig.SHOCKWAVE_STUN_SECONDS * (if (z.isBoss) GameConfig.SHOCKWAVE_BOSS_STUN_FRACTION else 1f)
                z.stunTimer = max(z.stunTimer, stun)
            }
        }
    }

    /** Drops a turret in front of the player. Only one stands at a time: a new one replaces the old. */
    private fun placeTurret() {
        val p = player
        turrets.clear()
        val t = Turret()
        t.place(
            (p.x + cos(p.rotation) * GameConfig.TURRET_PLACE_DISTANCE).coerceIn(t.radius, WIDTH - t.radius),
            (p.y + sin(p.rotation) * GameConfig.TURRET_PLACE_DISTANCE).coerceIn(t.radius, HEIGHT - t.radius)
        )
        t.rotation = p.rotation
        turrets.add(t)
        playSound(SoundEffect.PICKUP_GRENADE)
        radialBurst(ParticleSystem.SMOKE, t.x, t.y, 6, 120f, 0.7f, 10f)
    }

    /** Ages turrets, removes expired ones and lets the rest shoot the nearest zombie in range. */
    private fun updateTurrets(dt: Float) {
        var i = 0
        while (i < turrets.size) {
            val t = turrets[i]
            t.update(dt)
            if (!t.active) {
                turrets.removeAt(i)
                continue
            }
            aimAndFireTurret(t)
            i++
        }
    }

    private fun aimAndFireTurret(t: Turret) {
        var target: Zombie? = null
        var bestD2 = GameConfig.TURRET_RANGE * GameConfig.TURRET_RANGE
        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            i++
            if (!z.alive) continue
            val dx = z.x - t.x
            val dy = z.y - t.y
            val d2 = dx * dx + dy * dy
            if (d2 < bestD2) {
                bestD2 = d2
                target = z
            }
        }
        val z = target ?: return
        t.rotation = atan2(z.y - t.y, z.x - t.x)
        if (t.cooldown > 0f) return
        t.cooldown = GameConfig.TURRET_FIRE_INTERVAL
        val b = bulletPool.obtain()
        b.fire(t.x, t.y, t.rotation, GameConfig.TURRET_BULLET_SPEED, GameConfig.TURRET_DAMAGE, GameConfig.TURRET_RANGE + 80f, 1)
        b.fromTurret = true
        bullets.add(b)
        sound(SoundEffect.SHOT_SMG, t.x, t.y, 1.15f)
    }

    /**
     * Throws a grenade at the biggest group of zombies near the player (auto-aimed).
     * Every living zombie within throwing range is a candidate centre; the one with the most zombies
     * inside the blast radius wins (ties go to the closer one) and the grenade lands on the middle of
     * that group. If every zombie is out of range it is thrown the full distance towards the nearest
     * one. With no zombies alive nothing is thrown and no grenade is used. This runs once per tap,
     * never per frame, so the pairwise scan over at most a few dozen zombies is cheap.
     */
    private fun throwGrenade() {
        if (grenadeCount <= 0) return
        val p = player
        val range2 = GameConfig.GRENADE_MAX_RANGE * GameConfig.GRENADE_MAX_RANGE
        val blast2 = GameConfig.GRENADE_BLAST_RADIUS * GameConfig.GRENADE_BLAST_RADIUS

        var bestCount = 0
        var bestDist2 = Float.MAX_VALUE
        var bestX = 0f
        var bestY = 0f
        var nearest: Zombie? = null
        var nearestDist2 = Float.MAX_VALUE

        for (i in zombies.indices) {
            val z = zombies[i]
            if (!z.alive) continue
            val dx = z.x - p.x
            val dy = z.y - p.y
            val d2 = dx * dx + dy * dy
            if (d2 < nearestDist2) {
                nearestDist2 = d2
                nearest = z
            }
            if (d2 > range2) continue

            // how many zombies a blast centred on this one would catch, and where their middle is
            var count = 0
            var sumX = 0f
            var sumY = 0f
            for (j in zombies.indices) {
                val o = zombies[j]
                if (!o.alive) continue
                val ox = o.x - z.x
                val oy = o.y - z.y
                if (ox * ox + oy * oy <= blast2) {
                    count++
                    sumX += o.x
                    sumY += o.y
                }
            }
            if (count > bestCount || (count == bestCount && d2 < bestDist2)) {
                bestCount = count
                bestDist2 = d2
                bestX = sumX / count
                bestY = sumY / count
            }
        }

        val targetX: Float
        val targetY: Float
        val fallback = nearest
        if (bestCount > 0) {
            targetX = bestX
            targetY = bestY
        } else if (fallback != null) {
            targetX = fallback.x
            targetY = fallback.y
        } else {
            playSound(SoundEffect.UI_DENIED) // nothing to throw at, keep the grenade
            return
        }

        val dx = targetX - p.x
        val dy = targetY - p.y
        val angle = atan2(dy, dx)
        val distance = sqrt(dx * dx + dy * dy)
            .coerceIn(GameConfig.GRENADE_MIN_RANGE, GameConfig.GRENADE_MAX_RANGE)

        grenadeCount--
        p.grenadeFlash = FLASH_SECONDS
        playSound(SoundEffect.GRENADE_THROW)
        val g = grenadePool.obtain()
        g.launch(
            p.x + cos(angle) * GRENADE_SPAWN_OFFSET,
            p.y + sin(angle) * GRENADE_SPAWN_OFFSET,
            angle,
            (distance - GRENADE_SPAWN_OFFSET).coerceAtLeast(20f)
        )
        grenades.add(g)
    }

    private fun updateGrenades(dt: Float) {
        var i = 0
        while (i < grenades.size) {
            val g = grenades[i]
            g.update(dt)
            g.x = g.x.coerceIn(g.radius, WIDTH - g.radius)
            g.y = g.y.coerceIn(g.radius, HEIGHT - g.radius)
            if (g.active) {
                i++
            } else {
                explode(g.x, g.y)
                val last = grenades.size - 1
                grenades[i] = grenades[last]
                grenades.removeAt(last)
                grenadePool.release(g)
            }
        }
    }

    /** Damages every zombie in range (less towards the edge) and shoves it away. Does not hurt the player. */
    private fun explode(cx: Float, cy: Float) {
        val e = explosionPool.obtain()
        e.start(cx, cy, GameConfig.GRENADE_BLAST_RADIUS)
        explosions.add(e)
        explosionEffects(cx, cy)

        // chain reaction: barrels inside the blast go off too (a barrel is inactive before it explodes, so this ends)
        var k = 0
        while (k < map.obstacles.size) {
            val o = map.obstacles[k]
            k++
            if (!o.active || o.kind != ObstacleKind.BARREL) continue
            val bx = o.x - cx
            val by = o.y - cy
            val reach = GameConfig.GRENADE_BLAST_RADIUS + o.radius
            if (bx * bx + by * by <= reach * reach) detonateBarrel(o)
        }

        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            i++
            if (!z.alive) continue
            val dx = z.x - cx
            val dy = z.y - cy
            val reach = GameConfig.GRENADE_BLAST_RADIUS + z.radius
            val d2 = dx * dx + dy * dy
            if (d2 > reach * reach) continue
            val d = sqrt(d2)
            val t = (d / reach).coerceIn(0f, 1f)
            val damage = GameConfig.GRENADE_DAMAGE * (1f - (1f - GameConfig.GRENADE_EDGE_DAMAGE_FRACTION) * t) * player.character.blastDamageMult
            if (z.takeDamage(damage)) {
                registerKill(z, if (d > 0.001f) dx / d else 0f, if (d > 0.001f) dy / d else 1f)
            }
            if (d > 0.001f) {
                val push = GameConfig.GRENADE_KNOCKBACK * (1f - t)
                z.x += dx / d * push
                z.y += dy / d * push
            }
        }
    }

    /** Quicksand and snow slow whoever stands in them; solid props push everything out of their way. */
    private fun applyTerrain(e: Entity, startX: Float, startY: Float) {
        val slow = map.slowFactorAt(startX, startY)
        if (slow < 1f) {
            e.x = startX + (e.x - startX) * slow
            e.y = startY + (e.y - startY) * slow
        }
        if (map.resolve(e.x, e.y, e.radius)) {
            e.x = map.pushX
            e.y = map.pushY
        }
    }

    /** Standing in a poison pool keeps the player poisoned (like a Toxic zombie's hit, but it never stops while you stay). */
    private fun updatePoisonPools() {
        if (!player.alive || !map.inPoisonAt(player.x, player.y)) return
        val was = player.poisoned
        player.poison(GameConfig.POISON_POOL_DPS, GameConfig.POISON_POOL_SECONDS)
        if (!was) playSound(SoundEffect.POISONED)
    }

    private fun updateExplosions(dt: Float) {
        var i = 0
        while (i < explosions.size) {
            val e = explosions[i]
            e.update(dt)
            if (e.active) {
                i++
            } else {
                val last = explosions.size - 1
                explosions[i] = explosions[last]
                explosions.removeAt(last)
                explosionPool.release(e)
            }
        }
    }

    /** Ages pickups, removes expired ones and applies the ones the player walks over. */
    private fun updatePowerUps(dt: Float) {
        var i = 0
        while (i < powerUps.size) {
            val u = powerUps[i]
            u.update(dt)
            if (u.active) {
                val dx = u.x - player.x
                val dy = u.y - player.y
                val reach = u.radius + player.radius
                if (dx * dx + dy * dy <= reach * reach) {
                    applyPowerUp(u.type, u.x, u.y)
                    u.active = false
                }
            }
            if (u.active) {
                i++
            } else {
                val last = powerUps.size - 1
                powerUps[i] = powerUps[last]
                powerUps.removeAt(last)
                powerUpPool.release(u)
            }
        }
    }

    private fun applyPowerUp(type: PowerUpType, x: Float, y: Float) {
        pickupEffects(type, x, y)
        when (type) {
            PowerUpType.MEDKIT -> { player.heal(GameConfig.MEDKIT_HEAL); player.cure() }
            PowerUpType.RAPID_FIRE -> player.grantRapidFire(GameConfig.RAPID_FIRE_SECONDS * player.character.buffDurationMult)
            PowerUpType.DOUBLE_DAMAGE -> player.grantDamageBoost(GameConfig.DOUBLE_DAMAGE_SECONDS * player.character.buffDurationMult)
            PowerUpType.SHIELD -> player.grantShield(GameConfig.SHIELD_SECONDS * player.character.buffDurationMult)
            PowerUpType.GRENADE ->
                grenadeCount = (grenadeCount + GameConfig.GRENADE_PICKUP_AMOUNT).coerceAtMost(GameConfig.GRENADE_MAX_COUNT)
            PowerUpType.DIAMOND -> diamonds++
        }
    }

    /** Diamonds still lying on the ground (the level waits for these after the last boss). */
    fun diamondsOnGround(): Int {
        var n = 0
        var i = 0
        while (i < powerUps.size) {
            val u = powerUps[i]
            if (u.active && u.type == PowerUpType.DIAMOND) n++
            i++
        }
        return n
    }

    private fun dropPowerUp(type: PowerUpType, x: Float, y: Float) {
        val u = powerUpPool.obtain()
        u.spawn(type, x.coerceIn(u.radius, WIDTH - u.radius), y.coerceIn(u.radius, HEIGHT - u.radius))
        powerUps.add(u)
    }

    /** Weighted random pick; medkits are twice as likely while the player is below 40% health. */
    private fun pickDropType(): PowerUpType {
        val hurt = player.health < player.maxHealth * 0.4f
        var total = 0
        for (t in PowerUpType.entries) total += weightOf(t, hurt)
        var roll = Random.nextInt(total)
        for (t in PowerUpType.entries) {
            roll -= weightOf(t, hurt)
            if (roll < 0) return t
        }
        return PowerUpType.MEDKIT
    }

    private fun weightOf(t: PowerUpType, hurt: Boolean): Int =
        if (hurt && t == PowerUpType.MEDKIT) t.weight * 2 else t.weight

    /** A zombie's swing landed: damage the player, and poison them if it is a Toxic. */
    private fun hitPlayer(z: Zombie) {
        val shielded = player.shielded
        val wasPoisoned = player.poisoned
        sound(SoundEffect.ZOMBIE_SWING, z.x, z.y, pitchFor(z))
        if (player.takeDamage(z.swingDamage)) {
            playerHurtEffects(z)
            if (player.lastStandTriggered) {
                // Survivor's Last Stand just saved the run: burst, shake, and no poison on top of it
                player.lastStandTriggered = false
                abilityBurst(PowerUpType.SHIELD)
                addShake(0.3f)
            } else if (z.type.poisonDps > 0f) {
                player.poison(z.type.poisonDps, z.type.poisonSeconds)
                if (!wasPoisoned) playSound(SoundEffect.POISONED)
            }
            if (player.character.passive == PassiveType.THORNS && z.alive) {
                // Tank's Spiked Armor: the zombie that landed the swing is hurt in return
                if (z.takeDamage(GameConfig.THORNS_DAMAGE)) {
                    val dx = z.x - player.x
                    val dy = z.y - player.y
                    val d = sqrt(dx * dx + dy * dy)
                    registerKill(z, if (d > 0.001f) dx / d else 1f, if (d > 0.001f) dy / d else 0f)
                }
            }
        } else if (shielded) {
            shieldEffects()
        } else if (player.dodged) {
            // Runner's Nimble: the swing missed
            player.dodged = false
            radialBurst(ParticleSystem.DUST, player.x, player.y, 6, 170f, 0.4f, 6f)
        }
    }

    private fun registerKill(z: Zombie, dirX: Float, dirY: Float) {
        kills++
        coins += (z.type.coinReward * player.character.coinMult).roundToInt() // Scavenger bonus
        if (player.character.passive == PassiveType.TRIAGE && player.alive) player.heal(GameConfig.TRIAGE_KILL_HEAL)
        if (z.type.isBoss) {
            bossKills++
            // Bosses always drop a random power-up, a grenade pack and a handful of diamonds.
            dropPowerUp(pickDropType(), z.x - 40f, z.y)
            dropPowerUp(PowerUpType.GRENADE, z.x + 40f, z.y)
            for (n in 0 until GameConfig.BOSS_DIAMOND_DROPS) {
                dropPowerUp(PowerUpType.DIAMOND, z.x + (n - (GameConfig.BOSS_DIAMOND_DROPS - 1) / 2f) * 36f, z.y + 80f)
            }
        } else {
            if (Random.nextFloat() < z.type.dropChance) dropPowerUp(pickDropType(), z.x, z.y)
            if (Random.nextFloat() < z.type.diamondChance) dropPowerUp(PowerUpType.DIAMOND, z.x + 18f, z.y + 18f)
        }
        killEffects(z, dirX, dirY)
    }

    private fun updateZombies(dt: Float) {
        var alive = 0
        var foundBoss: Zombie? = null
        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            z.update(dt)
            if (z.alive) {
                alive++
                if (z.isBoss) foundBoss = z
                val startX = z.x
                val startY = z.y
                if (z.chase(dt, player, map)) hitPlayer(z)
                applyTerrain(z, startX, startY)
                if (z.isBoss) bossCues(z)
                if (z.summonReady) {
                    z.summonReady = false
                    if (zombies.size < GameConfig.BOSS_SUMMON_ALIVE_LIMIT) {
                        waves.spawnMinions(z.x, z.y, z.radius, GameConfig.BOSS_SUMMON_COUNT)
                        bossSummonEffects(z)
                    }
                }
                z.x = z.x.coerceIn(z.radius, WIDTH - z.radius)
                z.y = z.y.coerceIn(z.radius, HEIGHT - z.radius)
            }
            i++
        }
        aliveZombies = alive
        boss = foundBoss
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


    // =====================================================================================
    // Phase 9: sound, particles, stains, damage numbers and camera shake
    // =====================================================================================

    /** Plays a sound at full volume with no panning (the player's own actions, waves, game over). */
    fun playSound(effect: SoundEffect, volume: Float = 1f, pitch: Float = 1f) {
        audio?.play(effect, volume, 0f, pitch)
    }

    /** Plays a sound located in the world: quieter with distance, panned left/right of the player. */
    private fun sound(effect: SoundEffect, x: Float, y: Float, pitch: Float = 1f) {
        val a = audio ?: return
        val dx = x - player.x
        val dy = y - player.y
        val near = 1f - sqrt(dx * dx + dy * dy) / GameConfig.SFX_HEARING_RANGE
        if (near <= 0f) return
        val pan = (dx / GameConfig.SFX_PAN_RANGE).coerceIn(-1f, 1f) * GameConfig.SFX_PAN_STRENGTH
        a.play(effect, near * near, pan, pitch)
    }

    /** Big zombies sound deeper, small ones higher. */
    private fun pitchFor(z: Zombie): Float = (GameConfig.SFX_PITCH_REF_RADIUS / z.radius).coerceIn(0.6f, 1.25f)

    /** Adds camera shake (trauma is capped at 1 and fades on its own). */
    fun addShake(amount: Float) {
        if (!settings.screenShake) return
        trauma = min(1f, trauma + amount)
    }

    /** Shake that is weaker the farther the event is from the player. */
    private fun shakeNear(amount: Float, x: Float, y: Float) {
        val dx = x - player.x
        val dy = y - player.y
        addShake(amount * (1f - sqrt(dx * dx + dy * dy) / 1200f).coerceIn(0.25f, 1f))
    }

    /** Ages particles, stains, numbers and shake. Runs every tick, also behind the game over screen. */
    private fun updateEffects(dt: Float) {
        time += dt
        particles.update(dt)
        decals.update(dt)
        floatingTexts.update(dt)
        if (trauma > 0f) {
            trauma = max(0f, trauma - GameConfig.SHAKE_DECAY * dt)
            val m = GameConfig.SHAKE_MAX_OFFSET * trauma * trauma
            shakeX = (Random.nextFloat() * 2f - 1f) * m
            shakeY = (Random.nextFloat() * 2f - 1f) * m
        } else {
            shakeX = 0f
            shakeY = 0f
        }
    }

    /** Reload start / finish sounds, detected by watching the weapon's reload flag. */
    private fun syncReloadSounds() {
        val r = weapon.reloading
        if (r && !wasReloading) playSound(SoundEffect.RELOAD_START)
        else if (!r && wasReloading) playSound(SoundEffect.RELOAD_DONE)
        wasReloading = r
    }

    /** Background atmosphere: distant groans while zombies are around, a heartbeat at low health. */
    private fun updateAmbience(dt: Float) {
        if (area == GameArea.GRAVEYARD) updateThunder()
        groanTimer -= dt
        if (groanTimer <= 0f) {
            val base = GameConfig.GROAN_MIN_INTERVAL +
                    Random.nextFloat() * (GameConfig.GROAN_MAX_INTERVAL - GameConfig.GROAN_MIN_INTERVAL)
            groanTimer = base / (1f + aliveZombies * 0.04f)
            playGroan()
        }

        val lowLine = player.maxHealth * GameConfig.LOW_HEALTH_FRACTION
        if (player.health < lowLine) {
            heartbeatTimer -= dt
            if (heartbeatTimer <= 0f) {
                playSound(SoundEffect.HEARTBEAT)
                val ratio = (player.health / lowLine).coerceIn(0f, 1f) // 1 = just below the line, 0 = nearly dead
                heartbeatTimer = GameConfig.HEARTBEAT_INTERVAL * (1f + ratio)
            }
        } else {
            heartbeatTimer = 0f
        }
    }

    private var lastThunderCycle = -1

    /**
     * The graveyard's lightning flashes at the start of every 10 second cycle (see AtmosphereRenderer). Sound is
     * slower than light, so the thunder rolls in a moment after the flash.
     */
    private fun updateThunder() {
        val cycle = (time / GameConfig.LIGHTNING_PERIOD).toInt()
        val phase = time - cycle * GameConfig.LIGHTNING_PERIOD
        if (phase >= GameConfig.THUNDER_DELAY && phase < GameConfig.THUNDER_DELAY + 1f && cycle != lastThunderCycle) {
            lastThunderCycle = cycle
            playSound(SoundEffect.THUNDER)
        }
    }

    private fun playGroan() {
        if (zombies.isEmpty()) return
        var tries = 0
        while (tries < 6) {
            val z = zombies[Random.nextInt(zombies.size)]
            if (z.alive) {
                val effect = if (Random.nextBoolean()) SoundEffect.ZOMBIE_GROAN_1 else SoundEffect.ZOMBIE_GROAN_2
                sound(effect, z.x, z.y, pitchFor(z))
                return
            }
            tries++
        }
    }

    /** Sound, smoke, a brass casing and recoil shake for one shot. */
    private fun onShotFired(s: WeaponStats) {
        playSound(SoundEffect.forShot(s.type))
        val p = player
        val cosR = cos(p.rotation)
        val sinR = sin(p.rotation)
        val mx = p.x + cosR * s.type.muzzleOffset
        val my = p.y + sinR * s.type.muzzleOffset
        particles.spawn(
            ParticleSystem.SMOKE, mx, my,
            cosR * 45f + (Random.nextFloat() - 0.5f) * 30f, sinR * 45f + (Random.nextFloat() - 0.5f) * 30f,
            0.45f, 4f
        )
        // brass flies out to the player's right-hand side, perpendicular to the aim
        val eject = 150f + Random.nextFloat() * 90f
        particles.spawn(
            ParticleSystem.CASING, p.x + cosR * 16f, p.y + sinR * 16f,
            -sinR * eject - cosR * 30f, cosR * eject - sinR * 30f,
            1.1f, if (s.pellets >= 4) 5f else 3.5f, rot = Random.nextFloat() * 6.28f
        )
        if (s.pellets >= 4) addShake(GameConfig.SHAKE_ON_SHOTGUN)
    }

    /** Blood spray where a bullet struck a zombie, plus a thud. A killing blow is handled by [killEffects]. */
    private fun hitEffects(z: Zombie, x: Float, y: Float, dirX: Float, dirY: Float, killed: Boolean) {
        if (killed) return
        bloodBurst(x, y, dirX, dirY, 4, 220f, z.type == ZombieType.TOXIC)
        sound(SoundEffect.HIT_FLESH, x, y, pitchFor(z))
    }

    /** Sparks and dust where a bullet hit the edge of the map. */
    private fun wallHitEffects(x: Float, y: Float, dirX: Float, dirY: Float) {
        val base = atan2(-dirY, -dirX)
        var i = 0
        while (i < 5) {
            val a = base + (Random.nextFloat() - 0.5f) * 2.2f
            val s = 160f + Random.nextFloat() * 280f
            particles.spawn(ParticleSystem.SPARK, x, y, cos(a) * s, sin(a) * s, 0.2f + Random.nextFloat() * 0.15f, 2.5f)
            i++
        }
        radialBurst(ParticleSystem.DUST, x, y, 3, 70f, 0.5f, 5f)
    }

    /** Final damage number, blood, a stain and the death sound for a zombie that just died. */
    private fun killEffects(z: Zombie, dirX: Float, dirY: Float) {
        showDamageNumber(z, FloatingTexts.KILL)
        val goo = z.type == ZombieType.TOXIC
        val big = z.radius / 26f
        bloodBurst(z.x, z.y, dirX, dirY, (10f * big).toInt(), 320f, goo)
        radialBurst(if (goo) ParticleSystem.GOO else ParticleSystem.BLOOD, z.x, z.y, (6f * big).toInt(), 170f, 0.45f, 3.5f)
        decals.add(
            if (goo) DecalField.GOO else DecalField.BLOOD, z.x, z.y, z.radius * 2.6f,
            Random.nextFloat() * 6.28f,
            Random.nextInt(if (goo) DecalField.GOO_VARIANTS else DecalField.BLOOD_VARIANTS)
        )
        if (z.isBoss) {
            playSound(SoundEffect.BOSS_DEATH)
            shakeNear(GameConfig.SHAKE_ON_BOSS_DEATH, z.x, z.y)
            radialBurst(ParticleSystem.SMOKE, z.x, z.y, 12, 130f, 1.0f, 18f)
        } else {
            sound(SoundEffect.ZOMBIE_DEATH, z.x, z.y, pitchFor(z))
        }
    }

    /** A zombie's swing hurt the player: grunt, shake, blood and a stain. */
    private fun playerHurtEffects(z: Zombie) {
        playSound(SoundEffect.PLAYER_HURT)
        addShake(GameConfig.SHAKE_ON_HURT * (z.swingDamage / 20f).coerceIn(0.5f, 2f))
        val dx = player.x - z.x
        val dy = player.y - z.y
        val d = sqrt(dx * dx + dy * dy)
        val nx = if (d > 0.001f) dx / d else 1f
        val ny = if (d > 0.001f) dy / d else 0f
        bloodBurst(player.x, player.y, nx, ny, 7, 260f, false)
        decals.add(
            DecalField.BLOOD, player.x + nx * 20f, player.y + ny * 20f, 46f,
            Random.nextFloat() * 6.28f, Random.nextInt(DecalField.BLOOD_VARIANTS)
        )
    }

    /** The shield absorbed a swing: a ring of sparks and a zap. */
    private fun shieldEffects() {
        playSound(SoundEffect.SHIELD_BLOCK)
        var i = 0
        while (i < 8) {
            val a = Random.nextFloat() * 6.28f
            val s = 180f + Random.nextFloat() * 160f
            val r = player.radius + 16f
            particles.spawn(ParticleSystem.SPARK, player.x + cos(a) * r, player.y + sin(a) * r, cos(a) * s, sin(a) * s, 0.25f, 2.5f)
            i++
        }
    }

    private fun pickupEffects(type: PowerUpType, x: Float, y: Float) {
        playSound(
            when (type) {
                PowerUpType.MEDKIT -> SoundEffect.PICKUP_HEAL
                PowerUpType.GRENADE -> SoundEffect.PICKUP_GRENADE
                else -> SoundEffect.PICKUP_POWERUP
            }
        )
        radialBurst(ParticleSystem.PICKUP, x, y, 12, 260f, 0.55f, 4f, tag = type.ordinal)
    }

    private fun explosionEffects(cx: Float, cy: Float) {
        sound(SoundEffect.EXPLOSION, cx, cy)
        shakeNear(GameConfig.SHAKE_ON_EXPLOSION, cx, cy)
        decals.add(DecalField.SCORCH, cx, cy, GameConfig.GRENADE_BLAST_RADIUS * 1.1f, Random.nextFloat() * 6.28f, 0)
        radialBurst(ParticleSystem.FIRE, cx, cy, 28, 520f, 0.55f, 9f)
        radialBurst(ParticleSystem.SMOKE, cx, cy, 14, 160f, 1.1f, 16f)
        radialBurst(ParticleSystem.DUST, cx, cy, 14, 380f, 0.7f, 8f)
        radialBurst(ParticleSystem.SPARK, cx, cy, 14, 700f, 0.4f, 3f)
    }

    /** Boss ability cues: windup growl, charge whoosh and shake, enrage roar. */
    private fun bossCues(z: Zombie) {
        if (z.windupJustStarted) {
            z.windupJustStarted = false
            sound(SoundEffect.BOSS_WINDUP, z.x, z.y)
        }
        if (z.chargeJustStarted) {
            z.chargeJustStarted = false
            sound(SoundEffect.BOSS_CHARGE, z.x, z.y)
            shakeNear(GameConfig.SHAKE_ON_BOSS_CHARGE, z.x, z.y)
            var i = 0
            while (i < 6) { // dust kicked up behind the charge
                val s = 60f + Random.nextFloat() * 120f
                particles.spawn(
                    ParticleSystem.DUST, z.x - z.chargeDirX * z.radius, z.y - z.chargeDirY * z.radius,
                    -z.chargeDirX * s + (Random.nextFloat() - 0.5f) * 90f, -z.chargeDirY * s + (Random.nextFloat() - 0.5f) * 90f,
                    0.6f, 9f
                )
                i++
            }
        }
        if (z.enrageJustStarted) {
            z.enrageJustStarted = false
            sound(SoundEffect.BOSS_ROAR, z.x, z.y)
            shakeNear(GameConfig.SHAKE_ON_BOSS_ROAR, z.x, z.y)
        }
    }

    private fun bossSummonEffects(z: Zombie) {
        sound(SoundEffect.BOSS_SUMMON, z.x, z.y)
        radialBurst(ParticleSystem.SMOKE, z.x, z.y, 8, 120f, 0.8f, 14f)
    }

    private fun onPlayerDied() {
        playSound(SoundEffect.GAME_OVER)
        addShake(GameConfig.SHAKE_ON_GAME_OVER)
        radialBurst(ParticleSystem.BLOOD, player.x, player.y, 16, 260f, 0.5f, 4f)
        decals.add(DecalField.BLOOD, player.x, player.y, 130f, Random.nextFloat() * 6.28f, Random.nextInt(DecalField.BLOOD_VARIANTS))
    }

    /** Turns the damage a zombie took since its last number into one floating number. */
    private fun showDamageNumber(z: Zombie, kind: Int) {
        val dmg = z.pendingDamage
        z.pendingDamage = 0f
        if (!settings.damageNumbers || dmg < 0.5f) return
        z.numberCooldown = GameConfig.DAMAGE_NUMBER_INTERVAL
        val k = if (kind == FloatingTexts.NORMAL && player.damageBoost) FloatingTexts.BOOSTED else kind
        floatingTexts.spawn(z.x + (Random.nextFloat() - 0.5f) * 24f, z.y - z.radius - 8f, ceil(dmg).toInt(), k)
    }

    /** Shows the summed damage of zombies that were hit and are past their number cooldown. */
    private fun flushDamageNumbers() {
        var i = 0
        while (i < zombies.size) {
            val z = zombies[i]
            i++
            if (z.alive && z.pendingDamage > 0f && z.numberCooldown <= 0f) showDamageNumber(z, FloatingTexts.NORMAL)
        }
    }

    /** Blood (or toxic goo) sprayed in a cone around the direction (dirX, dirY). */
    private fun bloodBurst(x: Float, y: Float, dirX: Float, dirY: Float, count: Int, speed: Float, goo: Boolean) {
        val n = (count * settings.gore.amount).toInt()
        val kind = if (goo) ParticleSystem.GOO else ParticleSystem.BLOOD
        val base = atan2(dirY, dirX)
        var i = 0
        while (i < n) {
            val a = base + (Random.nextFloat() - 0.5f) * 1.5f
            val s = speed * (0.35f + Random.nextFloat() * 0.75f)
            particles.spawn(kind, x, y, cos(a) * s, sin(a) * s, 0.3f + Random.nextFloat() * 0.3f, 2.5f + Random.nextFloat() * 3.5f)
            i++
        }
    }

    /** [count] particles flying out in all directions; speed, life and size vary a little. */
    private fun radialBurst(kind: Int, x: Float, y: Float, count: Int, speed: Float, life: Float, size: Float, tag: Int = 0) {
        val n = if (kind == ParticleSystem.BLOOD || kind == ParticleSystem.GOO) (count * settings.gore.amount).toInt() else count
        var i = 0
        while (i < n) {
            val a = Random.nextFloat() * 6.2832f
            val s = speed * (0.3f + Random.nextFloat() * 0.7f)
            particles.spawn(
                kind, x, y, cos(a) * s, sin(a) * s,
                life * (0.7f + Random.nextFloat() * 0.5f), size * (0.6f + Random.nextFloat() * 0.6f), tag
            )
            i++
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
        val rapid = buffSeconds(player.rapidFireTimer)
        val damageBuff = buffSeconds(player.damageBoostTimer)
        val shield = buffSeconds(player.shieldTimer)
        val bossZombie = boss
        val bossType = bossZombie?.type
        val ability = player.character.ability
        val abilityPercent = if (abilityCooldown <= 0f) 100
            else ((1f - abilityCooldown / player.character.abilityCooldownSeconds) * 20f).toInt().coerceIn(0, 19) * 5
        val bossPercent = if (bossZombie != null) ceil(bossZombie.health / bossZombie.maxHealth * 100f).toInt().coerceIn(0, 100) else 0
        if (cur.health == health && cur.maxHealth == maxHealth && cur.poisoned == player.poisoned &&
            cur.wave == waves.wave && cur.phase == waves.phase && cur.countdownSeconds == countdown &&
            cur.clearedBonus == waves.clearedBonus && cur.zombiesLeft == left &&
            cur.kills == kills && cur.coins == coins && cur.gameOver == gameOver &&
            cur.weapon == weapon.stats.type && cur.ammo == weapon.ammo && cur.magazine == weapon.stats.magazine &&
            cur.reloading == weapon.reloading && cur.reloadPercent == reloadPercent &&
            cur.grenades == grenadeCount && cur.rapidFireSeconds == rapid &&
            cur.damageBoostSeconds == damageBuff && cur.shieldSeconds == shield &&
            cur.bossWave == waves.isBossWave && cur.boss == bossType && cur.bossPercent == bossPercent &&
            cur.diamonds == diamonds && cur.ability == ability && cur.abilityPercent == abilityPercent &&
            cur.totalWaves == setup.totalWaves && cur.level == levelNumber && cur.levelComplete == waves.levelComplete &&
            cur.collectingLoot == waves.collectingLoot
        ) return
        hud.value = HudState(
            health = health, maxHealth = maxHealth, poisoned = player.poisoned,
            wave = waves.wave, phase = waves.phase, countdownSeconds = countdown,
            clearedBonus = waves.clearedBonus, zombiesLeft = left,
            kills = kills, coins = coins, gameOver = gameOver,
            weapon = weapon.stats.type, ammo = weapon.ammo, magazine = weapon.stats.magazine,
            reloading = weapon.reloading, reloadPercent = reloadPercent,
            grenades = grenadeCount, rapidFireSeconds = rapid, damageBoostSeconds = damageBuff,
            shieldSeconds = shield, bossWave = waves.isBossWave, boss = bossType, bossPercent = bossPercent,
            diamonds = diamonds, ability = ability, abilityPercent = abilityPercent,
            totalWaves = setup.totalWaves, level = levelNumber, levelComplete = waves.levelComplete,
            collectingLoot = waves.collectingLoot
        )
    }

    /** Campaign level number for the HUD; 0 in the endless mode. */
    private val levelNumber: Int get() = if (setup.isCampaign) setup.level else 0

    private fun buffSeconds(timer: Float): Int = if (timer > 0f) ceil(timer).toInt() else 0
}