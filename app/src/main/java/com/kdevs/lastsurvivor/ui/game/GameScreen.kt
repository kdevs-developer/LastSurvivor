package com.kdevs.lastsurvivor.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
        } else {
            PauseOverlay(
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

@Composable
private fun PauseOverlay(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    val sounds = rememberSoundManager()
    Box(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.game_paused_title),
                style = MaterialTheme.typography.headlineLarge,
                color = colorResource(R.color.game_overlay_text)
            )
            Button(onClick = onResume, modifier = Modifier.width(220.dp)) { Text(stringResource(R.string.game_resume)) }
            OutlinedButton(
                onClick = {
                    // the click is played before switching so it is heard when turning sound on, not when turning it off
                    if (!soundEnabled) sounds.enabled = true
                    sounds.play(SoundEffect.UI_CLICK)
                    onToggleSound()
                },
                modifier = Modifier.width(220.dp)
            ) {
                Text(stringResource(if (soundEnabled) R.string.sound_on else R.string.sound_off))
            }
            OutlinedButton(onClick = onQuit, modifier = Modifier.width(220.dp)) { Text(stringResource(R.string.game_quit_to_menu)) }
        }
    }
}
