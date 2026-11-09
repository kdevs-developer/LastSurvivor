package com.kdevs.lastsurvivor.game.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import com.kdevs.lastsurvivor.game.engine.GameWorld

/**
 * Draws a GameWorld onto a Canvas. Allocation-free in render(): every Paint, Matrix and
 * Bitmap is created once up front. Runs on the game thread only.
 */
class GameRenderer {

    private val floorTile: Bitmap = PlaceholderSprites.floorTile(128)
    private val playerSprite: Bitmap = PlaceholderSprites.player(96)

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

    private val matrix = Matrix()
    private var lastFps = -1
    private var fpsText = ""

    fun render(canvas: Canvas, world: GameWorld, fps: Int, paused: Boolean) {
        canvas.drawColor(0xFF0B0B0D.toInt())

        canvas.save()
        canvas.translate(-world.camera.x, -world.camera.y)

        // floor + map border
        canvas.drawRect(0f, 0f, GameWorld.WIDTH, GameWorld.HEIGHT, floorPaint)
        canvas.drawRect(0f, 0f, GameWorld.WIDTH, GameWorld.HEIGHT, boundsPaint)

        // player (shadow, then rotated sprite)
        val p = world.player
        canvas.drawCircle(p.x + 6f, p.y + 8f, p.radius, shadowPaint)
        val half = playerSprite.width / 2f
        matrix.reset()
        matrix.postRotate(Math.toDegrees(p.rotation.toDouble()).toFloat(), half, half)
        matrix.postTranslate(p.x - half, p.y - half)
        canvas.drawBitmap(playerSprite, matrix, spritePaint)

        canvas.restore()

        // screen-space debug overlay
        if (fps != lastFps) {
            lastFps = fps
            fpsText = "FPS $fps"
        }
        canvas.drawText(fpsText, 24f, 44f, hudPaint)

        if (paused) {
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), pauseDim)
        }
    }
}
