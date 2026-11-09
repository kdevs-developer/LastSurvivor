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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdevs.lastsurvivor.game.engine.GameSurfaceView

@Composable
fun GameScreen(
    onExit: () -> Unit,
    viewModel: GameSessionViewModel = viewModel()
) {
    val context = LocalContext.current
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    val gameView = remember { GameSurfaceView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Push pause state into the engine (runs only when the flag changes).
    LaunchedEffect(paused) { gameView.setPaused(paused) }

    // Auto-pause when the app goes to the background.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        if (paused) viewModel.resume() else viewModel.pause()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { gameView }, modifier = Modifier.fillMaxSize())

        if (!paused) {
            IconButton(
                onClick = viewModel::pause,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = Color.White)
            }
        } else {
            PauseOverlay(onResume = viewModel::resume, onQuit = onExit)
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
            Text("PAUSED", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Button(onClick = onResume, modifier = Modifier.width(220.dp)) { Text("Resume") }
            OutlinedButton(onClick = onQuit, modifier = Modifier.width(220.dp)) { Text("Quit to Menu") }
        }
    }
}
