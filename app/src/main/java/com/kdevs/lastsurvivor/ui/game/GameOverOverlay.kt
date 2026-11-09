package com.kdevs.lastsurvivor.ui.game

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.engine.HudState

/** Shown when the player dies. Coins and the highest wave are saved by the caller. */
@Composable
fun GameOverOverlay(hud: HudState, onRetry: () -> Unit, onQuit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                color = colorResource(R.color.hud_banner_accent)
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
                Button(onClick = onRetry, modifier = Modifier.width(220.dp)) { Text(stringResource(R.string.game_retry)) }
                OutlinedButton(onClick = onQuit, modifier = Modifier.width(220.dp)) { Text(stringResource(R.string.game_quit_to_menu)) }
            }
        }
    }
}
