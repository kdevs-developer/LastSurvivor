package com.kdevs.lastsurvivor.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.kdevs.lastsurvivor.game.engine.GameWorld
import com.kdevs.lastsurvivor.game.engine.Hazard
import com.kdevs.lastsurvivor.game.engine.HazardKind
import com.kdevs.lastsurvivor.game.engine.Obstacle
import com.kdevs.lastsurvivor.game.engine.ObstacleKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the obstacles and hazards of the current map in code (no bitmaps), only the ones on screen.
 * Allocation-free: every Paint and RectF is created once. Game thread only.
 */
class PropRenderer(private val colors: GameColors) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.propShadow }
    private val rect = RectF()

    // ------------------------------------------------------------------ hazards (on the floor)

    fun drawHazards(canvas: Canvas, world: GameWorld) {
        val list = world.map.hazards
        var i = 0
        while (i < list.size) {
            val hz = list[i]
            i++
            if (!onScreen(world, hz.x, hz.y, hz.radius)) continue
            when (hz.kind) {
                HazardKind.POISON_POOL -> drawPool(canvas, hz, world.time)
                HazardKind.QUICKSAND -> drawRings(canvas, hz, colors.hazardSand, colors.hazardSandRing)
                HazardKind.SNOW_DRIFT -> drawRings(canvas, hz, colors.hazardSnow, colors.hazardSnowRing)
            }
        }
    }

    private fun drawPool(canvas: Canvas, hz: Hazard, time: Float) {
        fill.color = colors.hazardPoison
        canvas.drawCircle(hz.x, hz.y, hz.radius, fill)
        fill.color = colors.hazardPoisonCore
        canvas.drawCircle(hz.x, hz.y, hz.radius * 0.68f, fill)
        // three bubbles drifting around the pool
        fill.color = colors.hazardPoisonBubble
        for (k in 0 until 3) {
            val a = time * 0.5f + k * 2.1f + hz.variant
            val r = hz.radius * (0.18f + 0.12f * sin(time * 1.7f + k))
            canvas.drawCircle(hz.x + cos(a) * hz.radius * 0.42f, hz.y + sin(a) * hz.radius * 0.42f, r * 0.45f, fill)
        }
    }

    /** Quicksand and snow drifts: a soft disc with rings. */
    private fun drawRings(canvas: Canvas, hz: Hazard, base: Int, ring: Int) {
        fill.color = base
        canvas.drawCircle(hz.x, hz.y, hz.radius, fill)
        line.color = ring
        line.strokeWidth = 4f
        canvas.drawCircle(hz.x, hz.y, hz.radius * 0.72f, line)
        canvas.drawCircle(hz.x, hz.y, hz.radius * 0.44f, line)
        canvas.drawCircle(hz.x, hz.y, hz.radius * 0.18f, line)
    }

    // ------------------------------------------------------------------ obstacles

    fun drawObstacles(canvas: Canvas, world: GameWorld) {
        val list = world.map.obstacles
        var i = 0
        while (i < list.size) {
            val o = list[i]
            i++
            if (!o.active || !onScreen(world, o.x, o.y, o.radius + 40f)) continue
            drawShadow(canvas, o)
            when (o.kind) {
                ObstacleKind.CAR -> drawCar(canvas, o)
                ObstacleKind.BARRIER -> drawBarrier(canvas, o)
                ObstacleKind.BARREL -> drawBarrel(canvas, o)
                ObstacleKind.SWAMP_TREE -> drawLeafyTree(canvas, o)
                ObstacleKind.SWAMP_ROCK -> drawRock(canvas, o, colors.propSwampRock, colors.propSwampRockLight)
                ObstacleKind.BOULDER -> drawRock(canvas, o, colors.propBoulder, colors.propBoulderLight)
                ObstacleKind.CACTUS -> drawCactus(canvas, o)
                ObstacleKind.ICE_ROCK -> drawIce(canvas, o)
                ObstacleKind.PINE -> drawPine(canvas, o)
                ObstacleKind.TOMBSTONE -> drawTombstone(canvas, o)
                ObstacleKind.CRYPT -> drawCrypt(canvas, o)
                ObstacleKind.DEAD_TREE -> drawDeadTree(canvas, o)
            }
        }
    }

    private fun onScreen(world: GameWorld, x: Float, y: Float, r: Float): Boolean =
        x + r > world.camera.x && x - r < world.camera.x + world.viewportWidth &&
            y + r > world.camera.y && y - r < world.camera.y + world.viewportHeight

    private fun drawShadow(canvas: Canvas, o: Obstacle) {
        rect.set(o.x - o.radius, o.y - o.radius * 0.55f + 10f, o.x + o.radius, o.y + o.radius * 0.55f + 18f)
        canvas.drawOval(rect, shadow)
    }

    private fun degrees(radians: Float): Float = radians * 57.29578f

    // ---- Ruined City

    private fun drawCar(canvas: Canvas, o: Obstacle) {
        canvas.save()
        canvas.rotate(degrees(o.angle), o.x, o.y)
        fill.color = colors.propCarWheel
        for (k in 0 until 4) { // four wheels at (+-30, +-28)
            val sx = if (k and 1 == 0) -30f else 30f
            val sy = if (k and 2 == 0) -28f else 28f
            rect.set(o.x + sx - 11f, o.y + sy - 5f, o.x + sx + 11f, o.y + sy + 5f)
            canvas.drawRoundRect(rect, 3f, 3f, fill)
        }
        fill.color = colors.propCarBody
        rect.set(o.x - 52f, o.y - 26f, o.x + 52f, o.y + 26f)
        canvas.drawRoundRect(rect, 12f, 12f, fill)
        fill.color = colors.propCarGlass
        rect.set(o.x - 26f, o.y - 20f, o.x + 30f, o.y + 20f)
        canvas.drawRoundRect(rect, 8f, 8f, fill)
        fill.color = colors.propCarRoof
        rect.set(o.x - 20f, o.y - 17f, o.x + 18f, o.y + 17f)
        canvas.drawRoundRect(rect, 6f, 6f, fill)
        canvas.restore()
    }

    private fun drawBarrier(canvas: Canvas, o: Obstacle) {
        canvas.save()
        canvas.rotate(degrees(o.angle), o.x, o.y)
        fill.color = colors.propConcrete
        rect.set(o.x - 30f, o.y - 14f, o.x + 30f, o.y + 14f)
        canvas.drawRoundRect(rect, 4f, 4f, fill)
        line.color = colors.propConcreteDark
        line.strokeWidth = 3f
        canvas.drawLine(o.x - 20f, o.y - 14f, o.x - 10f, o.y + 14f, line)
        canvas.drawLine(o.x + 10f, o.y - 14f, o.x + 20f, o.y + 14f, line)
        canvas.restore()
    }

    private fun drawBarrel(canvas: Canvas, o: Obstacle) {
        fill.color = colors.propBarrel
        canvas.drawCircle(o.x, o.y, o.radius, fill)
        line.color = colors.propBarrelBand
        line.strokeWidth = 5f
        canvas.drawCircle(o.x, o.y, o.radius * 0.66f, line)
        fill.color = colors.propBarrelTop
        canvas.drawCircle(o.x, o.y, o.radius * 0.42f, fill)
    }

    // ---- Toxic Swamp

    private fun drawLeafyTree(canvas: Canvas, o: Obstacle) {
        val r = o.radius
        fill.color = colors.propTreeTrunk
        canvas.drawCircle(o.x, o.y, r * 0.3f, fill)
        fill.color = colors.propTreeCanopy
        canvas.drawCircle(o.x - r * 0.35f, o.y - r * 0.1f, r * 0.78f, fill)
        canvas.drawCircle(o.x + r * 0.4f, o.y, r * 0.72f, fill)
        canvas.drawCircle(o.x, o.y + r * 0.38f, r * 0.72f, fill)
        canvas.drawCircle(o.x, o.y - r * 0.3f, r * 0.7f, fill)
        fill.color = colors.propTreeCanopyLight
        canvas.drawCircle(o.x - r * 0.3f, o.y - r * 0.35f, r * 0.35f, fill)
        canvas.drawCircle(o.x + r * 0.25f, o.y + r * 0.1f, r * 0.28f, fill)
    }

    // ---- shared by the swamp and the desert

    private fun drawRock(canvas: Canvas, o: Obstacle, base: Int, light: Int) {
        fill.color = base
        canvas.drawCircle(o.x, o.y, o.radius, fill)
        fill.color = light
        canvas.drawCircle(o.x - o.radius * 0.25f, o.y - o.radius * 0.28f, o.radius * 0.55f, fill)
        line.color = base
        line.strokeWidth = 3f
        canvas.drawLine(o.x - o.radius * 0.1f, o.y - o.radius * 0.1f, o.x + o.radius * 0.5f, o.y + o.radius * 0.45f, line)
    }

    // ---- Dust Wasteland

    private fun drawCactus(canvas: Canvas, o: Obstacle) {
        fill.color = colors.propCactus
        rect.set(o.x - 7f, o.y - 20f, o.x + 7f, o.y + 20f)
        canvas.drawRoundRect(rect, 7f, 7f, fill)
        rect.set(o.x - 19f, o.y - 6f, o.x - 4f, o.y + 1f)
        canvas.drawRoundRect(rect, 3f, 3f, fill)
        rect.set(o.x - 19f, o.y - 15f, o.x - 12f, o.y + 1f)
        canvas.drawRoundRect(rect, 3f, 3f, fill)
        rect.set(o.x + 4f, o.y - 12f, o.x + 19f, o.y - 5f)
        canvas.drawRoundRect(rect, 3f, 3f, fill)
        rect.set(o.x + 12f, o.y - 21f, o.x + 19f, o.y - 5f)
        canvas.drawRoundRect(rect, 3f, 3f, fill)
        line.color = colors.propCactusLight
        line.strokeWidth = 2f
        canvas.drawLine(o.x - 2f, o.y - 15f, o.x - 2f, o.y + 14f, line)
    }

    // ---- Frozen Tundra

    private fun drawIce(canvas: Canvas, o: Obstacle) {
        fill.color = colors.propIce
        canvas.drawCircle(o.x, o.y, o.radius, fill)
        line.color = colors.propIceDark
        line.strokeWidth = 3f
        for (k in 0 until 4) {
            val a = o.angle + k * 1.57f
            canvas.drawLine(o.x, o.y, o.x + cos(a) * o.radius * 0.95f, o.y + sin(a) * o.radius * 0.95f, line)
        }
        fill.color = colors.propIceLight
        canvas.drawCircle(o.x - o.radius * 0.28f, o.y - o.radius * 0.3f, o.radius * 0.4f, fill)
    }

    private fun drawPine(canvas: Canvas, o: Obstacle) {
        fill.color = colors.propPine
        canvas.drawCircle(o.x, o.y, o.radius, fill)
        fill.color = colors.propPineLight
        canvas.drawCircle(o.x, o.y, o.radius * 0.72f, fill)
        fill.color = colors.propSnowCap
        canvas.drawCircle(o.x - 3f, o.y - 3f, o.radius * 0.4f, fill)
    }

    // ---- Haunted Graveyard

    private fun drawTombstone(canvas: Canvas, o: Obstacle) {
        fill.color = colors.propTomb
        rect.set(o.x - 15f, o.y - 20f, o.x + 15f, o.y + 18f)
        canvas.drawRoundRect(rect, 10f, 10f, fill)
        fill.color = colors.propTombLight
        rect.set(o.x - 15f, o.y - 20f, o.x - 5f, o.y + 18f)
        canvas.drawRoundRect(rect, 8f, 8f, fill)
        line.color = colors.propTombDark
        line.strokeWidth = 3f
        canvas.drawLine(o.x, o.y - 12f, o.x, o.y + 4f, line)
        canvas.drawLine(o.x - 6f, o.y - 6f, o.x + 6f, o.y - 6f, line)
    }

    private fun drawCrypt(canvas: Canvas, o: Obstacle) {
        val r = o.radius
        fill.color = colors.propCrypt
        rect.set(o.x - r, o.y - r * 0.8f, o.x + r, o.y + r * 0.8f)
        canvas.drawRoundRect(rect, 6f, 6f, fill)
        fill.color = colors.propCryptRoof
        rect.set(o.x - r * 1.08f, o.y - r * 0.95f, o.x + r * 1.08f, o.y - r * 0.3f)
        canvas.drawRoundRect(rect, 6f, 6f, fill)
        fill.color = colors.propTombDark
        rect.set(o.x - 11f, o.y + 2f, o.x + 11f, o.y + r * 0.8f)
        canvas.drawRoundRect(rect, 6f, 6f, fill)
    }

    private fun drawDeadTree(canvas: Canvas, o: Obstacle) {
        line.color = colors.propDeadTree
        line.strokeWidth = 6f
        for (k in 0 until 5) {
            val a = o.angle + k * 1.25f
            val ex = o.x + cos(a) * o.radius * 1.4f
            val ey = o.y + sin(a) * o.radius * 1.4f
            canvas.drawLine(o.x, o.y, ex, ey, line)
            canvas.drawLine(o.x + cos(a) * o.radius * 0.8f, o.y + sin(a) * o.radius * 0.8f,
                ex + cos(a + 0.7f) * 12f, ey + sin(a + 0.7f) * 12f, line)
        }
        fill.color = colors.propDeadTree
        canvas.drawCircle(o.x, o.y, o.radius * 0.4f, fill)
    }
}
