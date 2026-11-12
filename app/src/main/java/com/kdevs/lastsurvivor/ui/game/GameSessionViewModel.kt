package com.kdevs.lastsurvivor.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kdevs.lastsurvivor.data.PlayerPrefsRepository
import com.kdevs.lastsurvivor.data.RunCheckpoint
import com.kdevs.lastsurvivor.data.persistenceScope
import com.kdevs.lastsurvivor.game.engine.GameSettings
import com.kdevs.lastsurvivor.game.engine.HudState
import com.kdevs.lastsurvivor.game.engine.RunSetup
import com.kdevs.lastsurvivor.game.engine.RunTotals
import com.kdevs.lastsurvivor.game.entities.GameCharacter
import com.kdevs.lastsurvivor.game.weapons.WeaponStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Application-level state for a play session: pause, and saving the run.
 * Per-frame simulation state lives in GameWorld, not here.
 *
 * Saving works in "banks": the engine counts coins, kills and so on from zero for each stretch of
 * play, and each bank writes only what is new since the previous one ([banked]). Banks happen when
 * a new wave starts (together with the Continue checkpoint), when the app goes to the background,
 * and when the run ends. Writes run in [persistenceScope] so they finish even if this screen is
 * already closed.
 */
class GameSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PlayerPrefsRepository(app)

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    /** The player's options; the screen applies them to the engine whenever they change. */
    val settings: StateFlow<GameSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    val soundEnabled: StateFlow<Boolean> =
        repository.soundEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun toggleSound() {
        persistenceScope.launch { repository.setSoundEnabled(!soundEnabled.value) }
    }

    private var banked = RunTotals()
    private var runEnded = false

    /** What is being played; saved in the Continue checkpoint. Set by the game screen before the run starts. */
    private var setup: RunSetup = RunSetup.DEFAULT

    fun setSetup(newSetup: RunSetup) { setup = newSetup }

    /** Campaign waves never count as the endless "best wave" (they are capped by the level), so those runs bank wave 0. */
    private fun bankedWave(wave: Int): Int = if (setup.isCampaign) 0 else wave

    fun pause() { _paused.value = true }
    fun resume() { _paused.value = false }

    /**
     * Call once when the screen opens. A fresh run counts as a run played and discards the old
     * checkpoint; a continued one does neither. Also used for Retry.
     */
    fun beginRun(continued: Boolean) {
        banked = RunTotals()
        runEnded = false
        _paused.value = false
        persistenceScope.launch { repository.beginRun(fresh = !continued) }
    }

    /** A new wave is about to start: bank progress and save the Continue checkpoint. */
    fun checkpoint(hud: HudState, totals: RunTotals) {
        if (runEnded || hud.gameOver || hud.levelComplete || hud.wave < 2) return
        bank(totals, bankedWave(hud.wave), RunCheckpoint(hud.wave, hud.health, hud.grenades, setup), clear = false)
    }

    /** The app is leaving the foreground: bank what was earned, keep the checkpoint as it is. */
    fun bankProgress(hud: HudState, totals: RunTotals) {
        if (runEnded) return
        bank(totals, bankedWave(hud.wave), null, clear = false)
    }

    /**
     * The run is over. If the player [died] the checkpoint is thrown away; if they quit, it stays
     * so Continue can pick the run up again. Safe to call more than once: only the first call saves.
     */
    fun endRun(hud: HudState, totals: RunTotals, died: Boolean) {
        if (runEnded) return
        runEnded = true
        bank(totals, bankedWave(hud.wave), null, clear = died)
    }

    /** The last wave of campaign [level] was cleared: bank the run, drop the checkpoint and unlock the next level. */
    fun completeLevel(hud: HudState, totals: RunTotals, level: Int) {
        endRun(hud, totals, died = true) // "died" here only means: discard the checkpoint
        persistenceScope.launch { repository.completeLevel(level) }
    }

    private fun bank(totals: RunTotals, wave: Int, checkpoint: RunCheckpoint?, clear: Boolean) {
        val delta = totals - banked
        banked = totals
        persistenceScope.launch { repository.bankRun(delta, wave, checkpoint, clear) }
    }

    /** The equipped weapon with its saved upgrades. */
    suspend fun loadEquippedWeapon(): WeaponStats = repository.weapons.first().equippedStats()

    /** The character chosen on the Survivor screen (Survivor if the choice is not owned). */
    suspend fun loadCharacter(): GameCharacter = repository.character.first()

    suspend fun loadCheckpoint(): RunCheckpoint? = repository.checkpoint.first()
}
