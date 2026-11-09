package com.kdevs.lastsurvivor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
 * Phase 1 keeps persistence minimal (coins + highest wave) so the menu can show them.
 * Phase 10 extends this with weapons, upgrades, character, settings and statistics.
 */
class PlayerPrefsRepository(private val context: Context) {

    private object Keys {
        val COINS = intPreferencesKey("coins")
        val HIGHEST_WAVE = intPreferencesKey("highest_wave")
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
}
