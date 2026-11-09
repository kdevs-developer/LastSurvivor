package com.kdevs.lastsurvivor.game.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import com.kdevs.lastsurvivor.game.engine.GameWorld
import com.kdevs.lastsurvivor.game.entities.Zombie
import com.kdevs.lastsurvivor.game.entities.ZombieState
import com.kdevs.lastsurvivor.game.entities.ZombieType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a GameWorld onto a Canvas. Allocation-free in render(): every Paint, Matrix and
 * Bitmap is created once up front. Runs on the game thread only.
 */
class GameRenderer {

    private val floorTile: Bitmap = PlaceholderSprites.floorTile(128)
    private val playerSprite: Bitmap = PlaceholderSprites.player(96)

    // Placeholder zombie colours, indexed by ZombieType.ordinal: WALKER, RUNNER, TANK, TOXIC
    private val skinColors = intArrayOf(0xFF7FA06A.toInt(), 0xFFB08A6A.toInt(), 0xFF6E7F8E.toInt(), 0xFFB5E04A.toInt())
    private val clothColors = intArrayOf(0xFF4A4F63.toInt(), 0xFF7A3B3B.toInt(), 0xFF2E3A2E.toInt(), 0xFF3F5F2A.toInt())
    private val glowColors = intArrayOf(0, 0, 0, 0x5589C93A.toInt())

    /** zombieSprites[type.ordinal][frame]: frame 0/1 walk, 2 attack. */
    private val zombieSprites: Array<Array<Bitmap>> = Array(ZombieType.entries.size) { t ->
        val type = ZombieType.entries[t]
        val size = (type.radius * 3.2f).toInt()
        Array(3) { frame -> PlaceholderSprites.zombie(size, skinColors[t], clothColors[t], glowColors[t], frame) }
    }

    private val floorPaint = Paint().apply {
        shader = BitmapShader(floorTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }
    private val spritePaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000 }
    private val boundsPaint = Paint().apply {
        color = 0xFFC62828.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
    }
    private val pauseDim = Paint().apply { color = 0x88000000.toInt() }
    private val aimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x88FF3B30.toInt()
        strokeWidth = 3f
    }
    private val firePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFC107.toInt() }
    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFC107.toInt()
        textSize = 24f
    }
    private val barBackPaint = Paint().apply { color = 0xAA000000.toInt() }
    private val barFillPaint = Paint().apply { color = 0xFFE53935.toInt() }

    private val matrix = Matrix()
    private var lastFps = -1
    private var fpsText = ""
    private var lastAlive = -1
    private var lastSwings = -1
    private var zombieText = ""

    fun render(canvas: Canvas, world: GameWorld, fps: Int, paused: Boolean) {
        canvas.drawColor(0xFF0B0B0D.toInt())

        canvas.save()
        canvas.translate(-world.camera.x, -world.camera.y)

        // floor + map border
        canvas.drawRect(0f, 0f, GameWorld.WIDTH, GameWorld.HEIGHT, floorPaint)
        canvas.drawRect(0f, 0f, GameWorld.WIDTH, GameWorld.HEIGHT, boundsPaint)

        drawZombies(canvas, world)
        drawPlayer(canvas, world)

        canvas.restore()

        drawDebugOverlay(canvas, world, fps)

        if (paused) {
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), pauseDim)
        }
    }

    private fun drawPlayer(canvas: Canvas, world: GameWorld) {
        val p = world.player
        canvas.drawCircle(p.x + 6f, p.y + 8f, p.radius, shadowPaint)
        val half = playerSprite.width / 2f
        matrix.reset()
        matrix.postRotate(Math.toDegrees(p.rotation.toDouble()).toFloat(), half, half)
        matrix.postTranslate(p.x - half, p.y - half)
        canvas.drawBitmap(playerSprite, matrix, spritePaint)

        // aim laser from the muzzle while the aim stick is held
        val cosR = cos(p.rotation)
        val sinR = sin(p.rotation)
        if (p.aiming) {
            canvas.drawLine(
                p.x + cosR * 40f, p.y + sinR * 40f,
                p.x + cosR * 420f, p.y + sinR * 420f, aimPaint
            )
        }
        // placeholder muzzle flash while firing (real bullets arrive in Phase 5)
        if (p.firing) {
            canvas.drawCircle(p.x + cosR * 52f, p.y + sinR * 52f, 9f, firePaint)
        }
    }

    private fun drawZombies(canvas: Canvas, world: GameWorld) {
        val margin = 120f
        val left = world.camera.x - margin
        val top = world.camera.y - margin
        val right = world.camera.x + world.viewportWidth + margin
        val bottom = world.camera.y + world.viewportHeight + margin

        val list = world.zombies
        var i = 0
        while (i < list.size) {
            val z = list[i]
            i++
            if (z.x < left || z.x > right || z.y < top || z.y > bottom) continue
            drawZombie(canvas, z)
        }
    }

    private fun drawZombie(canvas: Canvas, z: Zombie) {
        val frame = when (z.state) {
            ZombieState.ATTACK -> 2
            ZombieState.WALK -> (z.animTime * 3f).toInt() and 1
            else -> 0
        }
        val bmp = zombieSprites[z.type.ordinal][frame]
        val half = bmp.width / 2f

        val sway = if (z.state == ZombieState.WALK) sin(z.animTime * 9f) * 5f else 0f
        val deg = Math.toDegrees(z.rotation.toDouble()).toFloat() + sway

        matrix.reset()
        matrix.postRotate(deg, half, half)
        matrix.postTranslate(z.x - half, z.y - half)

        if (z.state == ZombieState.DEAD) {
            // death animation: shrink and fade out
            val t = (z.deathTimer / Zombie.DEATH_SECONDS).coerceIn(0f, 1f)
            matrix.postScale(0.7f + 0.3f * t, 0.7f + 0.3f * t, z.x, z.y)
            spritePaint.alpha = (255 * t).toInt()
            canvas.drawBitmap(bmp, matrix, spritePaint)
            spritePaint.alpha = 255
            return
        }

        canvas.drawCircle(z.x + 4f, z.y + 6f, z.radius, shadowPaint)
        canvas.drawBitmap(bmp, matrix, spritePaint)

        // health bar once damaged
        if (z.health < z.maxHealth) {
            val w = z.radius * 1.6f
            val x0 = z.x - w / 2f
            val y0 = z.y - z.radius - 14f
            canvas.drawRect(x0, y0, x0 + w, y0 + 5f, barBackPaint)
            canvas.drawRect(x0, y0, x0 + w * (z.health / z.maxHealth), y0 + 5f, barFillPaint)
        }
    }

    /** Screen-space debug text. Strings are rebuilt only when their values change. */
    private fun drawDebugOverlay(canvas: Canvas, world: GameWorld, fps: Int) {
        if (fps != lastFps) {
            lastFps = fps
            fpsText = "FPS $fps"
        }
        canvas.drawText(fpsText, 24f, 44f, hudPaint)

        if (world.aliveZombies != lastAlive || world.zombieSwings != lastSwings) {
            lastAlive = world.aliveZombies
            lastSwings = world.zombieSwings
            zombieText = "Zombies $lastAlive   swings landed $lastSwings (damage in Phase 6)"
        }
        var line = 74f
        canvas.drawText(zombieText, 24f, line, debugPaint)
        line += 28f

        val pl = world.player
        if (pl.sprinting && pl.moving) { canvas.drawText("SPRINT", 24f, line, debugPaint); line += 28f }
        if (pl.firing) { canvas.drawText("FIRING (bullets in Phase 5)", 24f, line, debugPaint); line += 28f }
        if (pl.reloadFlash > 0f) { canvas.drawText("RELOAD pressed (Phase 7)", 24f, line, debugPaint); line += 28f }
        if (pl.grenadeFlash > 0f) { canvas.drawText("GRENADE: temporary 260-unit blast for testing", 24f, line, debugPaint) }
    }
}
