package com.kdevs.lastsurvivor.ui.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.audio.MusicManager
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.audio.SoundManager
import com.kdevs.lastsurvivor.game.engine.ControlSize
import com.kdevs.lastsurvivor.game.engine.GameSettings
import com.kdevs.lastsurvivor.game.engine.GoreLevel
import com.kdevs.lastsurvivor.ui.rememberSoundManager
import kotlin.math.roundToInt

/** Content never stretches wider than this, so tablets and TVs get a centred, readable layout. */
private val MaxContentWidth: Dp = 1000.dp

/** From this width up the three option panels (Audio, Feel, Display) sit side by side; narrower screens stack them. */
private val ThreePanelWidth: Dp = 560.dp

private typealias SettingsUpdate = ((GameSettings) -> GameSettings) -> Unit

/**
 * Options saved in DataStore. Every change applies at once and is picked up by the next run.
 * Three grouped panels side by side on wide screens or stacked on narrow ones (Audio: sound switch, music and
 * effects volume; Feel: screen shake, damage numbers, vibration; Display: gore, FPS), then a danger strip for
 * Reset progress. Everything fits one screen; the panels share the available height and only scroll inside
 * themselves on a screen too small for their content.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sounds = rememberSoundManager()
    var confirmReset by remember { mutableStateOf(false) }
    val update: SettingsUpdate = { transform -> viewModel.update(transform) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.settings_background))
            .safeDrawingPadding()
    ) {
        val sidePanels = maxWidth >= ThreePanelWidth
        val pad = if (maxWidth < 400.dp || maxHeight < 400.dp) 10.dp else 16.dp

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxSize()
                    .padding(pad),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Header(onBack = { sounds.play(SoundEffect.UI_CLICK); onBack() })

                if (sidePanels) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AudioPanel(settings, sounds, update, Modifier.weight(1f).fillMaxHeight())
                        FeelPanel(settings, sounds, update, Modifier.weight(1f).fillMaxHeight())
                        DisplayPanel(settings, sounds, update, Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    AudioPanel(settings, sounds, update, Modifier.fillMaxWidth().weight(1f))
                    FeelPanel(settings, sounds, update, Modifier.fillMaxWidth().weight(1f))
                    DisplayPanel(settings, sounds, update, Modifier.fillMaxWidth().weight(1f))
                }

                DangerStrip(onReset = { sounds.play(SoundEffect.UI_CLICK); confirmReset = true })
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = colorResource(R.color.neon_panel),
            titleContentColor = colorResource(R.color.neon_title),
            textContentColor = colorResource(R.color.neon_label),
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        sounds.play(SoundEffect.UI_DENIED)
                        viewModel.resetProgress()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colorResource(R.color.settings_danger_text))
                ) { Text(stringResource(R.string.settings_reset_confirm), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmReset = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colorResource(R.color.neon_label))
                ) { Text(stringResource(R.string.settings_reset_cancel)) }
            }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------------------------

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val shape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shape)
                .border(1.dp, colorResource(R.color.neon_border), shape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = colorResource(R.color.neon_green),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.neon_title),
            letterSpacing = 3.sp,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Panels
// ---------------------------------------------------------------------------------------------

@Composable
private fun AudioPanel(settings: GameSettings, sounds: SoundManager, update: SettingsUpdate, modifier: Modifier = Modifier) {
    val music = MusicManager.get(LocalContext.current)
    SettingsPanel(R.string.settings_group_audio, modifier) {
        ToggleRow(R.string.settings_sound, null, settings.soundEnabled, divider = true) { on ->
            // the click is heard when turning sound on, not when turning it off
            if (on) {
                sounds.enabled = true
                sounds.play(SoundEffect.UI_CLICK)
            }
            update { it.copy(soundEnabled = on) }
        }
        VolumeRow(
            titleRes = R.string.settings_music_volume,
            value = settings.musicVolume,
            enabled = settings.soundEnabled,
            divider = true,
            onLive = { music.setVolume(it) }, // you hear the music change while dragging
            onCommit = { v -> update { it.copy(musicVolume = v) } }
        )
        VolumeRow(
            titleRes = R.string.settings_sfx_volume,
            value = settings.sfxVolume,
            enabled = settings.soundEnabled,
            divider = false,
            onLive = { sounds.volume = it },
            onCommit = { v ->
                update { it.copy(sfxVolume = v) }
                sounds.play(SoundEffect.UI_CLICK) // a sample at the new level
            }
        )
    }
}

@Composable
private fun FeelPanel(settings: GameSettings, sounds: SoundManager, update: SettingsUpdate, modifier: Modifier = Modifier) {
    SettingsPanel(R.string.settings_group_feel, modifier) {
        ToggleRow(R.string.settings_screen_shake, R.string.settings_screen_shake_desc, settings.screenShake, divider = true) { on ->
            sounds.play(SoundEffect.UI_CLICK)
            update { it.copy(screenShake = on) }
        }
        ToggleRow(R.string.settings_damage_numbers, R.string.settings_damage_numbers_desc, settings.damageNumbers, divider = true) { on ->
            sounds.play(SoundEffect.UI_CLICK)
            update { it.copy(damageNumbers = on) }
        }
        ToggleRow(R.string.settings_vibration, R.string.settings_vibration_desc, settings.vibration, divider = false) { on ->
            sounds.play(SoundEffect.UI_CLICK)
            update { it.copy(vibration = on) }
        }
    }
}

@Composable
private fun DisplayPanel(settings: GameSettings, sounds: SoundManager, update: SettingsUpdate, modifier: Modifier = Modifier) {
    SettingsPanel(R.string.settings_group_display, modifier) {
        SegmentedRow(R.string.settings_gore, GoreLevel.entries, settings.gore, { it.labelRes() }, divider = true) { level ->
            sounds.play(SoundEffect.UI_CLICK)
            update { it.copy(gore = level) }
        }
        SegmentedRow(R.string.settings_control_size, ControlSize.entries, settings.controlSize, { it.labelRes() }, divider = true) { size ->
            sounds.play(SoundEffect.UI_CLICK)
            update { it.copy(controlSize = size) }
        }
        ToggleRow(R.string.settings_show_fps, R.string.settings_show_fps_desc, settings.showFps, divider = false) { on ->
            sounds.play(SoundEffect.UI_CLICK)
            update { it.copy(showFps = on) }
        }
    }
}

@Composable
private fun SettingsPanel(titleRes: Int, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colorResource(R.color.neon_panel))
            .border(1.dp, colorResource(R.color.neon_border), shape)
            .verticalScroll(rememberScrollState()) // only moves on a screen too small for the content
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(titleRes).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.neon_green_dim),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )
        content()
    }
}

@Composable
private fun RowDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colorResource(R.color.settings_divider)))
}

/** The whole row is the touch target (not just the small switch). */
@Composable
private fun ToggleRow(
    titleRes: Int,
    descriptionRes: Int?,
    checked: Boolean,
    divider: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.neon_title)
            )
            if (descriptionRes != null) {
                Text(
                    stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorResource(R.color.neon_label)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colorResource(R.color.neon_green),
                checkedThumbColor = colorResource(R.color.settings_on_accent),
                checkedBorderColor = colorResource(R.color.neon_green),
                uncheckedTrackColor = colorResource(R.color.settings_switch_off_track),
                uncheckedThumbColor = colorResource(R.color.neon_label),
                uncheckedBorderColor = colorResource(R.color.settings_switch_off_track)
            )
        )
    }
    if (divider) RowDivider()
}

/**
 * A 0..100% slider. [onLive] runs while dragging (so the change is heard at once, nothing is saved yet);
 * [onCommit] runs once when the finger lifts and is where the value is saved.
 */
@Composable
private fun VolumeRow(
    titleRes: Int,
    value: Float,
    enabled: Boolean,
    divider: Boolean,
    onLive: (Float) -> Unit,
    onCommit: (Float) -> Unit
) {
    var local by remember(value) { mutableStateOf(value) }
    Column(modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.45f).padding(top = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.neon_title)
            )
            Text(
                "${(local * 100f).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.neon_green)
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it; onLive(it) },
            onValueChangeFinished = { onCommit(local) },
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = colorResource(R.color.neon_green),
                activeTrackColor = colorResource(R.color.neon_green),
                inactiveTrackColor = colorResource(R.color.settings_switch_off_track),
                disabledThumbColor = colorResource(R.color.neon_label),
                disabledActiveTrackColor = colorResource(R.color.neon_label),
                disabledInactiveTrackColor = colorResource(R.color.settings_switch_off_track)
            )
        )
    }
    if (divider) RowDivider()
}

/** A compact title with a segmented control under it (Gore, Control size). Fits any width. */
@Composable
private fun <T> SegmentedRow(
    titleRes: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    divider: Boolean,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.neon_title)
        )
        Spacer(Modifier.height(4.dp))
        val shape = RoundedCornerShape(8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colorResource(R.color.settings_background))
                .border(1.dp, colorResource(R.color.neon_border), shape)
                .padding(2.dp)
        ) {
            for (option in options) {
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) colorResource(R.color.neon_green) else colorResource(R.color.transparent))
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(label(option)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = colorResource(if (isSelected) R.color.settings_on_accent else R.color.neon_label),
                        maxLines = 1
                    )
                }
            }
        }
    }
    if (divider) RowDivider()
}

// ---------------------------------------------------------------------------------------------
// Danger strip
// ---------------------------------------------------------------------------------------------

@Composable
private fun DangerStrip(onReset: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colorResource(R.color.neon_panel))
            .border(1.dp, colorResource(R.color.settings_danger_border), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_reset),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.settings_danger_text)
            )
            Text(
                stringResource(R.string.settings_reset_desc),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(R.color.neon_label)
            )
        }
        OutlinedButton(
            onClick = onReset,
            border = BorderStroke(1.dp, colorResource(R.color.settings_danger_outline)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(R.color.settings_danger_text))
        ) {
            Text(stringResource(R.string.settings_reset_button), fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun GoreLevel.labelRes(): Int = when (this) {
    GoreLevel.OFF -> R.string.settings_gore_off
    GoreLevel.LOW -> R.string.settings_gore_low
    GoreLevel.NORMAL -> R.string.settings_gore_normal
}

private fun ControlSize.labelRes(): Int = when (this) {
    ControlSize.SMALL -> R.string.settings_control_small
    ControlSize.NORMAL -> R.string.settings_control_normal
    ControlSize.LARGE -> R.string.settings_control_large
}
