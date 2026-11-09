package com.kdevs.lastsurvivor.ui.game.hud

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.engine.HudState
import com.kdevs.lastsurvivor.game.engine.WavePhase
import com.kdevs.lastsurvivor.ui.weapons.nameRes

/**
 * In-game HUD drawn by Compose over the game surface. It only recomposes when [hud] changes
 * (health, wave, kills, coins, countdown second), never per frame.
 * Touches pass straight through: nothing here is clickable.
 */
@Composable
fun GameHud(hud: HudState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
                Chip(Icons.Filled.GpsFixed, stringResource(R.string.hud_kills_format, hud.kills), colorResource(R.color.hud_banner_accent))
                Chip(Icons.Filled.MonetizationOn, stringResource(R.string.hud_coins_format, hud.coins), colorResource(R.color.coin_gold))
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
        }

        // Bottom-centre: equipped weapon, ammo and reload state
        AmmoDisplay(hud, Modifier.align(Alignment.BottomCenter))

        // Centre: banner while waiting for the next wave
        if (hud.phase == WavePhase.COUNTDOWN) {
            WaveBanner(hud, Modifier.align(Alignment.Center))
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

@Composable
private fun WaveBanner(hud: HudState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (hud.clearedBonus > 0) {
            Text(
                text = stringResource(R.string.hud_banner_cleared),
                color = colorResource(R.color.hud_health_good),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.hud_banner_bonus_format, hud.clearedBonus),
                color = colorResource(R.color.coin_gold),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = stringResource(R.string.hud_banner_wave_format, hud.wave),
            color = colorResource(R.color.hud_banner_accent),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.hud_banner_starts_in_format, hud.countdownSeconds),
            color = colorResource(R.color.hud_banner_text),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun HealthBar(health: Int, maxHealth: Int) {
    val fraction = if (maxHealth > 0) (health.toFloat() / maxHealth).coerceIn(0f, 1f) else 0f
    val fill = when {
        fraction > 0.5f -> colorResource(R.color.hud_health_good)
        fraction > 0.25f -> colorResource(R.color.hud_health_warn)
        else -> colorResource(R.color.hud_health_low)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Filled.Favorite, contentDescription = null, tint = fill, modifier = Modifier.size(22.dp))
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
                    .fillMaxWidth(fraction)
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

@Composable
private fun Chip(icon: ImageVector, label: String, tint: Color) {
    Surface(shape = RoundedCornerShape(50), color = colorResource(R.color.hud_panel)) {
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
