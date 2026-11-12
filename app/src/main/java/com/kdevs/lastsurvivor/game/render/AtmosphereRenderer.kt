package com.kdevs.lastsurvivor.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.kdevs.lastsurvivor.game.engine.GameArea
import com.kdevs.lastsurvivor.game.engine.GameConfig
import com.kdevs.lastsurvivor.game.engine.GameWorld
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Weather and visibility, drawn on the screen (not the map) after the world:
 *
 *  - Ruined City: rain streaks
 *  - Toxic Swamp: drifting spores, a green haze and a dim edge
 *  - Dust Wasteland: dust-storm streaks and a warm haze that gusts
 *  - Frozen Tundra: snowfall and a blizzard haze that gusts
 *  - Haunted Graveyard: fog banks, near-darkness around a flickering light, and lightning
 *
 * Allocation-free once running: particles live in fixed arrays and the gradients are built only when the
 * area or the screen size changes. Game thread only.
 */
class AtmosphereRenderer(private val colors: GameColors) {

    private val px = FloatArray(MAX)
    private val py = FloatArray(MAX)
    private val vx = FloatArray(MAX)
    private val vy = FloatArray(MAX)
    private val size = FloatArray(MAX)
    private val phase = FloatArray(MAX)

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val hazePaint = Paint()
    private val darkPaint = Paint()
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flashPaint = Paint().apply { color = colors.fxLightning }
    private val rnd = Random(11)

    private var seededArea: GameArea? = null
    private var seededW = -1
    private var seededH = -1
    private var lastTime = 0f
    private var darkArea: GameArea? = null
    private var darkW = -1
    private var darkH = -1

    fun draw(canvas: Canvas, world: GameWorld) {
        val area = world.area
        val w = canvas.width
        val h = canvas.height
        if (area != seededArea || w != seededW || h != seededH) seed(area, w, h)

        val dt = (world.time - lastTime).coerceIn(0f, 0.05f)
        lastTime = world.time

        drawParticles(canvas, world, area, dt, w, h)
        drawHaze(canvas, world.time, area, w, h)
        drawDarkness(canvas, world, area, w, h)
        if (area == GameArea.GRAVEYARD) drawLightning(canvas, world.time, w, h)
    }

    // ------------------------------------------------------------------ particles

    private fun countFor(area: GameArea): Int = when (area) {
        GameArea.CITY -> 90
        GameArea.SWAMP -> 45
        GameArea.DESERT -> 55
        GameArea.TUNDRA -> 110
        GameArea.GRAVEYARD -> 8
    }

    private fun seed(area: GameArea, w: Int, h: Int) {
        seededArea = area
        seededW = w
        seededH = h
        for (i in 0 until MAX) {
            px[i] = rnd.nextFloat() * w
            py[i] = rnd.nextFloat() * h
            phase[i] = rnd.nextFloat() * 6.2832f
            when (area) {
                GameArea.CITY -> { vx[i] = -90f; vy[i] = 900f + rnd.nextFloat() * 300f; size[i] = 1f }
                GameArea.SWAMP -> { vx[i] = 0f; vy[i] = -(14f + rnd.nextFloat() * 26f); size[i] = 2f + rnd.nextFloat() * 3f }
                GameArea.DESERT -> { vx[i] = 700f + rnd.nextFloat() * 400f; vy[i] = 40f + rnd.nextFloat() * 50f; size[i] = 1f }
                GameArea.TUNDRA -> { vx[i] = -40f; vy[i] = 70f + rnd.nextFloat() * 70f; size[i] = 2f + rnd.nextFloat() * 2.5f }
                GameArea.GRAVEYARD -> { vx[i] = 12f + rnd.nextFloat() * 14f; vy[i] = (rnd.nextFloat() - 0.5f) * 6f; size[i] = 1.8f + rnd.nextFloat() * 1.6f }
            }
        }
    }

    private fun drawParticles(canvas: Canvas, world: GameWorld, area: GameArea, dt: Float, w: Int, h: Int) {
        val n = countFor(area)
        val time = world.time
        val fw = w.toFloat()
        val fh = h.toFloat()
        // move and wrap
        for (i in 0 until n) {
            var drift = 0f
            if (area == GameArea.TUNDRA || area == GameArea.SWAMP) drift = sin(time * 0.9f + phase[i]) * 35f
            px[i] += (vx[i] + drift) * dt
            py[i] += vy[i] * dt
            val margin = if (area == GameArea.GRAVEYARD) 340f else 60f
            if (px[i] > fw + margin) px[i] = -margin
            if (px[i] < -margin) px[i] = fw + margin
            if (py[i] > fh + margin) py[i] = -margin
            if (py[i] < -margin) py[i] = fh + margin
        }
        when (area) {
            GameArea.CITY -> streaks(canvas, n, colors.fxRain, 2f, 0.02f)
            GameArea.DESERT -> streaks(canvas, n, colors.fxDust, 3f, 0.05f)
            GameArea.SWAMP -> dots(canvas, n, colors.fxSpore)
            GameArea.TUNDRA -> dots(canvas, n, colors.fxSnow)
            GameArea.GRAVEYARD -> fog(canvas, n, w, h)
        }
    }

    private fun streaks(canvas: Canvas, n: Int, color: Int, width: Float, length: Float) {
        particlePaint.style = Paint.Style.STROKE
        particlePaint.strokeWidth = width
        particlePaint.color = color
        for (i in 0 until n) canvas.drawLine(px[i], py[i], px[i] + vx[i] * length, py[i] + vy[i] * length, particlePaint)
    }

    private fun dots(canvas: Canvas, n: Int, color: Int) {
        particlePaint.style = Paint.Style.FILL
        particlePaint.color = color
        for (i in 0 until n) canvas.drawCircle(px[i], py[i], size[i], particlePaint)
    }

    /** Soft fog blobs: one radial gradient (radius 100 at the origin) scaled to each blob's size. */
    private fun fog(canvas: Canvas, n: Int, w: Int, h: Int) {
        if (fogPaint.shader == null) {
            val clear = colors.fxFog and 0x00FFFFFF
            fogPaint.shader = RadialGradient(0f, 0f, 100f, intArrayOf(colors.fxFog, clear), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        }
        val base = min(w, h) / 100f
        for (i in 0 until n) {
            val s = base * (1.5f + size[i])
            canvas.save()
            canvas.translate(px[i], py[i])
            canvas.scale(s, s)
            canvas.drawCircle(0f, 0f, 100f, fogPaint)
            canvas.restore()
        }
    }

    // ------------------------------------------------------------------ haze, darkness, lightning

    private fun drawHaze(canvas: Canvas, time: Float, area: GameArea, w: Int, h: Int) {
        val base = when (area) {
            GameArea.SWAMP -> colors.fxHazeSwamp
            GameArea.DESERT -> colors.fxHazeDesert
            GameArea.TUNDRA -> colors.fxHazeTundra
            else -> return
        }
        // gusts: the haze thickens and thins
        val gust = if (area == GameArea.SWAMP) 0.85f + 0.15f * sin(time * 0.6f) else 0.65f + 0.35f * sin(time * 0.8f)
        val alpha = ((base ushr 24) * gust).toInt().coerceIn(0, 255)
        hazePaint.color = (base and 0x00FFFFFF) or (alpha shl 24)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), hazePaint)
    }

    /** The graveyard is nearly dark outside a flickering light around the player; the swamp is only dim at the edges. */
    private fun drawDarkness(canvas: Canvas, world: GameWorld, area: GameArea, w: Int, h: Int) {
        val edge = when (area) {
            GameArea.GRAVEYARD -> colors.fxNight
            GameArea.SWAMP -> colors.fxSwampDark
            else -> return
        }
        if (area != darkArea || w != darkW || h != darkH) {
            darkArea = area
            darkW = w
            darkH = h
            val radius = if (area == GameArea.GRAVEYARD) min(w, h) * 0.5f else max(w, h) * 0.55f
            val clear = edge and 0x00FFFFFF
            darkPaint.shader = RadialGradient(0f, 0f, radius, intArrayOf(clear, clear, edge), floatArrayOf(0f, 0.38f, 1f), Shader.TileMode.CLAMP)
        }
        val sx = world.player.x - world.camera.x + world.shakeX
        val sy = world.player.y - world.camera.y + world.shakeY
        val flicker = if (area == GameArea.GRAVEYARD) 1f + 0.025f * sin(world.time * 9f) + 0.015f * sin(world.time * 23f) else 1f
        val big = max(w, h) * 3f
        canvas.save()
        canvas.translate(sx, sy)
        canvas.scale(flicker, flicker)
        canvas.drawRect(-big, -big, big, big, darkPaint)
        canvas.restore()
    }

    /** Two quick flashes every ten seconds. */
    private fun drawLightning(canvas: Canvas, time: Float, w: Int, h: Int) {
        val t = time % GameConfig.LIGHTNING_PERIOD
        val strength = when {
            t < 0.10f -> 0.5f * (1f - t / 0.10f)
            t in 0.26f..0.36f -> 0.35f * (1f - (t - 0.26f) / 0.10f)
            else -> return
        }
        flashPaint.alpha = (255 * strength).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), flashPaint)
    }

    private companion object {
        const val MAX = 110
    }
}
