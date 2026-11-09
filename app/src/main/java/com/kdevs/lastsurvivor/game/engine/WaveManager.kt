package com.kdevs.lastsurvivor.game.engine

import com.kdevs.lastsurvivor.game.entities.ZombieType
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Wave progression: countdown -> spawn the wave's zombies -> wait until all are dead ->
 * award the clear bonus -> next countdown. Each wave has more zombies, which are tougher and
 * (slightly) faster, and new zombie types unlock at the waves set in [ZombieType.unlockWave].
 * All tuning lives in [GameConfig]. Phase 8 hooks boss waves in here.
 */
class WaveManager(private val world: GameWorld) {

    var wave: Int = 1
        private set
    var phase: WavePhase = WavePhase.COUNTDOWN
        private set
    var countdown: Float = GameConfig.FIRST_WAVE_COUNTDOWN
        private set
    var clearedBonus: Int = 0
        private set

    /** Zombies of the current wave that have not spawned yet. */
    private var toSpawn = 0
    private var spawnTimer = 0f

    val zombiesLeft: Int get() = toSpawn + world.aliveZombies

    private val types: Array<ZombieType> = ZombieType.entries.toTypedArray()

    fun reset() {
        wave = 1
        phase = WavePhase.COUNTDOWN
        countdown = GameConfig.FIRST_WAVE_COUNTDOWN
        clearedBonus = 0
        toSpawn = 0
        spawnTimer = 0f
    }

    fun update(dt: Float) {
        when (phase) {
            WavePhase.COUNTDOWN -> {
                countdown -= dt
                if (countdown <= 0f) startWave()
            }
            WavePhase.ACTIVE -> updateActive(dt)
        }
    }

    private fun startWave() {
        phase = WavePhase.ACTIVE
        clearedBonus = 0
        toSpawn = min(GameConfig.WAVE_BASE_COUNT + GameConfig.WAVE_COUNT_PER_WAVE * wave, GameConfig.WAVE_MAX_COUNT)
        spawnTimer = 0.5f
    }

    private fun updateActive(dt: Float) {
        if (toSpawn > 0) {
            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                spawnTimer = spawnInterval()
                if (world.aliveZombies < maxAlive()) {
                    spawnOne()
                    toSpawn--
                }
            }
        } else if (world.aliveZombies == 0) {
            clearWave()
        }
    }

    private fun clearWave() {
        clearedBonus = GameConfig.WAVE_CLEAR_COINS_PER_WAVE * wave
        world.addCoins(clearedBonus)
        wave++
        phase = WavePhase.COUNTDOWN
        countdown = GameConfig.WAVE_COUNTDOWN
    }

    private fun spawnInterval(): Float =
        max(GameConfig.WAVE_MIN_SPAWN_INTERVAL, GameConfig.WAVE_SPAWN_INTERVAL - GameConfig.WAVE_SPAWN_INTERVAL_STEP * wave)

    private fun maxAlive(): Int =
        min(GameConfig.WAVE_MAX_ALIVE_BASE + GameConfig.WAVE_MAX_ALIVE_PER_WAVE * wave, GameConfig.WAVE_MAX_ALIVE_CAP)

    private fun spawnOne() {
        val type = pickType()
        val healthScale = 1f + GameConfig.WAVE_HEALTH_STEP * (wave - 1)
        val speedScale = min(GameConfig.WAVE_SPEED_CAP, 1f + GameConfig.WAVE_SPEED_STEP * (wave - 1))

        // Just outside the visible area, in a random direction around the player.
        val vw = world.viewportWidth
        val vh = world.viewportHeight
        val distance = max(600f, sqrt(vw * vw + vh * vh) / 2f + 120f)
        val angle = Random.nextFloat() * (2f * Math.PI.toFloat())
        val x = (world.player.x + cos(angle) * distance).coerceIn(type.radius, GameWorld.WIDTH - type.radius)
        val y = (world.player.y + sin(angle) * distance).coerceIn(type.radius, GameWorld.HEIGHT - type.radius)
        world.spawnZombie(type, x, y, healthScale, speedScale)
    }

    /** Weighted pick among the types unlocked at this wave. */
    private fun pickType(): ZombieType {
        var total = 0
        for (t in types) if (t.unlockWave <= wave) total += t.spawnWeight
        var roll = Random.nextInt(total)
        for (t in types) {
            if (t.unlockWave > wave) continue
            roll -= t.spawnWeight
            if (roll < 0) return t
        }
        return types[0]
    }
}
