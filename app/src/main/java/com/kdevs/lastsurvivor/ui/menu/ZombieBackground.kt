package com.kdevs.lastsurvivor.ui.menu

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.colorResource
import com.kdevs.lastsurvivor.R
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val LOOP_MS = 40_000

/** Loop positions (0..1) of lightning strikes; two are double flashes. None is near the loop seam. */
private val FLASHES = floatArrayOf(0.115f, 0.125f, 0.46f, 0.72f, 0.733f)
private const val FLASH_LENGTH = 0.012f // fraction of the loop, about half a second
private const val WINDOW_COLUMNS = 2
private const val BAT_COUNT = 3
private const val BAT_FLAP_CYCLES = 300 // integer => seamless loop

private class Building(
    val xFrac: Float,
    val wFrac: Float,
    val hFrac: Float,
    val rows: Int,
    val lit: BooleanArray,      // which windows have a light on
    val flickers: BooleanArray  // which of those lights flicker
)

/**
 * Animated infected-city skyline: moon, fog, lit windows, bats and lightning.
 * Animated values are read inside the draw lambda only, so this never recomposes per frame.
 */
@Composable
fun ZombieBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "menuBackground")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing)),
        label = "time"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.12f, targetValue = 0.34f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val skyTop = colorResource(R.color.menu_sky_top)
    val skyMiddle = colorResource(R.color.menu_sky_middle)
    val skyBottom = colorResource(R.color.menu_sky_bottom)
    val moon = colorResource(R.color.menu_moon)
    val moonGlowStart = colorResource(R.color.menu_moon_glow_start)
    val moonGlowEnd = colorResource(R.color.menu_moon_glow_end)
    val fogStart = colorResource(R.color.menu_fog_start)
    val fogEnd = colorResource(R.color.menu_fog_end)
    val buildingColor = colorResource(R.color.menu_building)
    val groundColor = colorResource(R.color.menu_ground)
    val vignette = colorResource(R.color.menu_vignette)
    val lightning = colorResource(R.color.menu_lightning)
    val bolt = colorResource(R.color.menu_bolt)
    val windowLit = colorResource(R.color.menu_window_lit)
    val batColor = colorResource(R.color.menu_bat)
    val moonDark = colorResource(R.color.menu_moon_dark)
    val buildings = remember {
        val rnd = Random(7)
        List(16) { i ->
            val hFrac = 0.18f + rnd.nextFloat() * 0.30f
            val rows = (hFrac / 0.04f).toInt().coerceAtLeast(2)
            val count = rows * WINDOW_COLUMNS
            Building(
                xFrac = i / 16f,
                wFrac = 0.05f + rnd.nextFloat() * 0.04f,
                hFrac = hFrac,
                rows = rows,
                lit = BooleanArray(count) { rnd.nextFloat() < 0.16f },
                flickers = BooleanArray(count) { rnd.nextFloat() < 0.25f }
            )
        }
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.86f

        drawRect(Brush.verticalGradient(listOf(skyTop, skyMiddle, skyBottom)))

        // blood moon
        drawCircle(
            Brush.radialGradient(
                listOf(moonGlowStart, moonGlowEnd),
                center = Offset(w * 0.5f, h * 0.2f), radius = h * 0.55f
            ),
            radius = h * 0.55f, center = Offset(w * 0.5f, h * 0.2f)
        )
        drawCircle(moon, radius = h * 0.1f, center = Offset(w * 0.5f, h * 0.2f))
        drawCircle(moonDark, radius = h * 0.022f, center = Offset(w * 0.5f - h * 0.03f, h * 0.2f - h * 0.025f))
        drawCircle(moonDark, radius = h * 0.014f, center = Offset(w * 0.5f + h * 0.04f, h * 0.2f + h * 0.03f))

        // bats crossing the sky (laps are whole numbers, so the loop is seamless)
        val flap = sin(t * BAT_FLAP_CYCLES * 2.0 * PI).toFloat()
        for (i in 0 until BAT_COUNT) {
            val laps = 2 + i
            val lap = (t * laps + i / BAT_COUNT.toFloat()) % 1f
            val batX = if (i % 2 == 0) lap * (w + 200f) - 100f else w + 100f - lap * (w + 200f)
            val batY = h * (0.14f + 0.09f * i) + sin((t * laps * 4f + i) * 2.0 * PI).toFloat() * h * 0.02f
            drawBat(batX, batY, h * 0.003f * (1f + 0.2f * i), flap, batColor)
        }

        // lightning: the sky brightens for a moment, and a bolt strikes behind the skyline
        var flash = 0f
        var flashIndex = -1
        for (i in FLASHES.indices) {
            val d = t - FLASHES[i]
            if (d >= 0f && d <= FLASH_LENGTH) {
                val e = 1f - d / FLASH_LENGTH
                if (e > flash) { flash = e; flashIndex = i }
            }
        }
        if (flashIndex >= 0) {
            drawRect(lightning.copy(alpha = 0.22f * flash))
            val boltX = w * (0.15f + 0.17f * flashIndex)
            var px = boltX
            var py = 0f
            val steps = 6
            for (step in 1..steps) {
                val ny = h * 0.5f * step / steps
                val nx = boltX + sin(step * 2.3f + flashIndex * 1.7f) * w * 0.03f
                drawLine(bolt.copy(alpha = flash), Offset(px, py), Offset(nx, ny), 3f, StrokeCap.Round)
                px = nx
                py = ny
            }
        }

        // drifting fog
        for (k in 0 until 3) {
            val fx = (((t + k / 3f) % 1f) * (w + 400f)) - 200f
            val fy = groundY - h * (0.05f + 0.07f * k)
            drawCircle(
                Brush.radialGradient(
                    listOf(fogStart, fogEnd),
                    center = Offset(fx, fy), radius = h * 0.35f
                ),
                radius = h * 0.35f, center = Offset(fx, fy)
            )
        }

        // skyline
        for (b in buildings) {
            val bh = h * b.hFrac
            drawRect(
                buildingColor,
                topLeft = Offset(b.xFrac * w, groundY - bh),
                size = Size(b.wFrac * w, bh)
            )
            // lit windows; some of them flicker
            val bw = b.wFrac * w
            val ww = bw * 0.2f
            val wh = ww * 1.2f
            for (r in 0 until b.rows) {
                val wy = groundY - bh + h * 0.03f + r * h * 0.04f
                if (wy + wh > groundY - h * 0.02f) break
                for (c in 0 until WINDOW_COLUMNS) {
                    val idx = r * WINDOW_COLUMNS + c
                    if (!b.lit[idx]) continue
                    var alpha = 0.8f
                    if (b.flickers[idx]) {
                        val wave = sin((t * (24 + idx % 5 * 6) + idx * 0.37f) * 2.0 * PI).toFloat()
                        if (wave < -0.3f) continue
                        alpha = 0.55f + 0.25f * wave
                    }
                    drawRect(
                        windowLit.copy(alpha = alpha * 0.25f),
                        topLeft = Offset(b.xFrac * w + bw * (0.18f + c * 0.45f) - ww * 0.25f, wy - wh * 0.25f),
                        size = Size(ww * 1.5f, wh * 1.5f)
                    )
                    drawRect(
                        windowLit.copy(alpha = alpha),
                        topLeft = Offset(b.xFrac * w + bw * (0.18f + c * 0.45f), wy),
                        size = Size(ww, wh)
                    )
                }
            }

        }
        drawRect(groundColor, topLeft = Offset(0f, groundY), size = Size(w, h - groundY))

        // pulsing red vignette
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, vignette.copy(alpha = pulse)),
                center = Offset(w / 2f, h / 2f), radius = w * 0.65f
            )
        )
    }
}

/** A bat silhouette: [flap] in -1..1 raises and lowers the wings. */
private fun DrawScope.drawBat(x: Float, y: Float, s: Float, flap: Float, color: Color) {
    val lift = flap * 9f * s
    for (k in 0..1) {
        val side = if (k == 0) -1 else 1
        val mid = Offset(x + side * 9f * s, y - lift - 2f * s)
        val tip = Offset(x + side * 18f * s, y + lift * 0.6f)
        drawLine(color, Offset(x, y), mid, 3f * s, StrokeCap.Round)
        drawLine(color, mid, tip, 2.2f * s, StrokeCap.Round)
    }
    drawCircle(color, 3.5f * s, Offset(x, y))
}