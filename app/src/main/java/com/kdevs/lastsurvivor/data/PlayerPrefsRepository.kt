package com.kdevs.lastsurvivor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kdevs.lastsurvivor.game.weapons.UpgradeConfig
import com.kdevs.lastsurvivor.game.weapons.UpgradeTrack
import com.kdevs.lastsurvivor.game.weapons.WeaponInventory
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "last_survivor_prefs")

data class PlayerProgress(
    val coins: Int = 0,
    val highestWave: Int = 0
)

/**
 * Persistence so far: coins, highest wave (Phase 1) and the weapon inventory (Phase 7: unlocked
 * weapons, equipped weapon, upgrade levels). Phase 10 extends this with character, settings and
 * statistics, and hardens it.
 */
class PlayerPrefsRepository(private val context: Context) {

    private object Keys {
        val COINS = intPreferencesKey("coins")
        val HIGHEST_WAVE = intPreferencesKey("highest_wave")
        val UNLOCKED_WEAPONS = stringSetPreferencesKey("unlocked_weapons")
        val EQUIPPED_WEAPON = stringPreferencesKey("equipped_weapon")
        fun level(type: WeaponType, track: UpgradeTrack) =
            intPreferencesKey("weapon_${type.name.lowercase()}_${track.name.lowercase()}")
    }

    val progress: Flow<PlayerProgress> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { PlayerProgress(coins = it[Keys.COINS] ?: 0, highestWave = it[Keys.HIGHEST_WAVE] ?: 0) }

    suspend fun addCoins(amount: Int) {
        context.dataStore.edit { it[Keys.COINS] = (it[Keys.COINS] ?: 0) + amount }
    }

    suspend fun updateHighestWave(wave: Int) {
        context.dataStore.edit {
            if (wave > (it[Keys.HIGHEST_WAVE] ?: 0)) it[Keys.HIGHEST_WAVE] = wave
        }
    }

    // ---------------- Weapons (Phase 7) ----------------

    val weapons: Flow<WeaponInventory> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val unlocked = (prefs[Keys.UNLOCKED_WEAPONS] ?: emptySet())
                .mapNotNull { name -> WeaponType.entries.firstOrNull { it.name == name } }
                .toSet() + WeaponType.PISTOL
            val equipped = WeaponType.entries.firstOrNull { it.name == prefs[Keys.EQUIPPED_WEAPON] }
                ?.takeIf { it in unlocked } ?: WeaponType.PISTOL
            val levels = WeaponType.entries.associateWith { type ->
                UpgradeTrack.entries.associateWith { track -> prefs[Keys.level(type, track)] ?: 0 }
            }
            WeaponInventory(unlocked, equipped, levels)
        }

    /** Buys [type] if the player has enough coins and does not own it yet. Returns true on success. */
    suspend fun unlockWeapon(type: WeaponType): Boolean {
        var bought = false
        context.dataStore.edit { prefs ->
            val unlocked = prefs[Keys.UNLOCKED_WEAPONS] ?: emptySet()
            val coins = prefs[Keys.COINS] ?: 0
            if (type != WeaponType.PISTOL && type.name !in unlocked && coins >= type.price) {
                prefs[Keys.COINS] = coins - type.price
                prefs[Keys.UNLOCKED_WEAPONS] = unlocked + type.name
                bought = true
            }
        }
        return bought
    }

    /** Equips an owned weapon; ignored if the player does not own it. */
    suspend fun equipWeapon(type: WeaponType) {
        context.dataStore.edit { prefs ->
            val unlocked = prefs[Keys.UNLOCKED_WEAPONS] ?: emptySet()
            if (type == WeaponType.PISTOL || type.name in unlocked) prefs[Keys.EQUIPPED_WEAPON] = type.name
        }
    }

    /** Buys the next level of [track] for an owned weapon. Returns true on success. */
    suspend fun upgradeWeapon(type: WeaponType, track: UpgradeTrack): Boolean {
        var upgraded = false
        context.dataStore.edit { prefs ->
            val unlocked = prefs[Keys.UNLOCKED_WEAPONS] ?: emptySet()
            val owned = type == WeaponType.PISTOL || type.name in unlocked
            val key = Keys.level(type, track)
            val level = prefs[key] ?: 0
            val cost = UpgradeConfig.cost(type, level)
            val coins = prefs[Keys.COINS] ?: 0
            if (owned && level < UpgradeConfig.MAX_LEVEL && coins >= cost) {
                prefs[Keys.COINS] = coins - cost
                prefs[key] = level + 1
                upgraded = true
            }
        }
        return upgraded
    }
}
