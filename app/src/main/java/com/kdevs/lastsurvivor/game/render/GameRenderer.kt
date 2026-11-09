package com.kdevs.lastsurvivor.game.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.engine.GameConfig
import com.kdevs.lastsurvivor.game.engine.GameWorld
import com.kdevs.lastsurvivor.game.entities.Bullet
import com.kdevs.lastsurvivor.game.entities.ImpactEffect
import com.kdevs.lastsurvivor.game.entities.Zombie
import com.kdevs.lastsurvivor.game.entities.ZombieState
import com.kdevs.lastsurvivor.game.entities.ZombieType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws a GameWorld onto a Canvas. Allocation-free in render(): every Paint, Matrix and
 * Bitmap is created once up front. Colours come from res/values/colors.xml via [GameColors]
 * and HUD text from res/values/strings_hud.xml. Runs on the game thread only.
 */
class GameRenderer(context: Context) {

    private val resources = context.resources
    private val colors = GameColors(context)

    private val floorTile: Bitmap = PlaceholderSprites.floorTile(128, colors)
    private val playerSprite: Bitmap = PlaceholderSprites.player(96, colors)

    /** zombieSprites[type.ordinal][frame]: frame 0/1 walk, 2 attack. */
    private val zombieSprites: Array<Array<Bitmap>> = Array(ZombieType.entries.size) { t ->
        val type = ZombieType.entries[t]
        val size = (type.radius * 3.2f).toInt()
        Array(3) { frame ->
            PlaceholderSprites.zombie(size, colors.zombieSkin[t], colors.zombieCloth[t], colors.zombieGlow[t], frame, colors)
        }
    }

    private val floorPaint = Paint().apply {
        shader = BitmapShader(floorTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }
    private val spritePaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.shadow }
    private val boundsPaint = Paint().apply {
        color = colors.border
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.hudText
        textSize = 28f
    }
    private val pauseDim = Paint().apply { color = colors.pauseDim }
    private val aimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.aimLaser
        strokeWidth = 3f
    }
    private val firePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.muzzleFlash }
    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.hudDebugText
        textSize = 24f
    }
    private val barBackPaint = Paint().apply { color = colors.zombieHealthBack }
    private val barFillPaint = Paint().apply { color = colors.zombieHealthFill }
    private val tracerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.bulletTracer
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }
    private val bulletCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.bulletCore }
    private val impactPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint()

    private val matrix = Matrix()

    // HUD strings are rebuilt only when the numbers behind them change.
    private var lastFps = -1
    private var fpsText = ""
    private var lastAlive = -1
    private var zombieText = ""
    private var lastShots = -1
    private var lastHits = -1
    private var shotsText = ""
    private val sprintText = resources.getString(R.string.hud_sprint)
    private val reloadText = resources.getString(R.string.hud_reload)
    private val grenadeText = resources.getString(R.string.hud_grenade)

    fun render(canvas: Canvas, world: GameWorld, fps: Int, paused: Boolean) {
        canvas.drawColor(colors.clear)

        canvas.save()
        canvas.translate(-world.camera.x, -world.camera.y)

        // floor + map border
        canvas.drawRect(0f, 0f, GameWorld.WIDTH, GameWorld.HEIGHT, floorPaint)
        canvas.drawRect(0f, 0f, GameWorld.WIDTH, GameWorld.HEIGHT, boundsPaint)

        drawZombies(canvas, world)
        drawBullets(canvas, world)   // before the player: bullets appear to leave the barrel
        drawPlayer(canvas, world)
        drawImpacts(canvas, world)

        canvas.restore()

        drawDamageFeedback(canvas, world)
        if (GameConfig.SHOW_DEBUG_OVERLAY) drawDebugOverlay(canvas, world, fps)

        if (paused) {
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), pauseDim)
        }
    }

    /** Full-screen red flash when the player is hit and a green tint while poisoned. */
    private fun drawDamageFeedback(canvas: Canvas, world: GameWorld) {
        val p = world.player
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        if (p.poisoned) {
            overlayPaint.color = colors.poisonTint
            overlayPaint.alpha = 48
            canvas.drawRect(0f, 0f, w, h, overlayPaint)
        }
        if (p.hurtFlash > 0f) {
            overlayPaint.color = colors.damageFlash
            overlayPaint.alpha = (120 * (p.hurtFlash / GameConfig.HURT_FLASH_SECONDS)).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, overlayPaint)
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

        val cosR = cos(p.rotation)
        val sinR = sin(p.rotation)
        // aim laser from the muzzle while the aim stick is held
        if (p.aiming) {
            canvas.drawLine(
                p.x + cosR * 40f, p.y + sinR * 40f,
                p.x + cosR * 420f, p.y + sinR * 420f, aimPaint
            )
        }
        // muzzle flash for a few frames after each real shot
        if (p.muzzleFlash > 0f) {
            canvas.drawCircle(p.x + cosR * 52f, p.y + sinR * 52f, 9f, firePaint)
        }
    }

    private fun drawBullets(canvas: Canvas, world: GameWorld) {
        val left = world.camera.x - TRACER_LENGTH
        val top = world.camera.y - TRACER_LENGTH
        val right = world.camera.x + world.viewportWidth + TRACER_LENGTH
        val bottom = world.camera.y + world.viewportHeight + TRACER_LENGTH

        val list = world.bullets
        var i = 0
        while (i < list.size) {
            val b: Bullet = list[i]
            i++
            if (b.x < left || b.x > right || b.y < top || b.y > bottom) continue
            val tail = min(b.travelled, TRACER_LENGTH)
            canvas.drawLine(b.x - b.dirX * tail, b.y - b.dirY * tail, b.x, b.y, tracerPaint)
            canvas.drawCircle(b.x, b.y, b.radius, bulletCorePaint)
        }
    }

    private fun drawImpacts(canvas: Canvas, world: GameWorld) {
        val list = world.impacts
        var i = 0
        while (i < list.size) {
            val e: ImpactEffect = list[i]
            i++
            val t = e.progress
            impactPaint.color = if (e.onZombie) colors.impactBlood else colors.impactDust
            impactPaint.alpha = (200 * (1f - t)).toInt()
            canvas.drawCircle(e.x, e.y, 6f + 16f * t, impactPaint)
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
            fpsText = resources.getString(R.string.hud_fps, fps)
        }
        canvas.drawText(fpsText, 24f, 44f, hudPaint)

        if (world.aliveZombies != lastAlive) {
            lastAlive = world.aliveZombies
            zombieText = resources.getString(R.string.hud_debug_zombies_format, lastAlive)
        }
        var line = 74f
        canvas.drawText(zombieText, 24f, line, debugPaint)
        line += 28f

        if (world.shotsFired != lastShots || world.shotsHit != lastHits) {
            lastShots = world.shotsFired
            lastHits = world.shotsHit
            val accuracy = if (lastShots > 0) lastHits * 100 / lastShots else 0
            shotsText = resources.getString(R.string.hud_shots_format, lastShots, lastHits, accuracy)
        }
        canvas.drawText(shotsText, 24f, line, debugPaint)
        line += 28f

        val pl = world.player
        if (pl.sprinting && pl.moving) { canvas.drawText(sprintText, 24f, line, debugPaint); line += 28f }
        if (pl.reloadFlash > 0f) { canvas.drawText(reloadText, 24f, line, debugPaint); line += 28f }
        if (pl.grenadeFlash > 0f) { canvas.drawText(grenadeText, 24f, line, debugPaint) }
    }

    private companion object {
        const val TRACER_LENGTH = 32f
    }
}
