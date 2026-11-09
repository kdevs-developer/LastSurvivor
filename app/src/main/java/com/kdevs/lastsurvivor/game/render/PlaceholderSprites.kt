package com.kdevs.lastsurvivor.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

/**
 * Programmatically generated placeholder art. Later phases replace these with real sprites
 * loaded from assets/sprites/ (see SpriteAtlas in Phase 9). Faces +X (right) at rotation 0.
 */
object PlaceholderSprites {

    fun floorTile(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(0xFF1B1D21.toInt())

        val rnd = Random(42)
        p.color = 0xFF202328.toInt()
        repeat(14) {
            val s = 6f + rnd.nextFloat() * 18f
            val left = rnd.nextFloat() * size
            val top = rnd.nextFloat() * size
            c.drawRect(left, top, left + s, top + s, p)
        }
        p.color = 0xFF16181B.toInt()
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        c.drawRect(1f, 1f, size - 1f, size - 1f, p)
        p.strokeWidth = 1.5f
        repeat(3) {
            val sx = rnd.nextFloat() * size
            val sy = rnd.nextFloat() * size
            c.drawLine(sx, sy, sx + rnd.nextFloat() * 30f - 15f, sy + rnd.nextFloat() * 30f - 15f, p)
        }
        return bmp
    }

    fun player(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val m = size / 2f
        val s = size.toFloat()

        // weapon (pointing right)
        p.color = 0xFF2B2B2B.toInt()
        c.drawRoundRect(RectF(m, m - s * 0.05f, m + s * 0.46f, m + s * 0.05f), 4f, 4f, p)
        // shoulders / arms
        p.color = 0xFF3B6EA5.toInt()
        c.drawCircle(m + s * 0.05f, m - s * 0.26f, s * 0.12f, p)
        c.drawCircle(m + s * 0.05f, m + s * 0.26f, s * 0.12f, p)
        // body
        p.color = 0xFF2F5A8A.toInt()
        c.drawCircle(m, m, s * 0.27f, p)
        // head
        p.color = 0xFFD9B38C.toInt()
        c.drawCircle(m + s * 0.04f, m, s * 0.15f, p)
        // hair
        p.color = 0xFF3A2A1C.toInt()
        c.drawCircle(m - s * 0.01f, m, s * 0.11f, p)
        return bmp
    }
}
