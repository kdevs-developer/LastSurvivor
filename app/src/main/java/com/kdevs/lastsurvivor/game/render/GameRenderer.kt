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
import com.kdevs.lastsurvivor.game.entities.ExplosionEffect
import com.kdevs.lastsurvivor.game.entities.Grenade
import com.kdevs.lastsurvivor.game.entities.ImpactEffect
import com.kdevs.lastsurvivor.game.entities.PowerUp
import com.kdevs.lastsurvivor.game.entities.PowerUpType
import com.kdevs.lastsurvivor.game.entities.Zombie
import com.kdevs.lastsurvivor.game.entities.ZombieState
import com.kdevs.lastsurvivor.game.entities.ZombieType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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
    private val reloadBackPaint = Paint().apply { color = colors.reloadBarBack }
    private val reloadFillPaint = Paint().apply { color = colors.reloadBarFill }

    // Phase 8: power-ups, grenades, explosions, shield / damage auras, boss warnings
    private val powerUpPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerUpRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.powerUpRing
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.powerUpSymbol
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val symbolFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.powerUpSymbol }
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.shieldBubble }
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.damageAura }
    private val grenadeBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.grenadeBody }
    private val grenadeFlashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.grenadeFlash }
    private val explosionFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.explosionFill }
    private val explosionRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.explosionRing
        style = Paint.Style.STROKE
    }
    private val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.bossWarning
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val enragedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.bossEnraged }

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
    private val grenadeText = resources.getString(R.string.hud_grenade)

    fun render(canvas: Canvas, world: GameWorld, fps: Int, paused: Boolean) {
        canvas.drawColor(colors.clear)

        canvas.save()
        canvas.translate(-world.camera.x, -world.camera.y)

        // floor + map border
        canvas.drawRect(0f, 0f, GameWorld.WIDTH, GameWorld.HEIGHT, floorPaint)
        canvas.drawRect(0f, 0f, GameWorld.WIDTH, GameWorld.HEIGHT, boundsPaint)

        drawPowerUps(canvas, world)
        drawBossWarnings(canvas, world)
        drawZombies(canvas, world)
        drawGrenades(canvas, world)
        drawBullets(canvas, world)   // before the player: bullets appear to leave the barrel
        drawPlayer(canvas, world)
        drawExplosions(canvas, world)
        drawImpacts(canvas, world)
        drawReloadBar(canvas, world)

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
        if (p.damageBoost) canvas.drawCircle(p.x, p.y, p.radius + 10f, auraPaint)
        val half = playerSprite.width / 2f
        matrix.reset()
        matrix.postRotate(Math.toDegrees(p.rotation.toDouble()).toFloat(), half, half)
        matrix.postTranslate(p.x - half, p.y - half)
        canvas.drawBitmap(playerSprite, matrix, spritePaint)
        // shield bubble; it blinks during its last two seconds
        if (p.shielded && (p.shieldTimer > 2f || ((p.shieldTimer * 8f).toInt() and 1) == 0)) {
            canvas.drawCircle(p.x, p.y, p.radius + 16f, bubblePaint)
            canvas.drawCircle(p.x, p.y, p.radius + 16f, powerUpRingPaint)
        }

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

    /** Thin progress bar over the player's head while the weapon reloads. */
    private fun drawReloadBar(canvas: Canvas, world: GameWorld) {
        val w = world.weapon
        if (!w.reloading) return
        val p = world.player
        val half = 30f
        val y = p.y - p.radius - 24f
        canvas.drawRect(p.x - half, y, p.x + half, y + 6f, reloadBackPaint)
        canvas.drawRect(p.x - half, y, p.x - half + 2f * half * w.reloadProgress, y + 6f, reloadFillPaint)
    }

    /** Pickups: a coloured disc with a white symbol, bobbing, blinking when about to expire. */
    private fun drawPowerUps(canvas: Canvas, world: GameWorld) {
        val margin = 80f
        val left = world.camera.x - margin
        val top = world.camera.y - margin
        val right = world.camera.x + world.viewportWidth + margin
        val bottom = world.camera.y + world.viewportHeight + margin

        val list = world.powerUps
        var i = 0
        while (i < list.size) {
            val u: PowerUp = list[i]
            i++
            if (u.x < left || u.x > right || u.y < top || u.y > bottom) continue
            if (u.expiring && ((u.age * 8f).toInt() and 1) == 0) continue
            val cy = u.y + sin(u.age * 4f) * 4f
            val r = u.radius
            canvas.drawCircle(u.x + 3f, cy + 6f, r, shadowPaint)
            canvas.drawCircle(u.x, cy, r + 5f + 3f * sin(u.age * 6f), powerUpRingPaint)
            powerUpPaint.color = colors.powerUp[u.type.ordinal]
            canvas.drawCircle(u.x, cy, r, powerUpPaint)
            drawPowerUpSymbol(canvas, u.type, u.x, cy, r)
        }
    }

    /** Symbols are drawn from lines and circles so they need no bitmaps. */
    private fun drawPowerUpSymbol(canvas: Canvas, type: PowerUpType, cx: Float, cy: Float, r: Float) {
        val s = r * 0.5f
        when (type) {
            PowerUpType.MEDKIT -> {
                canvas.drawLine(cx - s, cy, cx + s, cy, symbolPaint)
                canvas.drawLine(cx, cy - s, cx, cy + s, symbolPaint)
            }
            PowerUpType.RAPID_FIRE -> {
                // two chevrons: ">>"
                canvas.drawLine(cx - s, cy - s, cx - s * 0.1f, cy, symbolPaint)
                canvas.drawLine(cx - s * 0.1f, cy, cx - s, cy + s, symbolPaint)
                canvas.drawLine(cx + s * 0.1f, cy - s, cx + s, cy, symbolPaint)
                canvas.drawLine(cx + s, cy, cx + s * 0.1f, cy + s, symbolPaint)
            }
            PowerUpType.DOUBLE_DAMAGE -> {
                // crosshair
                canvas.drawCircle(cx, cy, s, symbolPaint)
                canvas.drawCircle(cx, cy, 2.5f, symbolFillPaint)
                canvas.drawLine(cx - s * 1.4f, cy, cx - s * 0.6f, cy, symbolPaint)
                canvas.drawLine(cx + s * 0.6f, cy, cx + s * 1.4f, cy, symbolPaint)
            }
            PowerUpType.SHIELD -> {
                canvas.drawLine(cx - s, cy - s, cx + s, cy - s, symbolPaint)
                canvas.drawLine(cx + s, cy - s, cx + s, cy, symbolPaint)
                canvas.drawLine(cx + s, cy, cx, cy + s * 1.2f, symbolPaint)
                canvas.drawLine(cx, cy + s * 1.2f, cx - s, cy, symbolPaint)
                canvas.drawLine(cx - s, cy, cx - s, cy - s, symbolPaint)
            }
            PowerUpType.GRENADE -> {
                canvas.drawCircle(cx, cy + s * 0.2f, s * 0.9f, symbolFillPaint)
                canvas.drawLine(cx, cy - s * 0.7f, cx + s * 0.5f, cy - s * 1.2f, symbolPaint)
            }
        }
    }

    private fun drawGrenades(canvas: Canvas, world: GameWorld) {
        val list = world.grenades
        var i = 0
        while (i < list.size) {
            val g: Grenade = list[i]
            i++
            canvas.drawCircle(g.x + 3f, g.y + 5f, g.radius, shadowPaint)
            canvas.drawCircle(g.x, g.y, g.radius, grenadeBodyPaint)
            // the warning light blinks faster as the fuse runs out
            if (((g.age * (6f + 14f * g.fuseProgress)).toInt() and 1) == 0) {
                canvas.drawCircle(g.x, g.y, g.radius * 0.5f, grenadeFlashPaint)
            }
        }
    }

    private fun drawExplosions(canvas: Canvas, world: GameWorld) {
        val list = world.explosions
        var i = 0
        while (i < list.size) {
            val e: ExplosionEffect = list[i]
            i++
            val t = e.progress
            val grow = 1f - (1f - t) * (1f - t) // fast start, slow finish
            explosionFillPaint.alpha = (170 * (1f - t)).toInt()
            canvas.drawCircle(e.x, e.y, e.radius * (0.25f + 0.75f * grow), explosionFillPaint)
            explosionRingPaint.alpha = (255 * (1f - t)).toInt()
            explosionRingPaint.strokeWidth = 4f + 10f * (1f - t)
            canvas.drawCircle(e.x, e.y, e.radius * grow, explosionRingPaint)
        }
    }

    /** Red strip from a winding-up Brute towards the player: step out of it before it charges. */
    private fun drawBossWarnings(canvas: Canvas, world: GameWorld) {
        val boss = world.boss ?: return
        if (boss.windup <= 0f) return
        val dx = world.player.x - boss.x
        val dy = world.player.y - boss.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1f) return
        val nx = dx / len
        val ny = dy / len
        val reach = 900f
        warningPaint.strokeWidth = boss.radius * 1.5f
        warningPaint.alpha = 50 + (50 * (0.5f + 0.5f * sin(boss.windup * 30f))).toInt()
        canvas.drawLine(boss.x, boss.y, boss.x + nx * reach, boss.y + ny * reach, warningPaint)
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
        if (z.enraged) canvas.drawCircle(z.x, z.y, z.radius + 14f, enragedPaint)
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
        if (pl.grenadeFlash > 0f) { canvas.drawText(grenadeText, 24f, line, debugPaint) }
    }

    private companion object {
        const val TRACER_LENGTH = 32f
    }
}
