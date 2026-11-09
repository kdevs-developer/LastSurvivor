package com.kdevs.lastsurvivor.game.engine

import com.kdevs.lastsurvivor.game.entities.ZombieType
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * TEMPORARY spawner for Phase 4: keeps a steady trickle of mixed zombies coming from
 * just outside the screen. Phase 6 replaces it with the wave system (wave size, boss, countdown).
 */
class ZombieSpawner(private val world: GameWorld) {

    companion object {
        private const val INTERVAL_SECONDS = 1.5f
        private const val MAX_ALIVE = 25
    }

    private val types: Array<ZombieType> = ZombieType.entries.toTypedArray()
    private val totalWeight: Int = types.sumOf { it.spawnWeight }
    private var timer = 1f

    fun update(dt: Float) {
        timer -= dt
        if (timer > 0f) return
        timer = INTERVAL_SECONDS
        if (world.aliveZombies >= MAX_ALIVE) return
        spawnOne()
    }

    private fun spawnOne() {
        val type = pickType()
        val vw = world.viewportWidth
        val vh = world.viewportHeight
        val distance = max(600f, sqrt(vw * vw + vh * vh) / 2f + 120f)
        val angle = Random.nextFloat() * (2f * Math.PI.toFloat())

        val x = (world.player.x + cos(angle) * distance).coerceIn(type.radius, GameWorld.WIDTH - type.radius)
        val y = (world.player.y + sin(angle) * distance).coerceIn(type.radius, GameWorld.HEIGHT - type.radius)
        world.spawnZombie(type, x, y)
    }

    private fun pickType(): ZombieType {
        var roll = Random.nextInt(totalWeight)
        for (t in types) {
            roll -= t.spawnWeight
            if (roll < 0) return t
        }
        return types[0]
    }
}
