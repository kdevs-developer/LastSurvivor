package com.kdevs.lastsurvivor.game

import com.kdevs.lastsurvivor.game.engine.GameArea
import com.kdevs.lastsurvivor.game.engine.GameConfig
import com.kdevs.lastsurvivor.game.engine.GameWorld
import com.kdevs.lastsurvivor.game.engine.HazardKind
import com.kdevs.lastsurvivor.game.engine.MapLayout
import com.kdevs.lastsurvivor.game.engine.ObstacleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class MapLayoutTest {

    private fun layout(area: GameArea, seed: Int = 7) = MapLayout().also { it.build(area, seed) }

    @Test fun `every area has its own props`() {
        for (area in GameArea.entries) assertTrue("$area has no obstacles", layout(area).obstacles.isNotEmpty())
        assertTrue(layout(GameArea.CITY).obstacles.any { it.kind == ObstacleKind.BARREL })
        assertTrue(layout(GameArea.SWAMP).hazards.any { it.kind == HazardKind.POISON_POOL })
        assertTrue(layout(GameArea.DESERT).hazards.any { it.kind == HazardKind.QUICKSAND })
        assertTrue(layout(GameArea.TUNDRA).hazards.any { it.kind == HazardKind.SNOW_DRIFT })
        assertTrue(layout(GameArea.GRAVEYARD).obstacles.any { it.kind == ObstacleKind.TOMBSTONE })
    }

    @Test fun `areas are not the same map in another colour`() {
        val kinds = GameArea.entries.map { a -> layout(a).obstacles.map { it.kind }.toSet() }
        for (i in kinds.indices) for (j in i + 1 until kinds.size) assertTrue(kinds[i].intersect(kinds[j]).isEmpty())
    }

    @Test fun `the same seed gives the same map and another seed another`() {
        val a = layout(GameArea.SWAMP, 5).obstacles.map { it.x to it.y }
        val b = layout(GameArea.SWAMP, 5).obstacles.map { it.x to it.y }
        val c = layout(GameArea.SWAMP, 6).obstacles.map { it.x to it.y }
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test fun `the player start is clear and props stay on the map`() {
        for (area in GameArea.entries) for (seed in 1..6) {
            for (o in layout(area, seed).obstacles) {
                val dx = o.x - GameWorld.WIDTH / 2f
                val dy = o.y - GameWorld.HEIGHT / 2f
                assertTrue("$area $seed: prop on the start", sqrt(dx * dx + dy * dy) >= 300f + o.radius - 0.01f)
                assertTrue(o.x - o.radius >= 0f && o.x + o.radius <= GameWorld.WIDTH)
                assertTrue(o.y - o.radius >= 0f && o.y + o.radius <= GameWorld.HEIGHT)
            }
        }
    }

    @Test fun `slow zones and poison pools are found where they are`() {
        val desert = layout(GameArea.DESERT)
        val sand = desert.hazards.first { it.kind == HazardKind.QUICKSAND }
        assertEquals(GameConfig.SLOW_ZONE_FACTOR, desert.slowFactorAt(sand.x, sand.y), 0.0001f)
        assertFalse(desert.inPoisonAt(sand.x, sand.y))
        val swamp = layout(GameArea.SWAMP)
        val pool = swamp.hazards.first { it.kind == HazardKind.POISON_POOL }
        assertTrue(swamp.inPoisonAt(pool.x, pool.y))
        assertEquals(1f, swamp.slowFactorAt(pool.x, pool.y), 0.0001f)
    }

    @Test fun `a circle inside an obstacle is pushed out to its edge`() {
        val map = layout(GameArea.GRAVEYARD)
        val o = map.obstacles.first()
        assertTrue(map.resolve(o.x + 1f, o.y, 1f))
        val dx = map.pushX - o.x
        val dy = map.pushY - o.y
        assertTrue(sqrt(dx * dx + dy * dy) >= o.radius + 1f - 0.01f)
        assertFalse(map.resolve(0f, 0f, 1f))
    }

    @Test fun `a destroyed barrel comes back on restore`() {
        val map = layout(GameArea.CITY)
        val barrel = map.obstacles.first { it.kind == ObstacleKind.BARREL }
        barrel.active = false
        barrel.health = 0f
        map.restore()
        assertTrue(barrel.active)
        assertEquals(GameConfig.BARREL_HEALTH, barrel.health, 0.0001f)
    }
}
