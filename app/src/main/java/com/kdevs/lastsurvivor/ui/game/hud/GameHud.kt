package com.kdevs.lastsurvivor.ui.game.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.engine.GameConfig
import com.kdevs.lastsurvivor.game.engine.HudState
import com.kdevs.lastsurvivor.game.engine.WavePhase
import com.kdevs.lastsurvivor.game.entities.ZombieType
import com.kdevs.lastsurvivor.ui.weapons.nameRes

/**
 * In-game HUD drawn by Compose over the game surface. It only recomposes when [hud] changes
 * (health, wave, kills, coins, countdown second), never per frame.
 * Touches pass straight through: nothing here is clickable.
 */
@Composable
fun GameHud(hud: HudState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(12.dp)
        ) {
            // Top-left: health, poison, kills and coins
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HealthBar(hud.health, hud.maxHealth)
                if (hud.poisoned) {
                    Text(
                        text = stringResource(R.string.hud_poisoned),
                        color = colorResource(R.color.hud_poison_text),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(Icons.Filled.GpsFixed, stringResource(R.string.hud_kills_format, hud.kills), colorResource(R.color.hud_banner_accent), popOnChange(hud.kills))
                    Chip(Icons.Filled.MonetizationOn, stringResource(R.string.hud_coins_format, hud.coins), colorResource(R.color.coin_gold), popOnChange(hud.coins))
                    Chip(Icons.Filled.Whatshot, stringResource(R.string.hud_grenades_format, hud.grenades), colorResource(R.color.grenade_flash), popOnChange(hud.grenades))
                }
                // Active power-up buffs with seconds left
                if (hud.rapidFireSeconds > 0 || hud.damageBoostSeconds > 0 || hud.shieldSeconds > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hud.rapidFireSeconds > 0) {
                            Chip(Icons.Filled.FastForward, stringResource(R.string.hud_buff_seconds_format, hud.rapidFireSeconds), colorResource(R.color.powerup_rapid))
                        }
                        if (hud.damageBoostSeconds > 0) {
                            Chip(Icons.Filled.LocalFireDepartment, stringResource(R.string.hud_buff_seconds_format, hud.damageBoostSeconds), colorResource(R.color.powerup_damage))
                        }
                        if (hud.shieldSeconds > 0) {
                            Chip(Icons.Filled.Shield, stringResource(R.string.hud_buff_seconds_format, hud.shieldSeconds), colorResource(R.color.powerup_shield))
                        }
                    }
                }
            }

            // Top-centre: current wave and zombies still to kill
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.hud_wave_format, hud.wave),
                    color = colorResource(R.color.hud_banner_text),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                if (hud.phase == WavePhase.ACTIVE) {
                    Text(
                        text = stringResource(R.string.hud_zombies_left_format, hud.zombiesLeft),
                        color = colorResource(R.color.hud_banner_text).copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (hud.boss != null) {
                    BossBar(hud.boss, hud.bossPercent)
                }
            }

            // Bottom-centre: equipped weapon, ammo and reload state
            AmmoDisplay(hud, Modifier.align(Alignment.BottomCenter))

        }

        // Upper-centre: edge-to-edge band while waiting for the next wave. It sits above the middle of the
        // screen so it does not cover the player, and it is not clickable.
        AnimatedVisibility(
            visible = hud.phase == WavePhase.COUNTDOWN,
            modifier = Modifier.align(BiasAlignment(0f, -0.4f)),
            enter = fadeIn(tween(250)) + scaleIn(tween(350), initialScale = 1.15f),
            exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.95f)
        ) {
            WaveBanner(hud)
        }
    }
}

@StringRes
private fun ZombieType.bossNameRes(): Int = when (this) {
    ZombieType.BRUTE -> R.string.boss_brute
    ZombieType.MOTHER -> R.string.boss_mother
    else -> R.string.boss_generic
}

/** Name and health bar of the living boss; turns orange once it is enraged. */
@Composable
private fun BossBar(boss: ZombieType, percent: Int) {
    val enraged = percent.toFloat() / 100f <= GameConfig.BOSS_ENRAGE_FRACTION
    val shown by animateFloatAsState(percent / 100f, tween(250), label = "bossHealth")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 6.dp)) {
        Text(
            text = stringResource(boss.bossNameRes()),
            color = colorResource(R.color.hud_banner_text),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black
        )
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(colorResource(R.color.hud_boss_back))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown)
                    .background(colorResource(if (enraged) R.color.hud_boss_enraged else R.color.hud_boss_fill))
            )
        }
    }
}

@Composable
private fun AmmoDisplay(hud: HudState, modifier: Modifier = Modifier) {
    val low = hud.magazine > 0 && hud.ammo * 4 <= hud.magazine
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(hud.weapon.nameRes()),
            color = colorResource(R.color.hud_banner_text).copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelLarge
        )
        if (hud.reloading) {
            Text(
                text = stringResource(R.string.hud_reloading),
                color = colorResource(R.color.coin_gold),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        } else {
            Text(
                text = stringResource(R.string.hud_ammo_format, hud.ammo, hud.magazine),
                color = if (low) colorResource(R.color.hud_ammo_low) else colorResource(R.color.game_overlay_text),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/** Boss that spawns on [wave]; the same rotation as the wave spawner (the first boss is wave 5, then 10...). */
private fun bossForWave(wave: Int): ZombieType? {
    val bosses = ZombieType.entries.filter { it.isBoss }
    if (bosses.isEmpty()) return null
    val bossNumber = (wave / GameConfig.BOSS_WAVE_INTERVAL).coerceAtLeast(1)
    return bosses[(bossNumber - 1) % bosses.size]
}

/**
 * Full-width band shown during the countdown: the clear bonus, the wave number and a ring that empties
 * as the countdown runs. Boss waves turn red and name the boss.
 */
@Composable
private fun WaveBanner(hud: HudState, modifier: Modifier = Modifier) {
    val boss = hud.bossWave
    val accent = colorResource(if (boss) R.color.wave_boss_accent else R.color.neon_green)
    val label = colorResource(if (boss) R.color.wave_boss_label else R.color.neon_label)
    val line = colorResource(if (boss) R.color.wave_band_boss_line else R.color.wave_band_line)
    val background = colorResource(if (boss) R.color.wave_band_boss_bg else R.color.wave_band_bg)
    val track = colorResource(if (boss) R.color.wave_ring_track_boss else R.color.wave_ring_track)
    val bossType = remember(hud.wave, boss) { if (boss) bossForWave(hud.wave) else null }
    val totalSeconds = if (hud.wave <= 1) GameConfig.FIRST_WAVE_COUNTDOWN else GameConfig.WAVE_COUNTDOWN

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = stroke)
                drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = stroke)
            }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (hud.clearedBonus > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.hud_banner_cleared),
                        color = colorResource(R.color.hud_health_good),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = stringResource(R.string.hud_banner_bonus_format, hud.clearedBonus),
                        color = colorResource(R.color.coin_gold),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (boss) {
                Text(
                    text = stringResource(R.string.hud_banner_boss),
                    color = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            Text(
                text = stringResource(R.string.hud_banner_wave_format, hud.wave),
                color = accent,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                maxLines = 1
            )
            Text(
                text = if (bossType != null) stringResource(bossType.bossNameRes()) else stringResource(R.string.hud_banner_get_ready),
                color = label,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp
            )
        }
        CountdownRing(hud.countdownSeconds, totalSeconds, accent, track)
    }
}

/**
 * A ring that drains smoothly over each second while the number pops once a second.
 * Both animated values are read only when drawing, so nothing recomposes per frame.
 */
@Composable
private fun CountdownRing(seconds: Int, totalSeconds: Float, color: Color, track: Color, modifier: Modifier = Modifier) {
    val pop = remember { Animatable(1f) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(seconds) {
        pop.snapTo(1.3f)
        progress.snapTo((seconds / totalSeconds).coerceIn(0f, 1f))
        launch { pop.animateTo(1f, tween(300)) }
        progress.animateTo(((seconds - 1) / totalSeconds).coerceIn(0f, 1f), tween(1000, easing = LinearEasing))
    }
    Box(modifier = modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * progress.value, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(
            text = seconds.toString(),
            color = colorResource(R.color.hud_banner_text),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
        )
    }
}

@Composable
private fun HealthBar(health: Int, maxHealth: Int) {
    val fraction = if (maxHealth > 0) (health.toFloat() / maxHealth).coerceIn(0f, 1f) else 0f
    val target = when {
        fraction > 0.5f -> colorResource(R.color.hud_health_good)
        fraction > 0.25f -> colorResource(R.color.hud_health_warn)
        else -> colorResource(R.color.hud_health_low)
    }
    // The bar glides to the new value and changes colour smoothly; a pale "trail" bar lags behind
    // after a hit so the size of the damage stays visible for a moment.
    val shown by animateFloatAsState(fraction, tween(250), label = "health")
    val trail by animateFloatAsState(fraction, tween(700, delayMillis = 250), label = "healthTrail")
    val fill by animateColorAsState(target, tween(300), label = "healthColor")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (fraction <= GameConfig.LOW_HEALTH_FRACTION) {
            PulsingHeart(fill)
        } else {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = fill, modifier = Modifier.size(22.dp))
        }
        Box(
            modifier = Modifier
                .width(190.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(50))
                .background(colorResource(R.color.hud_panel)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(trail)
                    .background(colorResource(R.color.hud_health_trail))
                    .align(Alignment.CenterStart)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown)
                    .background(fill)
                    .align(Alignment.CenterStart)
            )
            Text(
                text = stringResource(R.string.hud_health_format, health, maxHealth),
                color = colorResource(R.color.game_overlay_text),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** A heart that beats while health is low. The scale is read only when drawing, so nothing recomposes per frame. */
@Composable
private fun PulsingHeart(tint: Color) {
    val transition = rememberInfiniteTransition(label = "heart")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "heartScale"
    )
    Icon(
        Icons.Filled.Favorite,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}

/** A modifier that makes its element pop larger for a moment each time [key] changes (not on first show). */
@Composable
private fun popOnChange(key: Any): Modifier {
    val scale = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(key) {
        if (first[0]) {
            first[0] = false
        } else {
            scale.snapTo(1.25f)
            scale.animateTo(1f, tween(180))
        }
    }
    return Modifier.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

@Composable
private fun Chip(icon: ImageVector, label: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = colorResource(R.color.hud_panel)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(label, color = colorResource(R.color.game_overlay_text), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}