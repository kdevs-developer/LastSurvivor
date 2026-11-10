package com.kdevs.lastsurvivor.ui.menu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kdevs.lastsurvivor.data.PlayerPrefsRepository
import com.kdevs.lastsurvivor.data.PlayerProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainMenuViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PlayerPrefsRepository(app)

    val progress: StateFlow<PlayerProgress> = repository.progress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerProgress()
    )

    /** The Sound switch on the main menu. */
    val soundEnabled: StateFlow<Boolean> = repository.soundEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    fun toggleSound() {
        viewModelScope.launch { repository.setSoundEnabled(!soundEnabled.value) }
    }
}
