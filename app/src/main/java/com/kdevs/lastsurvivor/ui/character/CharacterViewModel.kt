package com.kdevs.lastsurvivor.ui.character

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kdevs.lastsurvivor.data.PlayerPrefsRepository
import com.kdevs.lastsurvivor.data.PlayerProgress
import com.kdevs.lastsurvivor.data.PlayerStats
import com.kdevs.lastsurvivor.data.persistenceScope
import com.kdevs.lastsurvivor.game.entities.CharacterSkin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CharacterUiState(
    val skin: CharacterSkin = CharacterSkin.SURVIVOR,
    val stats: PlayerStats = PlayerStats(),
    val progress: PlayerProgress = PlayerProgress()
)

class CharacterViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PlayerPrefsRepository(app)

    val state: StateFlow<CharacterUiState> = combine(
        repository.skin, repository.stats, repository.progress
    ) { skin, stats, progress -> CharacterUiState(skin, stats, progress) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CharacterUiState())

    fun selectSkin(skin: CharacterSkin) {
        persistenceScope.launch { repository.setSkin(skin) }
    }
}
