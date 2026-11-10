package com.kdevs.lastsurvivor.ui.upgrades

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kdevs.lastsurvivor.data.PlayerPrefsRepository
import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.audio.SoundManager
import com.kdevs.lastsurvivor.game.weapons.UpgradeTrack
import com.kdevs.lastsurvivor.game.weapons.WeaponInventory
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UpgradesUiState(
    val coins: Int = 0,
    val inventory: WeaponInventory = WeaponInventory(),
    val selected: WeaponType = WeaponType.PISTOL
)

class UpgradesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PlayerPrefsRepository(app)
    private val selected = MutableStateFlow(WeaponType.PISTOL)
    private val sounds = SoundManager.get(app)

    val state: StateFlow<UpgradesUiState> = combine(
        repository.progress, repository.weapons, selected
    ) { progress, inventory, sel ->
        UpgradesUiState(progress.coins, inventory, sel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpgradesUiState())

    init {
        // Start on the weapon that is currently equipped.
        viewModelScope.launch { selected.value = repository.weapons.first().equipped }
    }

    fun select(type: WeaponType) {
        sounds.play(SoundEffect.UI_CLICK)
        selected.value = type
    }

    fun unlock(type: WeaponType) {
        viewModelScope.launch { purchaseSound(repository.unlockWeapon(type)) }
    }

    fun equip(type: WeaponType) {
        sounds.play(SoundEffect.UI_CLICK)
        viewModelScope.launch { repository.equipWeapon(type) }
    }

    fun upgrade(type: WeaponType, track: UpgradeTrack) {
        viewModelScope.launch { purchaseSound(repository.upgradeWeapon(type, track)) }
    }

    /** A cash-register ding when a purchase went through, a buzz when it was refused. */
    private fun purchaseSound(success: Boolean) {
        sounds.play(if (success) SoundEffect.UI_PURCHASE else SoundEffect.UI_DENIED)
    }
}
