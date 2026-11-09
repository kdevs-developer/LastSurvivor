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
import com.kdevs.lastsurvivor.game.engine.GameSurfaceView
import com.kdevs.lastsurvivor.ui.game.controls.GameControls
import com.kdevs.lastsurvivor.ui.game.hud.GameHud

@Composable
fun GameScreen(
    onExit: () -> Unit,
    viewModel: GameSessionViewModel = viewModel()
) {
    val context = LocalContext.current
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    val gameView = remember { GameSurfaceView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val hud by gameView.hud.collectAsStateWithLifecycle()

    // Banks the run's coins and wave once, whether it ends by dying or by quitting.
    val quit: () -> Unit = {
        viewModel.saveRun(hud.wave, hud.coins)
        onExit()
    }

    // On death: release any held controls and save the run.
    LaunchedEffect(hud.gameOver) {
        if (hud.gameOver) {
            gameView.input.reset()
            viewModel.saveRun(hud.wave, hud.coins)
        }
    }

    // Push pause state into the engine (runs only when the flag changes).
    LaunchedEffect(paused) {
        gameView.setPaused(paused)
        if (paused) gameView.input.reset() // never leave a joystick or button "stuck" across a pause
    }

    // Auto-pause when the app goes to the background.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.pause()
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
                    viewModel.newRun()
                    gameView.restart()
                },
                onQuit = quit
            )
        } else if (!paused) {
            GameHud(hud)
            GameControls(input = gameView.input)
            IconButton(
                onClick = viewModel::pause,
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
            PauseOverlay(onResume = viewModel::resume, onQuit = quit)
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onQuit: () -> Unit) {
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
            OutlinedButton(onClick = onQuit, modifier = Modifier.width(220.dp)) { Text(stringResource(R.string.game_quit_to_menu)) }
        }
    }
}
