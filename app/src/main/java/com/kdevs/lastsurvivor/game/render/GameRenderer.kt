package com.kdevs.lastsurvivor.game.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.engine.GameConfig
import com.kdevs.lastsurvivor.game.engine.GameArea
import com.kdevs.lastsurvivor.game.engine.GameWorld
import com.kdevs.lastsurvivor.game.effects.FloatingTexts
import com.kdevs.lastsurvivor.game.effects.ParticleSystem
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
import kotlin.random.Random

/**
 * Draws a GameWorld onto a Canvas. Allocation-free in render(): every Paint, Matrix and
 * Bitmap is created once up front. Colours come from res/values/colors.xml via [GameColors]
 * and HUD text from res/values/strings_hud.xml. Runs on the game thread only.
 */
class GameRenderer(context: Context) {

    private val resources = context.resources
    private val colors = GameColors(context)

    /** All bitmaps: PNGs from assets/sprites/ where present, generated art otherwise. */
    private val sprites = SpriteAtlas(context, colors)

    private val floorPaint = Paint().apply {
        shader = BitmapShader(sprites.floorTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
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

    // Phase 9: particles, damage numbers, hit flash, vignette, muzzle flash
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val numberOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        color = colors.damageNumberOutline
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeJoin = Paint.Join.ROUND
    }
    private val numberStrings = Array(1000) { it.toString() }
    /** Draws a sprite tinted solid white (the colour filter keeps the sprite's silhouette). */
    private val flashPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(colors.hitFlash, PorterDuff.Mode.SRC_ATOP)
    }
    private val vignettePaint = Paint()
    private val lowHealthPaint = Paint()
    private var vignetteW = 0
    private var vignetteH = 0
    private val muzzleCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.muzzleCore }
    private val muzzleGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.muzzleGlow }
    private val explosionCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.explosionCore }
    private val flamePath = Path()

    // floor decoration: fixed positions from a fixed seed, so the map looks the same every run
    private val decorX = FloatArray(DECOR_COUNT)
    private val decorY = FloatArray(DECOR_COUNT)
    private val decorRot = FloatArray(DECOR_COUNT)
    private val decorScale = FloatArray(DECOR_COUNT)
    private val decorKind = IntArray(DECOR_COUNT)

    init {
        val rnd = Random(7)
        for (i in 0 until DECOR_COUNT) {
            decorX[i] = rnd.nextFloat() * GameWorld.WIDTH
            decorY[i] = rnd.nextFloat() * GameWorld.HEIGHT
            decorRot[i] = rnd.nextFloat() * 360f
            decorScale[i] = 0.8f + rnd.nextFloat() * 0.8f
            decorKind[i] = rnd.nextInt(PlaceholderSprites.DECOR_KINDS)
        }
    }

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

    /** The map theme the floor bitmaps are currently drawn for. */
    private var currentArea: GameArea? = null

    /** Redraws the floor, decoration and border in [area]'s colours. Runs on the game thread, only when the area changes. */
    private fun applyArea(area: GameArea) {
        colors.applyArea(area)
        sprites.rebuildArea()
        floorPaint.shader = BitmapShader(sprites.floorTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        boundsPaint.color = colors.border
        currentArea = area
    }

    fun render(canvas: Canvas, world: GameWorld, fps: Int, paused: Boolean) {
        if (world.area != currentArea) applyArea(world.area)
        canvas.drawColor(colors.clear)
        sprites.applySkin(world.character) // no-op unless the outfit was changed

        canvas.save()
        // the camera position plus this tick's shake offset
        canvas.translate(-world.camera.x + world.shakeX, -world.camera.y + world.shakeY)

        // floor, decoration, stains and map border
        canvas.drawRect(0f, 0f, GameWorld.WIDTH, GameWorld.HEIGHT, floorPaint)
        drawDecor(canvas, world)
        drawDecals(canvas, world)
        canvas.drawRect(0f, 0f, GameWorld.WIDTH, GameWorld.HEIGHT, boundsPaint)

        drawPowerUps(canvas, world)
        drawBossWarnings(canvas, world)
        drawTurrets(canvas, world)
        drawZombies(canvas, world)
        drawGrenades(canvas, world)
        drawBullets(canvas, world)   // before the player: bullets appear to leave the barrel
        drawPlayer(canvas, world)
        drawExplosions(canvas, world)
        drawImpacts(canvas, world)
        drawParticles(canvas, world)
        drawReloadBar(canvas, world)
        if (world.settings.damageNumbers) drawDamageNumbers(canvas, world)

        canvas.restore()

        drawVignette(canvas, world)
        drawDamageFeedback(canvas, world)
        if (world.settings.showFps) drawDebugOverlay(canvas, world, fps)

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
        if (p.adrenaline || p.dashing) canvas.drawCircle(p.x, p.y, p.radius + 8f, powerUpRingPaint)
        val weaponType = world.weapon.stats.type
        val sprite = sprites.playerSprites[weaponType.ordinal][playerFrame(world)]
        val half = sprite.width / 2f
        matrix.reset()
        matrix.postRotate(Math.toDegrees(p.rotation.toDouble()).toFloat(), half, half)
        matrix.postTranslate(p.x - half, p.y - half)
        canvas.drawBitmap(sprite, matrix, spritePaint)
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
        // muzzle flash for a few frames after each real shot: a warm glow, a flame and a white core
        if (p.muzzleFlash > 0f) {
            val k = (p.muzzleFlash / GameConfig.MUZZLE_FLASH_SECONDS).coerceIn(0f, 1f)
            val tip = weaponType.muzzleOffset
            val big = if (weaponType.pellets >= 4) 1.6f else 1f
            val tx = p.x + cosR * tip
            val ty = p.y + sinR * tip
            muzzleGlowPaint.alpha = (110 * k).toInt()
            canvas.drawCircle(tx, ty, 90f * big, muzzleGlowPaint)
            val len = 34f * big * (0.6f + 0.4f * k)
            val wide = 9f * big
            flamePath.reset()
            flamePath.moveTo(tx - sinR * wide, ty + cosR * wide)
            flamePath.lineTo(tx + cosR * len, ty + sinR * len)
            flamePath.lineTo(tx + sinR * wide, ty - cosR * wide)
            flamePath.close()
            canvas.drawPath(flamePath, firePaint)
            canvas.drawCircle(tx, ty, 8f * big, muzzleCorePaint)
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
            PowerUpType.DIAMOND -> {
                // a cut gem: a rhombus with a line across it
                canvas.drawLine(cx, cy - s * 1.2f, cx + s * 1.1f, cy - s * 0.2f, symbolPaint)
                canvas.drawLine(cx + s * 1.1f, cy - s * 0.2f, cx, cy + s * 1.2f, symbolPaint)
                canvas.drawLine(cx, cy + s * 1.2f, cx - s * 1.1f, cy - s * 0.2f, symbolPaint)
                canvas.drawLine(cx - s * 1.1f, cy - s * 0.2f, cx, cy - s * 1.2f, symbolPaint)
                canvas.drawLine(cx - s * 1.1f, cy - s * 0.2f, cx + s * 1.1f, cy - s * 0.2f, symbolPaint)
            }
        }
    }

    /** The Engineer's turret: a round base with a barrel that turns towards its target. Blinks before it expires. */
    private fun drawTurrets(canvas: Canvas, world: GameWorld) {
        val list = world.turrets
        var i = 0
        while (i < list.size) {
            val t = list[i]
            i++
            if (t.expiring && ((t.age * 8f).toInt() and 1) == 0) continue
            val drop = (t.age / 0.25f).coerceIn(0f, 1f) // grows in when placed
            val r = t.radius * (0.5f + 0.5f * drop)
            canvas.drawCircle(t.x + 4f, t.y + 6f, r, shadowPaint)
            powerUpPaint.color = colors.turretBody
            canvas.drawCircle(t.x, t.y, r, powerUpPaint)
            canvas.drawCircle(t.x, t.y, r, powerUpRingPaint)
            symbolPaint.color = colors.turretBarrel
            symbolPaint.strokeWidth = 9f
            canvas.drawLine(t.x, t.y, t.x + cos(t.rotation) * (r + 14f), t.y + sin(t.rotation) * (r + 14f), symbolPaint)
            symbolPaint.color = colors.powerUpSymbol
            symbolPaint.strokeWidth = 5f
            canvas.drawCircle(t.x, t.y, r * 0.35f, symbolFillPaint)
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
            if (t < 0.3f) { // bright white-hot flash at the very start
                val f = 1f - t / 0.3f
                explosionCorePaint.alpha = (255 * f).toInt()
                canvas.drawCircle(e.x, e.y, e.radius * (0.25f + 0.2f * f), explosionCorePaint)
            }
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
            ZombieState.ATTACK -> ATTACK_FRAME
            ZombieState.WALK -> (z.animTime * (3f + z.speed / 40f)).toInt() and 3 // faster zombies step faster
            else -> 0
        }
        val bmp = sprites.zombieSprites[z.type.ordinal][frame]
        val half = bmp.width / 2f

        val sway = if (z.state == ZombieState.WALK) sin(z.animTime * 9f) * 5f else 0f
        val deg = Math.toDegrees(z.rotation.toDouble()).toFloat() + sway

        matrix.reset()
        matrix.postRotate(deg, half, half)
        matrix.postTranslate(z.x - half, z.y - half)

        if (z.state == ZombieState.DEAD) {
            // death animation: collapse with a little twist, shrink and fade out
            val t = (z.deathTimer / Zombie.DEATH_SECONDS).coerceIn(0f, 1f)
            matrix.postRotate((1f - t) * 35f, z.x, z.y)
            matrix.postScale(0.7f + 0.3f * t, 0.7f + 0.3f * t, z.x, z.y)
            spritePaint.alpha = (255 * t).toInt()
            canvas.drawBitmap(bmp, matrix, spritePaint)
            spritePaint.alpha = 255
            return
        }

        canvas.drawCircle(z.x + 4f, z.y + 6f, z.radius, shadowPaint)
        if (z.enraged) canvas.drawCircle(z.x, z.y, z.radius + 14f, enragedPaint)

        // freshly spawned zombies (e.g. a Brood Mother's minions) grow and fade in
        val spawn = (z.age / GameConfig.ZOMBIE_SPAWN_ANIM_SECONDS).coerceIn(0f, 1f)
        if (spawn < 1f) {
            val scale = 0.6f + 0.4f * spawn
            matrix.postScale(scale, scale, z.x, z.y)
            spritePaint.alpha = (255 * spawn).toInt()
        }
        canvas.drawBitmap(bmp, matrix, spritePaint)
        spritePaint.alpha = 255
        if (z.hitFlash > 0f) { // brief white flash where a bullet landed
            flashPaint.alpha = (200 * (z.hitFlash / HIT_FLASH_SECONDS)).toInt().coerceIn(0, 255)
            canvas.drawBitmap(bmp, matrix, flashPaint)
        }

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


    // ---------------------------------------------------------------- Phase 9 drawing

    /** Floor decoration scattered over the map; positions are fixed (seeded), only visible ones are drawn. */
    private fun drawDecor(canvas: Canvas, world: GameWorld) {
        val left = world.camera.x - DECOR_SIZE_MAX
        val top = world.camera.y - DECOR_SIZE_MAX
        val right = world.camera.x + world.viewportWidth + DECOR_SIZE_MAX
        val bottom = world.camera.y + world.viewportHeight + DECOR_SIZE_MAX
        var i = 0
        while (i < DECOR_COUNT) {
            val x = decorX[i]
            val y = decorY[i]
            if (x >= left && x <= right && y >= top && y <= bottom) {
                val bmp = sprites.decor[decorKind[i]]
                val half = bmp.width / 2f
                matrix.reset()
                matrix.postRotate(decorRot[i], half, half)
                matrix.postScale(decorScale[i], decorScale[i], half, half)
                matrix.postTranslate(x - half, y - half)
                canvas.drawBitmap(bmp, matrix, spritePaint)
            }
            i++
        }
    }

    /** Blood pools, goo and scorch marks lying on the floor, fading out as they age. */
    private fun drawDecals(canvas: Canvas, world: GameWorld) {
        val d = world.decals
        val left = world.camera.x - 160f
        val top = world.camera.y - 160f
        val right = world.camera.x + world.viewportWidth + 160f
        val bottom = world.camera.y + world.viewportHeight + 160f
        var i = 0
        while (i < d.count) {
            val a = d.alpha(i)
            if (a > 0f && d.x[i] >= left && d.x[i] <= right && d.y[i] >= top && d.y[i] <= bottom) {
                val bmp = sprites.decal(d.kind[i], d.variant[i])
                val half = bmp.width / 2f
                val scale = d.size[i] / bmp.width
                matrix.reset()
                matrix.postRotate(Math.toDegrees(d.rot[i].toDouble()).toFloat(), half, half)
                matrix.postScale(scale, scale, half, half)
                matrix.postTranslate(d.x[i] - half, d.y[i] - half)
                spritePaint.alpha = (255 * a).toInt()
                canvas.drawBitmap(bmp, matrix, spritePaint)
            }
            i++
        }
        spritePaint.alpha = 255
    }

    /** Blood, sparks, smoke, fire, casings and pickup sparkles. Each kind has its own look. */
    private fun drawParticles(canvas: Canvas, world: GameWorld) {
        val ps = world.particles
        var i = 0
        while (i < ps.count) {
            val t = 1f - ps.life[i] / ps.maxLife[i] // 0 = just born, 1 = about to vanish
            val x = ps.x[i]
            val y = ps.y[i]
            val s = ps.size[i]
            when (ps.kind[i]) {
                ParticleSystem.BLOOD, ParticleSystem.GOO -> {
                    particlePaint.color = if (ps.kind[i] == ParticleSystem.BLOOD) colors.particleBlood else colors.particleGoo
                    particlePaint.alpha = (255 * (1f - t * t)).toInt()
                    canvas.drawCircle(x, y, s * (1f - 0.4f * t), particlePaint)
                }
                ParticleSystem.SPARK -> {
                    sparkPaint.color = colors.particleSpark
                    sparkPaint.alpha = (255 * (1f - t)).toInt()
                    sparkPaint.strokeWidth = s
                    canvas.drawLine(x, y, x - ps.vx[i] * 0.035f, y - ps.vy[i] * 0.035f, sparkPaint)
                }
                ParticleSystem.DUST -> {
                    particlePaint.color = colors.particleDust
                    particlePaint.alpha = (110 * (1f - t)).toInt()
                    canvas.drawCircle(x, y, s * (1f + 1.5f * t), particlePaint)
                }
                ParticleSystem.SMOKE -> {
                    particlePaint.color = colors.particleSmoke
                    particlePaint.alpha = (120 * (1f - t)).toInt()
                    canvas.drawCircle(x, y, s * (1f + 1.8f * t), particlePaint)
                }
                ParticleSystem.FIRE -> {
                    particlePaint.color = colors.particleFire
                    particlePaint.alpha = (255 * (1f - t)).toInt()
                    canvas.drawCircle(x, y, s * (1f - 0.7f * t), particlePaint)
                }
                ParticleSystem.CASING -> {
                    particlePaint.color = colors.particleCasing
                    particlePaint.alpha = (255 * (1f - t * t * t)).toInt()
                    canvas.save()
                    canvas.rotate(Math.toDegrees(ps.rot[i].toDouble()).toFloat(), x, y)
                    canvas.drawRect(x - s, y - s * 0.45f, x + s, y + s * 0.45f, particlePaint)
                    canvas.restore()
                }
                else -> { // PICKUP sparkle in the power-up's colour
                    particlePaint.color = colors.powerUp[ps.tag[i].coerceIn(0, colors.powerUp.size - 1)]
                    particlePaint.alpha = (255 * (1f - t)).toInt()
                    canvas.drawCircle(x, y, s * (1f - 0.6f * t), particlePaint)
                }
            }
            i++
        }
        particlePaint.alpha = 255
    }

    /** Damage numbers: pop in a little larger, float up, fade out. */
    private fun drawDamageNumbers(canvas: Canvas, world: GameWorld) {
        val ft = world.floatingTexts
        var i = 0
        while (i < ft.count) {
            val t = ft.age[i] / GameConfig.DAMAGE_NUMBER_LIFE
            val kind = ft.kind[i]
            val base = if (kind == FloatingTexts.KILL) 40f else 30f
            val pop = if (t < 0.2f) 1f + 0.5f * (1f - t / 0.2f) else 1f
            val text = numberText(ft.value[i])
            val alpha = (255 * (1f - t * t)).toInt()
            numberPaint.textSize = base * pop
            numberOutlinePaint.textSize = base * pop
            numberOutlinePaint.alpha = (alpha * 0.8f).toInt()
            numberPaint.color = when (kind) {
                FloatingTexts.BOOSTED -> colors.damageNumberBoosted
                FloatingTexts.KILL -> colors.damageNumberKill
                else -> colors.damageNumber
            }
            numberPaint.alpha = alpha
            canvas.drawText(text, ft.x[i], ft.y[i], numberOutlinePaint)
            canvas.drawText(text, ft.x[i], ft.y[i], numberPaint)
            i++
        }
    }

    private fun numberText(v: Int): String {
        val i = if (v < 0) 0 else if (v >= numberStrings.size) numberStrings.size - 1 else v
        return numberStrings[i]
    }

    /** Dark screen edges, plus a pulsing red edge glow when health is low. Screen space, after the world. */
    private fun drawVignette(canvas: Canvas, world: GameWorld) {
        val w = canvas.width
        val h = canvas.height
        if (w != vignetteW || h != vignetteH) {
            vignetteW = w
            vignetteH = h
            val radius = sqrt((w * w + h * h).toFloat()) / 2f
            val cx = w / 2f
            val cy = h / 2f
            val stops = floatArrayOf(0.55f, 1f)
            vignettePaint.shader = RadialGradient(
                cx, cy, radius, intArrayOf(colors.vignette and 0x00FFFFFF, colors.vignette), stops, Shader.TileMode.CLAMP
            )
            lowHealthPaint.shader = RadialGradient(
                cx, cy, radius, intArrayOf(colors.lowHealthVignette and 0x00FFFFFF, colors.lowHealthVignette), stops, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vignettePaint)

        val p = world.player
        val line = p.maxHealth * GameConfig.LOW_HEALTH_FRACTION
        if (p.alive && p.health < line) {
            val severity = 1f - p.health / line // 0 just below the line .. 1 nearly dead
            val hz = GameConfig.LOW_HEALTH_PULSE_HZ * (0.5f + 0.5f * severity)
            val pulse = 0.5f + 0.5f * sin(world.time * hz * 6.2832f)
            lowHealthPaint.alpha = (255 * (0.35f + 0.45f * severity) * (0.55f + 0.45f * pulse)).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), lowHealthPaint)
        }
    }

    /** Which player sprite frame to show: standing, or the two walk steps alternating. */
    private fun playerFrame(world: GameWorld): Int {
        if (!world.player.moving) return 0
        val rate = if (world.player.sprinting) 14f else 9f
        return PLAYER_WALK_FRAMES[(world.time * rate).toInt() and 3]
    }

    private companion object {
        const val TRACER_LENGTH = 32f
        const val ATTACK_FRAME = SpriteAtlas.ZOMBIE_FRAMES - 1
        const val HIT_FLASH_SECONDS = 0.1f // matches Zombie.takeDamage
        const val DECOR_COUNT = 220
        const val DECOR_SIZE_MAX = 160f
        /** The four ticks of a walk cycle: step, stand, other step, stand. */
        val PLAYER_WALK_FRAMES = intArrayOf(1, 0, 2, 0)
    }
}
