package com.kdevs.lastsurvivor.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kdevs.lastsurvivor.data.PlayerPrefsRepository
import com.kdevs.lastsurvivor.game.weapons.WeaponStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application-level state for a play session: pause, and saving the result of a run.
 * Per-frame simulation state lives in GameWorld, not here.
 */
class GameSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PlayerPrefsRepository(app)

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    private var runSaved = false

    fun pause() { _paused.value = true }
    fun resume() { _paused.value = false }

    /**
     * Banks the coins and highest wave of the current run. Safe to call more than once:
     * only the first call after [newRun] saves.
     */
    fun saveRun(wave: Int, coins: Int) {
        if (runSaved) return
        runSaved = true
        viewModelScope.launch {
            if (coins > 0) repository.addCoins(coins)
            repository.updateHighestWave(wave)
        }
    }

    /** The equipped weapon with its saved upgrades. */
    suspend fun loadEquippedWeapon(): WeaponStats = repository.weapons.first().equippedStats()

    /** Call when a new run starts after game over. */
    fun newRun() {
        runSaved = false
        _paused.value = false
    }
}
