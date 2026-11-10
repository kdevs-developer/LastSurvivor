package com.kdevs.lastsurvivor.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.kdevs.lastsurvivor.game.entities.ZombieType
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Programmatically drawn art, used whenever there is no PNG for a sprite in assets/sprites/
 * (SpriteAtlas looks there first). Everything faces +X (right) at rotation 0. All colours come
 * from colors.xml through [GameColors]. Runs once at start-up, never per frame.
 */
object PlaceholderSprites {

    /** Kinds of floor decoration; see [decor]. */
    const val DECOR_KINDS = 4

    private class Look(
        val torso: Float, val head: Float, val headOffset: Float,
        val armWidth: Float, val armY: Float, val reachBonus: Float
    )

    private fun lookOf(type: ZombieType): Look = when (type) {
        ZombieType.WALKER -> Look(0.24f, 0.15f, 0.05f, 0.09f, 0.20f, 0f)
        ZombieType.RUNNER -> Look(0.19f, 0.14f, 0.09f, 0.07f, 0.17f, 0.06f)
        ZombieType.TANK -> Look(0.31f, 0.13f, 0.06f, 0.13f, 0.27f, -0.02f)
        ZombieType.TOXIC -> Look(0.24f, 0.15f, 0.05f, 0.09f, 0.20f, 0f)
        ZombieType.BRUTE -> Look(0.30f, 0.13f, 0.10f, 0.14f, 0.30f, 0f)
        ZombieType.MOTHER -> Look(0.21f, 0.14f, 0.07f, 0.07f, 0.19f, 0.02f)
    }

    // ------------------------------------------------------------------ small drawing helpers

    /** A filled circle with a dark outline. */
    private fun disc(c: Canvas, p: Paint, cx: Float, cy: Float, r: Float, fill: Int, outline: Int, ow: Float) {
        p.style = Paint.Style.FILL
        p.color = outline
        c.drawCircle(cx, cy, r + ow, p)
        p.color = fill
        c.drawCircle(cx, cy, r, p)
    }

    /** A soft light patch in the upper-left of a disc, to make it look round. */
    private fun shine(c: Canvas, p: Paint, cx: Float, cy: Float, r: Float, colors: GameColors) {
        p.style = Paint.Style.FILL
        p.color = colors.spriteHighlight
        c.drawCircle(cx - r * 0.3f, cy - r * 0.35f, r * 0.55f, p)
    }

    /** A thick rounded line with a dark outline (arms, tentacles, horns). */
    private fun limb(c: Canvas, p: Paint, x0: Float, y0: Float, x1: Float, y1: Float, w: Float, fill: Int, outline: Int, ow: Float) {
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.color = outline
        p.strokeWidth = w + ow * 2f
        c.drawLine(x0, y0, x1, y1, p)
        p.color = fill
        p.strokeWidth = w
        c.drawLine(x0, y0, x1, y1, p)
        p.style = Paint.Style.FILL
    }

    /** A horizontal rounded bar from x0 to x1 centred on cy (a gun part). */
    private fun bar(c: Canvas, p: Paint, x0: Float, x1: Float, cy: Float, thick: Float, fill: Int, outline: Int, ow: Float) {
        p.style = Paint.Style.FILL
        val r = RectF(x0 - ow, cy - thick / 2f - ow, x1 + ow, cy + thick / 2f + ow)
        p.color = outline
        c.drawRoundRect(r, thick * 0.3f, thick * 0.3f, p)
        r.set(x0, cy - thick / 2f, x1, cy + thick / 2f)
        p.color = fill
        c.drawRoundRect(r, thick * 0.25f, thick * 0.25f, p)
    }

    // ------------------------------------------------------------------ floor and decoration

    fun floorTile(size: Int, colors: GameColors): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val s = size.toFloat()
        c.drawColor(colors.floorBase)

        val rnd = Random(42)
        // grain
        repeat(size) {
            p.color = if (rnd.nextBoolean()) colors.floorSpeck else colors.floorLine
            val x = rnd.nextFloat() * s
            val y = rnd.nextFloat() * s
            c.drawRect(x, y, x + 2f, y + 2f, p)
        }
        // larger worn patches
        p.color = colors.floorSpeck
        repeat(size / 7) {
            val w = 8f + rnd.nextFloat() * 26f
            val x = rnd.nextFloat() * s
            val y = rnd.nextFloat() * s
            c.drawRect(x, y, x + w, y + w * (0.4f + rnd.nextFloat()), p)
        }
        // slab joints: the tile edge plus a faint cross, so the repeat reads as paving
        p.style = Paint.Style.STROKE
        p.color = colors.floorLine
        p.strokeWidth = 3f
        c.drawRect(1.5f, 1.5f, s - 1.5f, s - 1.5f, p)
        p.strokeWidth = 1.5f
        c.drawLine(s / 2f, 0f, s / 2f, s, p)
        c.drawLine(0f, s / 2f, s, s / 2f, p)
        // hairline cracks (random walks)
        repeat(4) {
            var x = rnd.nextFloat() * s
            var y = rnd.nextFloat() * s
            repeat(6) {
                val nx = x + rnd.nextFloat() * 36f - 18f
                val ny = y + rnd.nextFloat() * 36f - 18f
                c.drawLine(x, y, nx, ny, p)
                x = nx
                y = ny
            }
        }
        return bmp
    }

    /**
     * Floor decoration, placed around the map by the renderer. kind: 0 crack, 1 oil stain,
     * 2 rubble, 3 road marking. Drawn on a transparent square of [size] pixels.
     */
    fun decor(kind: Int, size: Int, colors: GameColors, seed: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val s = size.toFloat()
        val rnd = Random(seed)
        when (kind) {
            0 -> { // a jagged crack with two branches
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.strokeJoin = Paint.Join.ROUND
                p.color = colors.decorCrack
                val path = Path()
                var x = s * 0.08f
                var y = s * (0.35f + rnd.nextFloat() * 0.3f)
                path.moveTo(x, y)
                var branchX = 0f
                var branchY = 0f
                var i = 0
                while (x < s * 0.92f) {
                    x += s * (0.07f + rnd.nextFloat() * 0.07f)
                    y += s * (rnd.nextFloat() - 0.5f) * 0.18f
                    path.lineTo(x, y)
                    if (i == 3) { branchX = x; branchY = y }
                    i++
                }
                p.strokeWidth = s * 0.022f
                c.drawPath(path, p)
                p.strokeWidth = s * 0.012f
                var bx = branchX
                var by = branchY
                repeat(4) {
                    val nx = bx + s * (0.04f + rnd.nextFloat() * 0.05f)
                    val ny = by + s * (0.05f + rnd.nextFloat() * 0.05f)
                    c.drawLine(bx, by, nx, ny, p)
                    bx = nx
                    by = ny
                }
            }
            1 -> { // an oil or water stain: overlapping ellipses build up darker in the middle
                p.style = Paint.Style.FILL
                p.color = colors.decorStain
                repeat(6) {
                    val cx = s * (0.35f + rnd.nextFloat() * 0.3f)
                    val cy = s * (0.35f + rnd.nextFloat() * 0.3f)
                    val rx = s * (0.12f + rnd.nextFloat() * 0.16f)
                    val ry = s * (0.10f + rnd.nextFloat() * 0.14f)
                    c.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), p)
                }
            }
            2 -> { // a pile of rubble: lumps with a lit top-left edge
                p.style = Paint.Style.FILL
                repeat(9) {
                    val cx = s * (0.25f + rnd.nextFloat() * 0.5f)
                    val cy = s * (0.25f + rnd.nextFloat() * 0.5f)
                    val r = s * (0.05f + rnd.nextFloat() * 0.07f)
                    p.color = colors.decorRubble
                    c.drawCircle(cx, cy, r, p)
                    p.color = colors.decorRubbleLight
                    c.drawCircle(cx - r * 0.25f, cy - r * 0.25f, r * 0.6f, p)
                }
            }
            else -> { // worn road markings: two paint dashes
                p.style = Paint.Style.FILL
                p.color = colors.decorMark
                val h = s * 0.07f
                c.drawRect(s * 0.08f, s * 0.5f - h / 2f, s * 0.44f, s * 0.5f + h / 2f, p)
                c.drawRect(s * 0.56f, s * 0.5f - h / 2f, s * 0.92f, s * 0.5f + h / 2f, p)
                p.color = colors.floorBase // chip bits out of the paint
                repeat(5) {
                    val x = s * (0.1f + rnd.nextFloat() * 0.8f)
                    c.drawRect(x, s * 0.5f - h / 2f, x + s * 0.03f, s * 0.5f + h / 2f, p)
                }
            }
        }
        return bmp
    }

    // ------------------------------------------------------------------ stains

    /** A blood (or toxic goo) splatter: a few blobs, satellite droplets and short streaks. */
    fun splatter(size: Int, color: Int, seed: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = color
        val s = size.toFloat()
        val m = s / 2f
        val rnd = Random(seed)
        repeat(7) { // overlapping blobs: the alpha adds up to a darker core
            c.drawCircle(
                m + (rnd.nextFloat() - 0.5f) * s * 0.3f, m + (rnd.nextFloat() - 0.5f) * s * 0.3f,
                s * (0.08f + rnd.nextFloat() * 0.13f), p
            )
        }
        repeat(14) { // droplets thrown outwards
            val a = rnd.nextFloat() * 6.2832f
            val d = s * (0.22f + rnd.nextFloat() * 0.22f)
            c.drawCircle(m + cos(a) * d, m + sin(a) * d, s * (0.012f + rnd.nextFloat() * 0.03f), p)
        }
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        repeat(5) { // streaks
            val a = rnd.nextFloat() * 6.2832f
            p.strokeWidth = s * (0.015f + rnd.nextFloat() * 0.02f)
            c.drawLine(m + cos(a) * s * 0.12f, m + sin(a) * s * 0.12f, m + cos(a) * s * (0.3f + rnd.nextFloat() * 0.15f), m + sin(a) * s * (0.3f + rnd.nextFloat() * 0.15f), p)
        }
        return bmp
    }

    /** A scorch mark: dark in the middle, fading to nothing at the edge. */
    fun scorch(size: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val m = size / 2f
        val transparent = color and 0x00FFFFFF
        p.shader = RadialGradient(m, m, m, intArrayOf(color, color, transparent), floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(m, m, m, p)
        return bmp
    }

    // ------------------------------------------------------------------ the player

    /**
     * Top-down player holding [weapon], facing +X. frame 0 = standing, 1 and 2 = the two steps of a walk.
     * The barrel tip lands near WeaponType.muzzleOffset (for a 96 px sprite).
     */
    fun player(size: Int, colors: GameColors, weapon: WeaponType, frame: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val s = size.toFloat()
        val m = s / 2f
        val ow = s * 0.02f
        val line = colors.spriteOutline

        // boots: one forward, one back while walking
        val stride = when (frame) { 1 -> 0.13f; 2 -> -0.13f; else -> 0f }
        disc(c, p, m + s * stride, m - s * 0.17f, s * 0.085f, colors.playerBoot, line, ow)
        disc(c, p, m - s * stride, m + s * 0.17f, s * 0.085f, colors.playerBoot, line, ow)

        // backpack
        p.style = Paint.Style.FILL
        val pack = RectF(m - s * 0.37f, m - s * 0.16f, m - s * 0.12f, m + s * 0.16f)
        p.color = line
        c.drawRoundRect(RectF(pack.left - ow, pack.top - ow, pack.right + ow, pack.bottom + ow), s * 0.05f, s * 0.05f, p)
        p.color = colors.playerPack
        c.drawRoundRect(pack, s * 0.04f, s * 0.04f, p)

        // body
        disc(c, p, m, m, s * 0.27f, colors.playerBody, line, ow)
        shine(c, p, m, m, s * 0.27f, colors)

        drawWeapon(c, p, s, weapon, colors)

        // arms and hands reaching to the weapon
        val nearX = when (weapon) { WeaponType.PISTOL -> 0.15f; WeaponType.SMG -> 0.14f; WeaponType.SHOTGUN -> 0.14f; WeaponType.RIFLE -> 0.13f }
        val farX = when (weapon) { WeaponType.PISTOL -> 0.21f; WeaponType.SMG -> 0.31f; WeaponType.SHOTGUN -> 0.37f; WeaponType.RIFLE -> 0.39f }
        val nearY = m + s * 0.06f
        val farY = m - s * 0.05f
        limb(c, p, m + s * 0.02f, m + s * 0.25f, m + s * nearX, nearY, s * 0.1f, colors.playerArm, line, ow)
        limb(c, p, m + s * 0.02f, m - s * 0.25f, m + s * farX, farY, s * 0.1f, colors.playerArm, line, ow)
        disc(c, p, m + s * nearX, nearY, s * 0.055f, colors.playerSkin, line, ow * 0.8f)
        disc(c, p, m + s * farX, farY, s * 0.055f, colors.playerSkin, line, ow * 0.8f)

        // head and hair
        disc(c, p, m + s * 0.04f, m, s * 0.15f, colors.playerSkin, line, ow)
        p.style = Paint.Style.FILL
        p.color = colors.playerHair
        c.drawCircle(m - s * 0.01f, m, s * 0.11f, p)
        shine(c, p, m - s * 0.01f, m, s * 0.11f, colors)
        return bmp
    }

    private fun drawWeapon(c: Canvas, p: Paint, s: Float, weapon: WeaponType, colors: GameColors) {
        val m = s / 2f
        val ow = s * 0.018f
        val line = colors.spriteOutline
        when (weapon) {
            WeaponType.PISTOL -> {
                bar(c, p, m + s * 0.10f, m + s * 0.37f, m, s * 0.075f, colors.playerWeapon, line, ow)
                bar(c, p, m + s * 0.14f, m + s * 0.30f, m, s * 0.03f, colors.playerWeaponAccent, line, 0f)
            }
            WeaponType.SMG -> {
                bar(c, p, m + s * 0.05f, m + s * 0.43f, m, s * 0.095f, colors.playerWeapon, line, ow)
                bar(c, p, m + s * 0.12f, m + s * 0.30f, m, s * 0.035f, colors.playerWeaponAccent, line, 0f)
                bar(c, p, m + s * 0.38f, m + s * 0.43f, m, s * 0.05f, colors.playerWeaponAccent, line, 0f)
            }
            WeaponType.SHOTGUN -> {
                bar(c, p, m - s * 0.02f, m + s * 0.13f, m, s * 0.085f, colors.playerWeaponWood, line, ow) // stock
                bar(c, p, m + s * 0.13f, m + s * 0.27f, m, s * 0.095f, colors.playerWeapon, line, ow)     // receiver
                bar(c, p, m + s * 0.27f, m + s * 0.53f, m, s * 0.055f, colors.playerWeapon, line, ow)     // barrel
                bar(c, p, m + s * 0.31f, m + s * 0.43f, m, s * 0.095f, colors.playerWeaponWood, line, ow) // pump
                bar(c, p, m + s * 0.50f, m + s * 0.53f, m, s * 0.05f, colors.playerWeaponAccent, line, 0f)
            }
            WeaponType.RIFLE -> {
                bar(c, p, m - s * 0.04f, m + s * 0.11f, m, s * 0.085f, colors.playerWeaponWood, line, ow) // stock
                bar(c, p, m + s * 0.11f, m + s * 0.31f, m, s * 0.075f, colors.playerWeapon, line, ow)     // receiver
                bar(c, p, m + s * 0.31f, m + s * 0.52f, m, s * 0.045f, colors.playerWeapon, line, ow)     // barrel
                bar(c, p, m + s * 0.15f, m + s * 0.31f, m, s * 0.04f, colors.playerWeaponAccent, line, ow) // scope
                bar(c, p, m + s * 0.49f, m + s * 0.52f, m, s * 0.06f, colors.playerWeaponAccent, line, 0f)
            }
        }
    }

    // ------------------------------------------------------------------ zombies

    /**
     * Top-down zombie of [type], facing +X. frames 0..3 are a walk cycle, frame 4 is the attack lunge.
     * Each type has its own build and features; colours come from colors.xml (zombie_* entries).
     */
    fun zombie(size: Int, type: ZombieType, frame: Int, colors: GameColors): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val s = size.toFloat()
        val m = s / 2f
        val ow = s * 0.018f
        val line = colors.spriteOutline
        val i = type.ordinal
        val skin = colors.zombieSkin[i]
        val cloth = colors.zombieCloth[i]
        val glow = colors.zombieGlow[i]
        val look = lookOf(type)

        if (glow != 0) {
            p.style = Paint.Style.FILL
            p.color = glow
            c.drawCircle(m, m, s * 0.46f, p)
        }

        // Brood Mother: a swollen abdomen behind the torso
        if (type == ZombieType.MOTHER) {
            disc(c, p, m - s * 0.20f, m, s * 0.26f, cloth, line, ow)
            p.style = Paint.Style.FILL
            for (k in 0 until 5) {
                val a = 2.45f + k * 0.38f
                val sx = m - s * 0.20f + cos(a) * s * 0.2f
                val sy = m + sin(a) * s * 0.2f
                disc(c, p, sx, sy, s * 0.07f, colors.zombieSac, line, ow * 0.7f)
                shine(c, p, sx, sy, s * 0.07f, colors)
            }
            // two thin tentacles
            for (side in intArrayOf(-1, 1)) {
                val path = Path()
                path.moveTo(m + s * 0.05f, m + side * s * 0.1f)
                path.quadTo(m + s * 0.32f, m + side * s * 0.36f, m + s * 0.44f, m + side * s * 0.2f)
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.color = line
                p.strokeWidth = s * 0.045f + ow * 2f
                c.drawPath(path, p)
                p.color = skin
                p.strokeWidth = s * 0.045f
                c.drawPath(path, p)
            }
            p.style = Paint.Style.FILL
        }

        // arms reaching forward; the walk cycle alternates which one is further out
        val reachL: Float
        val reachR: Float
        when (frame) {
            0 -> { reachL = 0.36f; reachR = 0.22f }
            1 -> { reachL = 0.29f; reachR = 0.29f }
            2 -> { reachL = 0.22f; reachR = 0.36f }
            3 -> { reachL = 0.29f; reachR = 0.29f }
            else -> { reachL = 0.48f; reachR = 0.48f } // attack lunge
        }
        val armW = s * look.armWidth
        val endL = m + s * (reachL + look.reachBonus).coerceAtMost(0.49f)
        val endR = m + s * (reachR + look.reachBonus).coerceAtMost(0.49f)
        val yL = m - s * look.armY
        val yR = m + s * look.armY
        limb(c, p, m, yL, endL, yL, armW, skin, line, ow)
        limb(c, p, m, yR, endR, yR, armW, skin, line, ow)
        val fist = if (type == ZombieType.TANK || type == ZombieType.BRUTE) armW * 0.8f else armW * 0.55f
        disc(c, p, endL, yL, fist, skin, line, ow * 0.8f)
        disc(c, p, endR, yR, fist, skin, line, ow * 0.8f)

        // torso and ragged clothes
        val tx = m - s * 0.02f
        disc(c, p, tx, m, s * look.torso, cloth, line, ow)
        shine(c, p, tx, m, s * look.torso, colors)
        p.style = Paint.Style.FILL
        p.color = colors.zombieWound
        c.drawCircle(tx - s * 0.06f, m + s * 0.08f, s * 0.055f, p)
        p.color = colors.spriteShade
        c.drawCircle(tx + s * 0.05f, m - s * 0.06f, s * 0.07f, p) // a tear in the cloth

        when (type) {
            ZombieType.TANK -> {
                // riveted chest plate and shoulder pads
                p.style = Paint.Style.FILL
                val plate = RectF(m - s * 0.13f, m - s * 0.18f, m + s * 0.09f, m + s * 0.18f)
                p.color = line
                c.drawRoundRect(RectF(plate.left - ow, plate.top - ow, plate.right + ow, plate.bottom + ow), s * 0.05f, s * 0.05f, p)
                p.color = colors.zombieArmor
                c.drawRoundRect(plate, s * 0.04f, s * 0.04f, p)
                p.color = colors.spriteHighlight
                for (k in 0 until 4) {
                    c.drawCircle(plate.left + s * 0.035f + (k % 2) * (plate.width() - s * 0.07f), plate.top + s * 0.04f + (k / 2) * (plate.height() - s * 0.08f), s * 0.014f, p)
                }
                disc(c, p, m + s * 0.0f, yL, s * 0.105f, colors.zombieArmor, line, ow)
                disc(c, p, m + s * 0.0f, yR, s * 0.105f, colors.zombieArmor, line, ow)
                shine(c, p, m, yL, s * 0.105f, colors)
                shine(c, p, m, yR, s * 0.105f, colors)
            }
            ZombieType.TOXIC -> {
                // glowing pustules on the back
                val spots = arrayOf(floatArrayOf(-0.10f, -0.10f, 0.05f), floatArrayOf(-0.14f, 0.06f, 0.04f),
                    floatArrayOf(-0.04f, 0.15f, 0.045f), floatArrayOf(0.02f, -0.16f, 0.035f))
                for (sp in spots) {
                    disc(c, p, m + s * sp[0], m + s * sp[1], s * sp[2], colors.zombiePustule, line, ow * 0.7f)
                    shine(c, p, m + s * sp[0], m + s * sp[1], s * sp[2], colors)
                }
            }
            ZombieType.BRUTE -> {
                // bone spikes along both shoulders
                for (side in intArrayOf(-1, 1)) {
                    for (k in 0 until 3) {
                        val bx = m + s * (-0.06f + 0.08f * k)
                        val by = m + side * s * 0.27f
                        val path = Path()
                        path.moveTo(bx - s * 0.03f, by)
                        path.lineTo(bx - s * 0.015f, m + side * s * (0.43f + 0.015f * k))
                        path.lineTo(bx + s * 0.03f, by)
                        path.close()
                        p.style = Paint.Style.FILL
                        p.color = colors.zombieSpike
                        c.drawPath(path, p)
                        p.style = Paint.Style.STROKE
                        p.strokeWidth = ow * 0.8f
                        p.strokeJoin = Paint.Join.ROUND
                        p.color = line
                        c.drawPath(path, p)
                    }
                }
                p.style = Paint.Style.FILL
            }
            else -> Unit
        }

        // head, eyes
        val hx = m + s * look.headOffset
        disc(c, p, hx, m, s * look.head, skin, line, ow)
        shine(c, p, hx, m, s * look.head, colors)
        if (type == ZombieType.BRUTE) { // horns
            limb(c, p, hx, m - s * look.head * 0.8f, hx + s * 0.07f, m - s * (look.head + 0.1f), s * 0.035f, colors.zombieSpike, line, ow * 0.7f)
            limb(c, p, hx, m + s * look.head * 0.8f, hx + s * 0.07f, m + s * (look.head + 0.1f), s * 0.035f, colors.zombieSpike, line, ow * 0.7f)
        }
        p.style = Paint.Style.FILL
        val eyeR = if (type.isBoss) s * 0.035f else s * 0.025f
        p.color = colors.zombieEye
        c.drawCircle(hx + s * look.head * 0.7f, m - s * look.head * 0.4f, eyeR, p)
        c.drawCircle(hx + s * look.head * 0.7f, m + s * look.head * 0.4f, eyeR, p)
        if (frame == 4) { // mouth open while lunging
            p.color = colors.zombieWound
            c.drawCircle(hx + s * look.head * 0.9f, m, s * 0.035f, p)
        }
        return bmp
    }
}
