package com.kdevs.lastsurvivor.ui.game

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-level state for a play session (pause, later: game-over, HUD snapshots).
 * Per-frame simulation state lives in GameWorld, not here.
 */
class GameSessionViewModel : ViewModel() {

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    fun pause() { _paused.value = true }
    fun resume() { _paused.value = false }
}
