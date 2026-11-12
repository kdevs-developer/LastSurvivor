package com.kdevs.lastsurvivor.ui.mode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kdevs.lastsurvivor.data.PlayerPrefsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Campaign progress for the mode select screen. */
class ModeSelectViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PlayerPrefsRepository(app)

    /** The highest campaign level cleared (0 = none); the next level is unlocked. */
    val cleared: StateFlow<Int> = repository.campaignCleared.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}
