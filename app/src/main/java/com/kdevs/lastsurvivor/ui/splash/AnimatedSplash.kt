package com.kdevs.lastsurvivor.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kdevs.lastsurvivor.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * White animated splash shown right after the system splash (which is also white, so the hand-over is
 * seamless): the head pops in, the crosshair spins into place, a red arc orbits it, the title and
 * tagline fade in over a loading line. Calls [onFinished] when the sequence ends (about 2.9 s).
 */
@Composable
fun AnimatedSplash(onFinished: () -> Unit) {
    val night = colorResource(R.color.night)
    val red = colorResource(R.color.blood_red)
    val green = colorResource(R.color.toxic_green)
    val grey = Color(0xFF5F5E5A)

    val pop = remember { Animatable(0f) }
    val spin = remember { Animatable(-120f) }
    val title = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    val load = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "splashLoop")
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "orbit"
    )
    val eyes by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "eyes"
    )

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
            launch { spin.animateTo(0f, tween(900, easing = FastOutSlowInEasing)) }
            launch { delay(600); title.animateTo(1f, tween(500)) }
            launch { delay(900); bar.animateTo(1f, tween(500)) }
            launch { delay(1000); tag.animateTo(1f, tween(500)) }
            launch { delay(300); load.animateTo(1f, tween(2600, easing = LinearEasing)) }
        }
        delay(250)
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            Canvas(Modifier.size(132.dp)) {
                // The artwork is drawn in a 72 x 72 box (the visible part of the 108 dp launcher canvas).
                val u = size.minDimension / 72f
                fun p(v: Float) = (v - 18f) * u
                val c = Offset(p(54f), p(54f))
                val popA = pop.value.coerceIn(0f, 1f)
                val spinA = ((spin.value + 120f) / 120f).coerceIn(0f, 1f)

                scale(scale = pop.value, pivot = c) {
                    drawCircle(night.copy(alpha = popA), radius = 12f * u, center = Offset(p(54f), p(51f)))
                    drawRoundRect(
                        night.copy(alpha = popA),
                        topLeft = Offset(p(48f), p(58f)),
                        size = Size(12f * u, 9f * u),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * u)
                    )
                    drawCircle(green.copy(alpha = popA * eyes), 3f * u, Offset(p(49.5f), p(50.5f)))
                    drawCircle(green.copy(alpha = popA * eyes), 3f * u, Offset(p(58.5f), p(50.5f)))
                    val nose = Path().apply {
                        moveTo(p(54f), p(54.5f)); lineTo(p(52.4f), p(57.5f)); lineTo(p(55.6f), p(57.5f)); close()
                    }
                    drawPath(nose, Color.White.copy(alpha = popA))
                    for (x in listOf(51f, 54f, 57f)) {
                        drawLine(Color.White.copy(alpha = popA), Offset(p(x), p(61.5f)), Offset(p(x), p(65.5f)),
                            strokeWidth = u, cap = StrokeCap.Round)
                    }
                    val drip = Path().apply {
                        moveTo(p(54f), p(67.5f)); lineTo(p(52.3f), p(69.8f)); lineTo(p(55.7f), p(69.8f)); close()
                    }
                    drawPath(drip, red.copy(alpha = popA))
                    drawCircle(red.copy(alpha = popA), 1.8f * u, Offset(p(54f), p(70.2f)))
                }

                rotate(degrees = spin.value, pivot = c) {
                    val ring = night.copy(alpha = spinA)
                    drawCircle(ring, radius = 27f * u, center = c, style = Stroke(3.5f * u))
                    val ticks = listOf(
                        Offset(54f, 18f) to Offset(54f, 24f), Offset(54f, 84f) to Offset(54f, 90f),
                        Offset(18f, 54f) to Offset(24f, 54f), Offset(84f, 54f) to Offset(90f, 54f)
                    )
                    for ((a, b) in ticks) {
                        drawLine(ring, Offset(p(a.x), p(a.y)), Offset(p(b.x), p(b.y)),
                            strokeWidth = 3.5f * u, cap = StrokeCap.Round)
                    }
                }

                rotate(degrees = orbit, pivot = c) {
                    drawArc(
                        color = red.copy(alpha = spinA),
                        startAngle = -135f,
                        sweepAngle = 89f,
                        useCenter = false,
                        topLeft = Offset(c.x - 27f * u, c.y - 27f * u),
                        size = Size(54f * u, 54f * u),
                        style = Stroke(width = 3.5f * u, cap = StrokeCap.Round)
                    )
                }
            }

            Text(
                text = "LAST SURVIVOR",
                color = night,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.14.em,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 8.dp.toPx()
                }
            )
            Box(
                Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .graphicsLayer {
                        scaleX = bar.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(red)
            )
            Text(
                text = "SURVIVE THE NIGHT",
                color = grey,
                fontSize = 12.sp,
                letterSpacing = 0.2.em,
                modifier = Modifier.graphicsLayer {
                    alpha = tag.value
                    translationY = (1f - tag.value) * 8.dp.toPx()
                }
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(load.value)
                .height(3.dp)
                .background(red)
        )
    }
}
