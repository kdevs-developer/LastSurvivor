package com.kdevs.lastsurvivor.game.engine

import kotlin.math.sqrt
import kotlin.random.Random

/** What a solid prop looks like and how it behaves. Drawn in code by PropRenderer. */
enum class ObstacleKind(val blocksBullets: Boolean = true) {
    // Ruined City
    CAR, BARRIER, BARREL,
    // Toxic Swamp
    SWAMP_TREE, SWAMP_ROCK,
    // Dust Wasteland
    BOULDER, CACTUS,
    // Frozen Tundra
    ICE_ROCK, PINE,
    // Haunted Graveyard
    TOMBSTONE, CRYPT, DEAD_TREE
}

/** Ground that changes how you move or what happens to you. */
enum class HazardKind(val slows: Boolean, val poisons: Boolean) {
    POISON_POOL(slows = false, poisons = true),   // Swamp: standing in it poisons the player
    QUICKSAND(slows = true, poisons = false),     // Desert: slows the player and zombies
    SNOW_DRIFT(slows = true, poisons = false)     // Tundra: slows the player and zombies
}

/** A solid circle on the map. Players, zombies and (most) bullets cannot pass; barrels explode when shot. */
class Obstacle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val kind: ObstacleKind,
    /** Drawing angle in radians, and a small number that picks between looks. */
    val angle: Float,
    val variant: Int
) {
    var active: Boolean = true
    var health: Float = 0f
}

class Hazard(val x: Float, val y: Float, val radius: Float, val kind: HazardKind, val variant: Int)

/**
 * The props of one map: obstacles and hazards, generated from a seed so a level or area always looks
 * the same. Each [GameArea] has its own recipe, so the areas differ in how they play, not only in colour:
 *
 *  - Ruined City: wrecked cars and concrete barriers for cover, explosive barrels (shoot them!)
 *  - Toxic Swamp: dense trees, rocks and poison pools you must not stand in
 *  - Dust Wasteland: boulders, cacti and quicksand that slows you (and the zombies)
 *  - Frozen Tundra: ice rocks, pines and deep snow drifts that slow everything
 *  - Haunted Graveyard: rows of tombstones, crypts and dead trees (the renderer adds darkness and fog)
 *
 * Nothing is generated near the player's start, and obstacles keep a gap between each other so there is
 * always room to walk through.
 */
class MapLayout {

    val obstacles = ArrayList<Obstacle>(128)
    val hazards = ArrayList<Hazard>(16)

    private val w = GameWorld.WIDTH
    private val h = GameWorld.HEIGHT
    private val cx = w / 2f
    private val cy = h / 2f

    /** Rebuilds the layout for [area]; [seed] makes each campaign level different. */
    fun build(area: GameArea, seed: Int) {
        obstacles.clear()
        hazards.clear()
        val rnd = Random(seed)
        when (area) {
            GameArea.CITY -> buildCity(rnd)
            GameArea.SWAMP -> buildSwamp(rnd)
            GameArea.DESERT -> buildDesert(rnd)
            GameArea.TUNDRA -> buildTundra(rnd)
            GameArea.GRAVEYARD -> buildGraveyard(rnd)
        }
    }

    /** New run on the same map: destroyed barrels are back. */
    fun restore() {
        var i = 0
        while (i < obstacles.size) {
            val o = obstacles[i]
            o.active = true
            if (o.kind == ObstacleKind.BARREL) o.health = GameConfig.BARREL_HEALTH
            i++
        }
    }

    // ------------------------------------------------------------------ queries (allocation-free)

    /** Movement factor at a point: [GameConfig.SLOW_ZONE_FACTOR] inside a slowing hazard, otherwise 1. */
    fun slowFactorAt(x: Float, y: Float): Float {
        var i = 0
        while (i < hazards.size) {
            val hz = hazards[i]
            i++
            if (!hz.kind.slows) continue
            val dx = x - hz.x
            val dy = y - hz.y
            if (dx * dx + dy * dy < hz.radius * hz.radius) return GameConfig.SLOW_ZONE_FACTOR
        }
        return 1f
    }

    fun inPoisonAt(x: Float, y: Float): Boolean {
        var i = 0
        while (i < hazards.size) {
            val hz = hazards[i]
            i++
            if (!hz.kind.poisons) continue
            val dx = x - hz.x
            val dy = y - hz.y
            if (dx * dx + dy * dy < hz.radius * hz.radius) return true
        }
        return false
    }

    /** Pushes a circle at ([x],[y]) out of every solid obstacle; returns true if it moved. Result in [pushX]/[pushY]. */
    var pushX: Float = 0f
        private set
    var pushY: Float = 0f
        private set

    fun resolve(x: Float, y: Float, radius: Float): Boolean {
        var px = x
        var py = y
        var moved = false
        var i = 0
        while (i < obstacles.size) {
            val o = obstacles[i]
            i++
            if (!o.active) continue
            val dx = px - o.x
            val dy = py - o.y
            val min = o.radius + radius
            if (dx > min || dx < -min || dy > min || dy < -min) continue
            val d2 = dx * dx + dy * dy
            if (d2 >= min * min) continue
            if (d2 > 0.0001f) {
                val d = sqrt(d2)
                px = o.x + dx / d * min
                py = o.y + dy / d * min
            } else {
                px = o.x + min // exactly on the centre: pick a side
            }
            moved = true
        }
        pushX = px
        pushY = py
        return moved
    }

    // ------------------------------------------------------------------ placement helpers

    /** Adds a solid prop if it is clear of the player's start and keeps [gap] to every other prop. */
    private fun place(x: Float, y: Float, r: Float, kind: ObstacleKind, rnd: Random, gap: Float = GAP): Boolean {
        if (x < r + EDGE || x > w - r - EDGE || y < r + EDGE || y > h - r - EDGE) return false
        val sx = x - cx
        val sy = y - cy
        val safe = START_CLEAR + r
        if (sx * sx + sy * sy < safe * safe) return false
        var i = 0
        while (i < obstacles.size) {
            val o = obstacles[i]
            i++
            val dx = x - o.x
            val dy = y - o.y
            val min = r + o.radius + gap
            if (dx * dx + dy * dy < min * min) return false
        }
        val o = Obstacle(x, y, r, kind, rnd.nextFloat() * 6.2832f, rnd.nextInt(3))
        if (kind == ObstacleKind.BARREL) o.health = GameConfig.BARREL_HEALTH
        obstacles.add(o)
        return true
    }

    private fun placeHazard(x: Float, y: Float, r: Float, kind: HazardKind, rnd: Random): Boolean {
        val sx = x - cx
        val sy = y - cy
        val safe = START_CLEAR + r * 0.6f
        if (sx * sx + sy * sy < safe * safe) return false
        if (x < EDGE || x > w - EDGE || y < EDGE || y > h - EDGE) return false
        var i = 0
        while (i < hazards.size) {
            val o = hazards[i]
            i++
            val dx = x - o.x
            val dy = y - o.y
            val min = r + o.radius
            if (dx * dx + dy * dy < min * min) return false
        }
        hazards.add(Hazard(x, y, r, kind, rnd.nextInt(3)))
        return true
    }

    /** Tries up to [tries] random spots until [count] props were placed. */
    private inline fun scatter(count: Int, rnd: Random, tries: Int = 400, put: (Float, Float) -> Boolean) {
        var placed = 0
        var t = 0
        while (placed < count && t < tries) {
            t++
            if (put(EDGE + rnd.nextFloat() * (w - 2f * EDGE), EDGE + rnd.nextFloat() * (h - 2f * EDGE))) placed++
        }
    }

    // ------------------------------------------------------------------ the five recipes

    /** Streets: lines of concrete barriers, wrecked cars, and clusters of explosive barrels. */
    private fun buildCity(rnd: Random) {
        // roadblocks: 3-4 barriers in a row, with gaps to run through
        scatter(9, rnd) { x, y ->
            val angle = if (rnd.nextBoolean()) 0f else 1.5708f
            val n = 3 + rnd.nextInt(2)
            var ok = false
            for (k in 0 until n) {
                val ox = x + kotlin.math.cos(angle) * k * 78f
                val oy = y + kotlin.math.sin(angle) * k * 78f
                if (place(ox, oy, 30f, ObstacleKind.BARRIER, rnd, gap = 6f)) ok = true
            }
            ok
        }
        scatter(22, rnd) { x, y -> place(x, y, 44f, ObstacleKind.CAR, rnd) }
        // barrel clusters
        scatter(7, rnd) { x, y ->
            var any = false
            for (k in 0 until 3) {
                if (place(x + (k - 1) * 52f, y + rnd.nextFloat() * 30f, 22f, ObstacleKind.BARREL, rnd, gap = 4f)) any = true
            }
            any
        }
    }

    /** Dense forest with poison pools. */
    private fun buildSwamp(rnd: Random) {
        scatter(10, rnd) { x, y -> placeHazard(x, y, 110f + rnd.nextFloat() * 90f, HazardKind.POISON_POOL, rnd) }
        scatter(52, rnd, tries = 900) { x, y -> place(x, y, 38f + rnd.nextFloat() * 20f, ObstacleKind.SWAMP_TREE, rnd, gap = 70f) }
        scatter(14, rnd) { x, y -> place(x, y, 26f + rnd.nextFloat() * 14f, ObstacleKind.SWAMP_ROCK, rnd) }
    }

    /** Open ground with boulders, cacti and quicksand. */
    private fun buildDesert(rnd: Random) {
        scatter(9, rnd) { x, y -> placeHazard(x, y, 120f + rnd.nextFloat() * 80f, HazardKind.QUICKSAND, rnd) }
        scatter(26, rnd) { x, y -> place(x, y, 42f + rnd.nextFloat() * 30f, ObstacleKind.BOULDER, rnd, gap = 90f) }
        scatter(30, rnd) { x, y -> place(x, y, 17f, ObstacleKind.CACTUS, rnd, gap = 60f) }
    }

    /** Ice rocks, pines and deep snow. */
    private fun buildTundra(rnd: Random) {
        scatter(9, rnd) { x, y -> placeHazard(x, y, 130f + rnd.nextFloat() * 80f, HazardKind.SNOW_DRIFT, rnd) }
        scatter(28, rnd) { x, y -> place(x, y, 36f + rnd.nextFloat() * 30f, ObstacleKind.ICE_ROCK, rnd, gap = 80f) }
        scatter(26, rnd) { x, y -> place(x, y, 30f, ObstacleKind.PINE, rnd, gap = 70f) }
    }

    /** Rows of tombstones with aisles between them, crypts and dead trees. */
    private fun buildGraveyard(rnd: Random) {
        // crypts first (big), then rows of graves around them
        scatter(5, rnd) { x, y -> place(x, y, 62f, ObstacleKind.CRYPT, rnd, gap = 160f) }
        var y = 340f
        while (y < h - 300f) {
            var x = 300f
            while (x < w - 300f) {
                if (rnd.nextFloat() < 0.62f) place(x + rnd.nextFloat() * 16f - 8f, y + rnd.nextFloat() * 14f - 7f, 20f, ObstacleKind.TOMBSTONE, rnd, gap = 40f)
                x += 125f
            }
            y += 250f // 250 between rows leaves a wide aisle
        }
        scatter(18, rnd) { x, y2 -> place(x, y2, 28f, ObstacleKind.DEAD_TREE, rnd, gap = 90f) }
    }

    private companion object {
        const val EDGE = 120f           // keep props off the map border
        const val START_CLEAR = 300f    // nothing near the player's start
        const val GAP = 90f             // default free space between two props
    }
}
