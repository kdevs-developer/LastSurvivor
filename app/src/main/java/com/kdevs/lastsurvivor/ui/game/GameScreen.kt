package com.kdevs.lastsurvivor.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kdevs.lastsurvivor.game.engine.HudState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.engine.GameSurfaceView
import com.kdevs.lastsurvivor.game.engine.WavePhase
import com.kdevs.lastsurvivor.ui.rememberSoundManager
import com.kdevs.lastsurvivor.ui.game.controls.GameControls
import com.kdevs.lastsurvivor.ui.game.hud.GameHud
import com.kdevs.lastsurvivor.ui.game.hud.WaveBannerOverlay

@Composable
fun GameScreen(
    continueRun: Boolean,
    onExit: () -> Unit,
    viewModel: GameSessionViewModel = viewModel()
) {
    val context = LocalContext.current
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    val gameView = remember { GameSurfaceView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val hud by gameView.hud.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sounds = rememberSoundManager()

    // Equip the weapon and outfit chosen on the upgrades and character screens, then start the run:
    // either from the saved checkpoint (Continue) or from wave 1.
    LaunchedEffect(Unit) {
        gameView.equipWeapon(viewModel.loadEquippedWeapon())
        gameView.setSkin(viewModel.loadSkin())
        val saved = if (continueRun) viewModel.loadCheckpoint() else null
        viewModel.beginRun(continued = saved != null)
        if (saved != null) gameView.continueRun(saved.wave, saved.health, saved.grenades)
    }

    // Shake, gore, damage numbers and the FPS counter follow the Settings screen.
    LaunchedEffect(settings) { gameView.applySettings(settings) }

    // Banks the run once, whether it ends by dying or by quitting (quitting keeps the Continue checkpoint).
    val quit: () -> Unit = {
        viewModel.endRun(hud, gameView.runTotals(), died = hud.gameOver)
        onExit()
    }

    // On death: release any held controls and save the run (the checkpoint is discarded).
    LaunchedEffect(hud.gameOver) {
        if (hud.gameOver) {
            gameView.input.reset()
            viewModel.endRun(hud, gameView.runTotals(), died = true)
        }
    }

    // Each time a new wave starts counting down: bank progress and save the Continue checkpoint.
    LaunchedEffect(hud.wave) {
        if (hud.phase == WavePhase.COUNTDOWN) viewModel.checkpoint(hud, gameView.runTotals())
    }

    // Push pause state into the engine (runs only when the flag changes).
    LaunchedEffect(paused) {
        gameView.setPaused(paused)
        if (paused) gameView.input.reset() // never leave a joystick or button "stuck" across a pause
    }

    // The observer below outlives recompositions, so it reads the latest HUD through this holder.
    val hudRef = rememberUpdatedState(hud)

    // Auto-pause when the app goes to the background.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.pause()
            // the process may be killed while in the background, so bank what was earned now
            if (event == Lifecycle.Event.ON_STOP) viewModel.bankProgress(hudRef.value, gameView.runTotals())
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        when {
            hud.gameOver -> quit()
            paused -> viewModel.resume()
            else -> viewModel.pause()
        }
    }

    Box(Modifier.fillMaxSize().background(colorResource(R.color.game_background))) {
        AndroidView(factory = { gameView }, modifier = Modifier.fillMaxSize())

        if (hud.gameOver) {
            GameHud(hud)
            GameOverOverlay(
                hud = hud,
                onRetry = {
                    viewModel.beginRun(continued = false)
                    gameView.restart()
                },
                onQuit = quit
            )
        } else if (!paused) {
            GameHud(hud)
            GameControls(input = gameView.input)
            IconButton(
                onClick = {
                    sounds.play(SoundEffect.UI_CLICK)
                    viewModel.pause()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Filled.Pause,
                    contentDescription = stringResource(R.string.game_pause_description),
                    tint = colorResource(R.color.game_overlay_text)
                )
            }
            // Above the HUD and controls, semi-transparent so they stay visible underneath
            WaveBannerOverlay(hud)
        } else {
            PauseOverlay(
                hud = hud,
                soundEnabled = soundEnabled,
                onToggleSound = viewModel::toggleSound,
                onResume = {
                    sounds.play(SoundEffect.UI_CLICK)
                    viewModel.resume()
                },
                onQuit = {
                    sounds.play(SoundEffect.UI_CLICK)
                    quit()
                }
            )
        }
    }
}

/**
 * Pause menu over a dimmed game. Wide landscape screens show the title and run summary on the left and the
 * buttons on the right; portrait and narrow screens stack them. It scrolls if the screen is very short.
 */
@Composable
private fun PauseOverlay(
    hud: HudState,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    val sounds = rememberSoundManager()
    val onSound: () -> Unit = {
        // the click is played before switching so it is heard when turning sound on, not when turning it off
        if (!soundEnabled) sounds.enabled = true
        sounds.play(SoundEffect.UI_CLICK)
        onToggleSound()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.pause_scrim))
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        val split = maxWidth >= 560.dp && maxWidth > maxHeight
        if (split) {
            Row(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                PauseSummary(hud, centered = false, modifier = Modifier.weight(1f))
                PauseButtons(soundEnabled, onResume, onSound, onQuit, modifier = Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PauseSummary(hud, centered = true, modifier = Modifier.fillMaxWidth())
                PauseButtons(soundEnabled, onResume, onSound, onQuit, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** PAUSED, where the run stands, and the run's kills, coins and grenades. */
@Composable
private fun PauseSummary(hud: HudState, centered: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            stringResource(R.string.game_paused_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.neon_title),
            letterSpacing = 5.sp,
            maxLines = 1
        )
        Text(
            text = if (hud.phase == WavePhase.ACTIVE) {
                stringResource(R.string.game_pause_wave_active_format, hud.wave, hud.zombiesLeft)
            } else {
                stringResource(R.string.game_pause_wave_countdown_format, hud.wave)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(R.color.neon_label)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            PauseStat(R.string.game_pause_kills, hud.kills.toString(), colorResource(R.color.neon_title))
            PauseStat(R.string.game_pause_coins, stringResource(R.string.hud_coins_format, hud.coins), colorResource(R.color.coin_gold))
            PauseStat(R.string.game_pause_grenades, hud.grenades.toString(), colorResource(R.color.grenade_flash))
        }
    }
}

@Composable
private fun PauseStat(labelRes: Int, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Column {
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = colorResource(R.color.neon_label)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

/** Resume (big, neon), sound toggle, and Quit (red outline so it is not tapped by accident). */
@Composable
private fun PauseButtons(
    soundEnabled: Boolean,
    onResume: () -> Unit,
    onSound: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onResume,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.neon_green),
                contentColor = colorResource(R.color.pause_on_accent)
            )
        ) {
            Text(
                stringResource(R.string.game_resume).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        OutlinedButton(
            onClick = onSound,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = shape,
            border = BorderStroke(1.dp, colorResource(R.color.neon_border)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(R.color.neon_title))
        ) {
            Text(stringResource(if (soundEnabled) R.string.sound_on else R.string.sound_off))
        }
        OutlinedButton(
            onClick = onQuit,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = shape,
            border = BorderStroke(1.dp, colorResource(R.color.pause_danger_border)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(R.color.pause_danger_text))
        ) {
            Text(stringResource(R.string.game_quit_to_menu))
        }
    }
}