package com.kdevs.lastsurvivor.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.engine.HudState
import com.kdevs.lastsurvivor.ui.rememberSoundManager

/** Shown when the player dies. Coins and the highest wave are saved by the caller. */
@Composable
fun GameOverOverlay(hud: HudState, onRetry: () -> Unit, onQuit: () -> Unit) {
    val sounds = rememberSoundManager()

    // The screen fades in and the title drops in from larger; read only in the draw phase.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(durationMillis = 600)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value }
            .background(colorResource(R.color.game_over_scrim))
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.game_over_title),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = colorResource(R.color.hud_banner_accent),
                modifier = Modifier.graphicsLayer {
                    val s = 1.5f - 0.5f * appear.value
                    scaleX = s
                    scaleY = s
                }
            )
            val textColor = colorResource(R.color.game_overlay_text)
            Text(stringResource(R.string.game_over_wave_format, hud.wave), style = MaterialTheme.typography.titleLarge, color = textColor)
            Text(stringResource(R.string.game_over_kills_format, hud.kills), style = MaterialTheme.typography.titleMedium, color = textColor)
            Text(stringResource(R.string.game_over_coins_format, hud.coins), style = MaterialTheme.typography.titleMedium, color = colorResource(R.color.coin_gold))
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { sounds.play(SoundEffect.UI_CLICK); onRetry() },
                    modifier = Modifier.width(220.dp)
                ) { Text(stringResource(R.string.game_retry)) }
                OutlinedButton(
                    onClick = { sounds.play(SoundEffect.UI_CLICK); onQuit() },
                    modifier = Modifier.width(220.dp)
                ) { Text(stringResource(R.string.game_quit_to_menu)) }
            }
        }
    }
}
