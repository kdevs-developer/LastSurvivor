package com.kdevs.lastsurvivor.ui.character

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.data.PlayerStats
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.entities.CharacterSkin
import com.kdevs.lastsurvivor.ui.rememberSoundManager

/** Outfit picker on the left, lifetime statistics on the right. */
@Composable
fun CharacterScreen(onBack: () -> Unit, viewModel: CharacterViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sounds = rememberSoundManager()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ---- Left: outfits ----
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.character_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.character_outfit_heading), style = MaterialTheme.typography.titleMedium)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (skin in CharacterSkin.entries) {
                    SkinCard(skin, selected = skin == state.skin) {
                        sounds.play(SoundEffect.UI_CLICK)
                        viewModel.selectSkin(skin)
                    }
                }
            }
            OutlinedButton(onClick = { sounds.play(SoundEffect.UI_CLICK); onBack() }) {
                Text(stringResource(R.string.common_back))
            }
        }

        // ---- Right: statistics ----
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(stringResource(R.string.character_stats_heading), style = MaterialTheme.typography.titleMedium)
            val stats = state.stats
            StatRow(R.string.stat_runs, number(stats.runs))
            StatRow(R.string.stat_highest_wave, number(state.progress.highestWave))
            StatRow(R.string.stat_kills, number(stats.kills))
            StatRow(R.string.stat_bosses, number(stats.bossKills))
            StatRow(R.string.stat_accuracy, stringResource(R.string.stat_accuracy_format, stats.accuracyPercent))
            StatRow(R.string.stat_coins_earned, number(stats.coinsEarned))
            StatRow(R.string.stat_time, playTime(stats))
        }
    }
}

@Composable
private fun number(value: Int): String = stringResource(R.string.stat_number_format, value)

@Composable
private fun playTime(stats: PlayerStats): String = when {
    stats.playHours > 0 -> stringResource(R.string.stat_time_hours_format, stats.playHours, stats.playMinutes)
    stats.playMinutes > 0 -> stringResource(R.string.stat_time_minutes_format, stats.playMinutes)
    else -> stringResource(R.string.stat_time_none)
}

@Composable
private fun SkinCard(skin: CharacterSkin, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val borderStroke = if (selected) BorderStroke(2.dp, colorResource(R.color.blood_red)) else BorderStroke(1.dp, colorResource(R.color.character_card))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colorResource(if (selected) R.color.character_card_selected else R.color.character_card))
            .border(borderStroke, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // a jacket-coloured disc with a sleeve-coloured ring
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colorResource(skin.armColorRes()))
                .padding(6.dp)
                .clip(CircleShape)
                .background(colorResource(skin.bodyColorRes()))
        )
        Text(stringResource(skin.nameRes()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (selected) {
            Text(
                stringResource(R.string.skin_selected),
                style = MaterialTheme.typography.labelLarge,
                color = colorResource(R.color.blood_red)
            )
        }
    }
}

@Composable
private fun StatRow(labelRes: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(labelRes), color = colorResource(R.color.character_stat_label), style = MaterialTheme.typography.bodyLarge)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}
