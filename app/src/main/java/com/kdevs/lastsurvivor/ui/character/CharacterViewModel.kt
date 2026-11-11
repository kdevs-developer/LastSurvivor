package com.kdevs.lastsurvivor.ui.character

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kdevs.lastsurvivor.data.CharacterInventory
import com.kdevs.lastsurvivor.data.PlayerPrefsRepository
import com.kdevs.lastsurvivor.data.PlayerProgress
import com.kdevs.lastsurvivor.data.PlayerStats
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.audio.SoundManager
import com.kdevs.lastsurvivor.game.entities.GameCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CharacterUiState(
    /** The character on show (browsed with the arrows); not necessarily the one chosen for play. */
    val viewing: GameCharacter = GameCharacter.SURVIVOR,
    val inventory: CharacterInventory = CharacterInventory(),
    val stats: PlayerStats = PlayerStats(),
    val progress: PlayerProgress = PlayerProgress()
) {
    val owned: Boolean get() = viewing in inventory.unlocked
    val selected: Boolean get() = viewing == inventory.selected
    val canAfford: Boolean get() = progress.diamonds >= viewing.diamondPrice
}

class CharacterViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PlayerPrefsRepository(app)
    private val sounds = SoundManager.get(app)
    private val viewing = MutableStateFlow(GameCharacter.SURVIVOR)

    val state: StateFlow<CharacterUiState> = combine(
        viewing, repository.characters, repository.stats, repository.progress
    ) { shown, inventory, stats, progress -> CharacterUiState(shown, inventory, stats, progress) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CharacterUiState())

    init {
        // Open on the character that is currently chosen for play.
        viewModelScope.launch { viewing.value = repository.characters.first().selected }
    }

    /** Shows the next ([delta] = 1) or previous (-1) character, wrapping around. */
    fun step(delta: Int) {
        sounds.play(SoundEffect.UI_CLICK)
        val all = GameCharacter.entries
        viewing.value = all[(all.indexOf(viewing.value) + delta + all.size) % all.size]
    }

    /** Chooses the character on show for the next run (only if owned). */
    fun select() {
        val c = viewing.value
        sounds.play(SoundEffect.UI_CLICK)
        viewModelScope.launch { repository.selectCharacter(c) }
    }

    /** Buys the character on show with diamonds, then chooses it. A purchase sound if it went through, a buzz if not. */
    fun unlock() {
        val c = viewing.value
        viewModelScope.launch {
            val bought = repository.unlockCharacter(c)
            if (bought) repository.selectCharacter(c)
            sounds.play(if (bought) SoundEffect.UI_PURCHASE else SoundEffect.UI_DENIED)
        }
    }
}
