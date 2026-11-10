package com.kdevs.lastsurvivor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.engine.GoreLevel
import com.kdevs.lastsurvivor.ui.rememberSoundManager

/** Options saved in DataStore. Every change applies at once and is picked up by the next run. */
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sounds = rememberSoundManager()
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = { sounds.play(SoundEffect.UI_CLICK); onBack() }) {
                Text(stringResource(R.string.common_back))
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToggleRow(R.string.settings_sound, null, settings.soundEnabled) { on ->
                // the click is heard when turning sound on, not when turning it off
                if (on) {
                    sounds.enabled = true
                    sounds.play(SoundEffect.UI_CLICK)
                }
                viewModel.update { it.copy(soundEnabled = on) }
            }
            ToggleRow(R.string.settings_screen_shake, R.string.settings_screen_shake_desc, settings.screenShake) { on ->
                sounds.play(SoundEffect.UI_CLICK)
                viewModel.update { it.copy(screenShake = on) }
            }
            ToggleRow(R.string.settings_damage_numbers, R.string.settings_damage_numbers_desc, settings.damageNumbers) { on ->
                sounds.play(SoundEffect.UI_CLICK)
                viewModel.update { it.copy(damageNumbers = on) }
            }
            ToggleRow(R.string.settings_show_fps, R.string.settings_show_fps_desc, settings.showFps) { on ->
                sounds.play(SoundEffect.UI_CLICK)
                viewModel.update { it.copy(showFps = on) }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.settings_gore),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                for (level in GoreLevel.entries) {
                    val label = stringResource(level.labelRes())
                    val choose = {
                        sounds.play(SoundEffect.UI_CLICK)
                        viewModel.update { it.copy(gore = level) }
                    }
                    if (settings.gore == level) Button(onClick = choose) { Text(label) }
                    else OutlinedButton(onClick = choose) { Text(label) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_reset), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.settings_reset_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorResource(R.color.character_stat_label)
                    )
                }
                OutlinedButton(onClick = { sounds.play(SoundEffect.UI_CLICK); confirmReset = true }) {
                    Text(stringResource(R.string.settings_reset))
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    sounds.play(SoundEffect.UI_DENIED)
                    viewModel.resetProgress()
                }) { Text(stringResource(R.string.settings_reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.settings_reset_cancel)) }
            }
        )
    }
}

@Composable
private fun ToggleRow(titleRes: Int, descriptionRes: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            if (descriptionRes != null) {
                Text(
                    stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorResource(R.color.character_stat_label)
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun GoreLevel.labelRes(): Int = when (this) {
    GoreLevel.OFF -> R.string.settings_gore_off
    GoreLevel.LOW -> R.string.settings_gore_low
    GoreLevel.NORMAL -> R.string.settings_gore_normal
}
