package com.kdevs.lastsurvivor.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kdevs.lastsurvivor.data.PlayerPrefsRepository
import com.kdevs.lastsurvivor.data.persistenceScope
import com.kdevs.lastsurvivor.game.engine.GameSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PlayerPrefsRepository(app)

    val settings: StateFlow<GameSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GameSettings()
    )

    fun update(transform: (GameSettings) -> GameSettings) {
        persistenceScope.launch { repository.updateSettings(transform) }
    }

    fun resetProgress() {
        persistenceScope.launch { repository.resetProgress() }
    }
}
