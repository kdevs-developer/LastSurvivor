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
private const val ZOMBIE_COUNT = 6
private const val WALK_CYCLES = 60 // integer => seamless loop

private class Building(val xFrac: Float, val wFrac: Float, val hFrac: Float)

/**
 * Animated dark city skyline with zombies shambling across.
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
    val zombieBody = colorResource(R.color.menu_zombie_body)
    val zombieEye = colorResource(R.color.menu_zombie_eye)
    val buildings = remember {
        val rnd = Random(7)
        List(16) { i -> Building(i / 16f, 0.05f + rnd.nextFloat() * 0.04f, 0.18f + rnd.nextFloat() * 0.30f) }
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
                center = Offset(w * 0.78f, h * 0.2f), radius = h * 0.4f
            ),
            radius = h * 0.4f, center = Offset(w * 0.78f, h * 0.2f)
        )
        drawCircle(moon, radius = h * 0.09f, center = Offset(w * 0.78f, h * 0.2f))

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
        }
        drawRect(groundColor, topLeft = Offset(0f, groundY), size = Size(w, h - groundY))

        // zombies
        for (i in 0 until ZOMBIE_COUNT) {
            val progress = (t + i / ZOMBIE_COUNT.toFloat()) % 1f
            val x = progress * (w + 240f) - 120f
            val scale = h * (0.0011f + 0.00015f * (i % 3))
            val bob = sin((t * WALK_CYCLES + i) * 2.0 * PI).toFloat()
            drawZombie(x, groundY + (i % 3) * h * 0.02f, scale, bob, zombieBody, zombieEye)
        }

        // pulsing red vignette
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, vignette.copy(alpha = pulse)),
                center = Offset(w / 2f, h / 2f), radius = w * 0.65f
            )
        )
    }
}

private fun DrawScope.drawZombie(x: Float, groundY: Float, s: Float, bob: Float, body: Color, eye: Color) {
    val bobY = bob * 3f * s * 10f
    val hip = Offset(x, groundY - 40f * s * 10f)
    val shoulder = Offset(x + 6f * s * 10f, hip.y - 38f * s * 10f + bobY)
    val head = Offset(shoulder.x + 4f * s * 10f, shoulder.y - 12f * s * 10f)

    // legs
    val stride = bob * 10f * s * 10f
    drawLine(body, hip, Offset(x - stride, groundY), 7f * s * 10f, StrokeCap.Round)
    drawLine(body, hip, Offset(x + stride, groundY), 7f * s * 10f, StrokeCap.Round)
    // torso
    drawLine(body, hip, shoulder, 14f * s * 10f, StrokeCap.Round)
    // arms reaching forward
    drawLine(body, shoulder, Offset(shoulder.x + 34f * s * 10f, shoulder.y + 4f * s * 10f + bobY), 5f * s * 10f, StrokeCap.Round)
    drawLine(body, shoulder, Offset(shoulder.x + 30f * s * 10f, shoulder.y + 14f * s * 10f - bobY), 5f * s * 10f, StrokeCap.Round)
    // head + glowing eye
    drawCircle(body, 9f * s * 10f, head)
    drawCircle(eye, 1.6f * s * 10f, Offset(head.x + 4f * s * 10f, head.y - 1f * s * 10f))
}
