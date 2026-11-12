package com.kdevs.lastsurvivor.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.kdevs.lastsurvivor.game.engine.CampaignLevel
import com.kdevs.lastsurvivor.game.engine.HudState
import com.kdevs.lastsurvivor.ui.rememberSoundManager

/**
 * Shown when the last wave of a campaign level is cleared. The run's kills, coins and diamonds already
 * include the level bonus. The buttons sit in one row so the screen fits short landscape phones.
 */
@Composable
fun LevelCompleteOverlay(
    hud: HudState,
    level: CampaignLevel,
    hasNext: Boolean,
    onNext: () -> Unit,
    onReplay: () -> Unit,
    onQuit: () -> Unit
) {
    val sounds = rememberSoundManager()

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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.level_complete_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = colorResource(R.color.neon_green),
                modifier = Modifier.graphicsLayer {
                    val s = 1.5f - 0.5f * appear.value
                    scaleX = s
                    scaleY = s
                }
            )
            val textColor = colorResource(R.color.game_overlay_text)
            Text(stringResource(R.string.level_complete_level_format, level.number), style = MaterialTheme.typography.titleLarge, color = textColor)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.game_over_kills_format, hud.kills), style = MaterialTheme.typography.titleSmall, color = textColor)
                Text(stringResource(R.string.game_over_coins_format, hud.coins), style = MaterialTheme.typography.titleSmall, color = colorResource(R.color.coin_gold))
                if (hud.diamonds > 0) {
                    Text(stringResource(R.string.game_over_diamonds_format, hud.diamonds), style = MaterialTheme.typography.titleSmall, color = colorResource(R.color.powerup_diamond))
                }
            }
            Text(
                stringResource(R.string.level_complete_bonus_format, level.bonusCoins, level.bonusDiamonds),
                style = MaterialTheme.typography.labelLarge,
                color = colorResource(R.color.neon_label)
            )
            if (!hasNext) {
                Text(
                    stringResource(R.string.level_complete_campaign_done),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.neon_title)
                )
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasNext) {
                    Button(
                        onClick = { sounds.play(SoundEffect.UI_CLICK); onNext() },
                        modifier = Modifier.width(150.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.neon_green),
                            contentColor = colorResource(R.color.survivor_on_accent)
                        )
                    ) { Text(stringResource(R.string.level_next)) }
                }
                OutlinedButton(
                    onClick = { sounds.play(SoundEffect.UI_CLICK); onReplay() },
                    modifier = Modifier.width(120.dp)
                ) { Text(stringResource(R.string.level_replay)) }
                OutlinedButton(
                    onClick = { sounds.play(SoundEffect.UI_CLICK); onQuit() },
                    modifier = Modifier.width(150.dp)
                ) { Text(stringResource(R.string.game_quit_to_menu)) }
            }
        }
    }
}
