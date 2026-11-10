package com.kdevs.lastsurvivor.game.engine

import com.kdevs.lastsurvivor.game.audio.SoundEffect
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
 * Every [GameConfig.BOSS_WAVE_INTERVAL]th wave is a boss wave: the boss spawns at once with a
 * smaller escort, and clearing it pays double. All tuning lives in [GameConfig].
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
    private val bosses: List<ZombieType> = ZombieType.entries.filter { it.isBoss }

    /** True when the current wave (the one counting down or in progress) has a boss. */
    val isBossWave: Boolean get() = isBossWave(wave)

    private fun isBossWave(w: Int): Boolean = w % GameConfig.BOSS_WAVE_INTERVAL == 0

    fun reset() {
        wave = 1
        phase = WavePhase.COUNTDOWN
        countdown = GameConfig.FIRST_WAVE_COUNTDOWN
        clearedBonus = 0
        toSpawn = 0
        spawnTimer = 0f
    }

    /** Starts the run at [w] with a normal between-waves countdown (Continue). */
    fun restoreWave(w: Int) {
        reset()
        wave = w.coerceAtLeast(1)
        countdown = GameConfig.WAVE_COUNTDOWN
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
        val normalCount = min(GameConfig.WAVE_BASE_COUNT + GameConfig.WAVE_COUNT_PER_WAVE * wave, GameConfig.WAVE_MAX_COUNT)
        toSpawn = if (isBossWave) (normalCount * GameConfig.BOSS_WAVE_ESCORT_FRACTION).toInt() else normalCount
        spawnTimer = 0.5f
        if (isBossWave) {
            spawnBoss()
            world.playSound(SoundEffect.BOSS_ROAR)
            world.addShake(GameConfig.SHAKE_ON_BOSS_ROAR)
        } else {
            world.playSound(SoundEffect.WAVE_START)
        }
    }

    /** Bosses alternate through the boss types and get tougher each time. */
    private fun spawnBoss() {
        val bossNumber = wave / GameConfig.BOSS_WAVE_INTERVAL // 1 for the first boss
        val type = bosses[(bossNumber - 1) % bosses.size]
        val healthScale = 1f + GameConfig.BOSS_HEALTH_PER_BOSS * (bossNumber - 1)
        spawnOffscreen(type, healthScale, 1f)
    }

    /** A Brood Mother's summon: [count] Walkers in a ring just outside the boss. */
    fun spawnMinions(cx: Float, cy: Float, ringRadius: Float, count: Int) {
        val type = ZombieType.WALKER
        val healthScale = 1f + GameConfig.WAVE_HEALTH_STEP * (wave - 1)
        val speedScale = min(GameConfig.WAVE_SPEED_CAP, 1f + GameConfig.WAVE_SPEED_STEP * (wave - 1))
        val start = Random.nextFloat() * (2f * Math.PI.toFloat())
        for (i in 0 until count) {
            val a = start + i * (2f * Math.PI.toFloat() / count)
            val x = (cx + cos(a) * (ringRadius + type.radius)).coerceIn(type.radius, GameWorld.WIDTH - type.radius)
            val y = (cy + sin(a) * (ringRadius + type.radius)).coerceIn(type.radius, GameWorld.HEIGHT - type.radius)
            world.spawnZombie(type, x, y, healthScale, speedScale)
        }
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
        clearedBonus = GameConfig.WAVE_CLEAR_COINS_PER_WAVE * wave *
            (if (isBossWave) GameConfig.BOSS_CLEAR_COIN_MULT else 1)
        world.addCoins(clearedBonus)
        world.playSound(SoundEffect.WAVE_CLEAR)
        wave++
        phase = WavePhase.COUNTDOWN
        countdown = GameConfig.WAVE_COUNTDOWN
    }

    private fun spawnInterval(): Float =
        max(GameConfig.WAVE_MIN_SPAWN_INTERVAL, GameConfig.WAVE_SPAWN_INTERVAL - GameConfig.WAVE_SPAWN_INTERVAL_STEP * wave)

    private fun maxAlive(): Int =
        min(GameConfig.WAVE_MAX_ALIVE_BASE + GameConfig.WAVE_MAX_ALIVE_PER_WAVE * wave, GameConfig.WAVE_MAX_ALIVE_CAP)

    private fun spawnOne() {
        val healthScale = 1f + GameConfig.WAVE_HEALTH_STEP * (wave - 1)
        val speedScale = min(GameConfig.WAVE_SPEED_CAP, 1f + GameConfig.WAVE_SPEED_STEP * (wave - 1))
        spawnOffscreen(pickType(), healthScale, speedScale)
    }

    private fun spawnOffscreen(type: ZombieType, healthScale: Float, speedScale: Float) {
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
