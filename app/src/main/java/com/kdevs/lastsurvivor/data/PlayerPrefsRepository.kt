package com.kdevs.lastsurvivor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kdevs.lastsurvivor.game.engine.GameSettings
import com.kdevs.lastsurvivor.game.engine.Campaign
import com.kdevs.lastsurvivor.game.engine.GoreLevel
import com.kdevs.lastsurvivor.game.engine.RunTotals
import com.kdevs.lastsurvivor.game.entities.GameCharacter
import com.kdevs.lastsurvivor.game.weapons.UpgradeConfig
import com.kdevs.lastsurvivor.game.weapons.UpgradeTrack
import com.kdevs.lastsurvivor.game.weapons.WeaponInventory
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

// A damaged file is replaced by an empty one instead of crashing the app on every launch.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "last_survivor_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

/**
 * Scope for saves that must finish even if the screen that asked for them is already gone (for
 * example Quit to Menu: the game screen's ViewModel is cleared right after). Lives as long as the process.
 */
val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

data class PlayerProgress(
    val coins: Int = 0,
    val highestWave: Int = 0,
    val diamonds: Int = 0
)

/** Which characters the player owns and which one is chosen for the next run. */
data class CharacterInventory(
    val unlocked: Set<GameCharacter> = setOf(GameCharacter.SURVIVOR),
    val selected: GameCharacter = GameCharacter.SURVIVOR
)

/**
 * Everything the game saves, in one DataStore file:
 *  - progress: coins and the highest wave (Phase 1)
 *  - weapons: unlocked, equipped and upgrade levels (Phase 7)
 *  - settings: sound (Phase 9), screen shake, gore, damage numbers, FPS counter (Phase 10)
 *  - characters: diamonds, unlocked characters and the chosen one
 *  - stats: lifetime totals (Phase 10)
 *  - checkpoint: the start of the current wave, for Continue (Phase 10)
 *
 * Reads clamp or ignore damaged values, so a bad entry can never crash the game or hand out
 * coins. Every change that touches several values is one transaction ([bankRun], purchases).
 * The constructor taking a [DataStore] exists so tests can use a temporary file.
 */
class PlayerPrefsRepository(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    private object Keys {
        val COINS = intPreferencesKey("coins")
        val HIGHEST_WAVE = intPreferencesKey("highest_wave")
        val UNLOCKED_WEAPONS = stringSetPreferencesKey("unlocked_weapons")
        val EQUIPPED_WEAPON = stringPreferencesKey("equipped_weapon")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SCREEN_SHAKE = booleanPreferencesKey("screen_shake")
        val GORE = stringPreferencesKey("gore_level")
        val DAMAGE_NUMBERS = booleanPreferencesKey("damage_numbers")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val DIAMONDS = intPreferencesKey("diamonds")
        val UNLOCKED_CHARACTERS = stringSetPreferencesKey("unlocked_characters")
        val SELECTED_CHARACTER = stringPreferencesKey("selected_character")
        val CHECKPOINT = stringPreferencesKey("run_checkpoint")
        val CAMPAIGN_CLEARED = intPreferencesKey("campaign_cleared")
        val STAT_RUNS = intPreferencesKey("stat_runs")
        val STAT_KILLS = intPreferencesKey("stat_kills")
        val STAT_SHOTS = intPreferencesKey("stat_shots_fired")
        val STAT_HITS = intPreferencesKey("stat_shots_hit")
        val STAT_BOSSES = intPreferencesKey("stat_boss_kills")
        val STAT_SECONDS = intPreferencesKey("stat_seconds")
        val STAT_COINS = intPreferencesKey("stat_coins_earned")

        /** Keys that survive "Reset progress": options are not progress. */
        val KEPT_ON_RESET = setOf(SOUND_ENABLED.name, SCREEN_SHAKE.name, GORE.name, DAMAGE_NUMBERS.name, SHOW_FPS.name)

        fun level(type: WeaponType, track: UpgradeTrack) =
            intPreferencesKey("weapon_${type.name.lowercase()}_${track.name.lowercase()}")
    }

    /** The stored preferences; an unreadable file is treated as empty. */
    private val data: Flow<Preferences> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    val progress: Flow<PlayerProgress> = data
        .map {
            PlayerProgress(
                coins = (it[Keys.COINS] ?: 0).coerceAtLeast(0),
                highestWave = (it[Keys.HIGHEST_WAVE] ?: 0).coerceAtLeast(0),
                diamonds = (it[Keys.DIAMONDS] ?: 0).coerceAtLeast(0)
            )
        }
        .distinctUntilChanged()

    suspend fun addCoins(amount: Int) {
        store.edit { it[Keys.COINS] = (it[Keys.COINS] ?: 0) + amount }
    }

    suspend fun updateHighestWave(wave: Int) {
        store.edit {
            if (wave > (it[Keys.HIGHEST_WAVE] ?: 0)) it[Keys.HIGHEST_WAVE] = wave
        }
    }

    // ---------------- Settings (Phase 9 sound, Phase 10 the rest) ----------------

    private fun Preferences.toSettings() = GameSettings(
        soundEnabled = this[Keys.SOUND_ENABLED] ?: true,
        screenShake = this[Keys.SCREEN_SHAKE] ?: true,
        gore = GoreLevel.entries.firstOrNull { it.name == this[Keys.GORE] } ?: GoreLevel.NORMAL,
        damageNumbers = this[Keys.DAMAGE_NUMBERS] ?: true,
        showFps = this[Keys.SHOW_FPS] ?: false
    )

    val settings: Flow<GameSettings> = data.map { it.toSettings() }.distinctUntilChanged()

    /** Changes the options with [transform], which receives the current ones. */
    suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        store.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.SOUND_ENABLED] = s.soundEnabled
            prefs[Keys.SCREEN_SHAKE] = s.screenShake
            prefs[Keys.GORE] = s.gore.name
            prefs[Keys.DAMAGE_NUMBERS] = s.damageNumbers
            prefs[Keys.SHOW_FPS] = s.showFps
        }
    }

    /** The sound effects switch (menu and pause screen). On by default. */
    val soundEnabled: Flow<Boolean> = data.map { it[Keys.SOUND_ENABLED] ?: true }.distinctUntilChanged()

    suspend fun setSoundEnabled(enabled: Boolean) = updateSettings { it.copy(soundEnabled = enabled) }

    // ---------------- Characters and diamonds ----------------

    private fun Preferences.toCharacters(): CharacterInventory {
        val unlocked = (this[Keys.UNLOCKED_CHARACTERS] ?: emptySet())
            .mapNotNull { name -> GameCharacter.entries.firstOrNull { it.name == name } }
            .toSet() + GameCharacter.SURVIVOR
        val selected = GameCharacter.entries.firstOrNull { it.name == this[Keys.SELECTED_CHARACTER] }
            ?.takeIf { it in unlocked } ?: GameCharacter.SURVIVOR
        return CharacterInventory(unlocked, selected)
    }

    /** Owned characters (Survivor always) and the chosen one; an unowned or unknown choice falls back to Survivor. */
    val characters: Flow<CharacterInventory> = data.map { it.toCharacters() }.distinctUntilChanged()

    /** The character to play in the next run. */
    val character: Flow<GameCharacter> = data.map { it.toCharacters().selected }.distinctUntilChanged()

    /** Buys [character] with diamonds if the player can afford it and does not own it yet. Returns true on success. */
    suspend fun unlockCharacter(character: GameCharacter): Boolean {
        var bought = false
        store.edit { prefs ->
            val owned = prefs.toCharacters().unlocked
            val diamonds = (prefs[Keys.DIAMONDS] ?: 0).coerceAtLeast(0)
            if (character !in owned && diamonds >= character.diamondPrice) {
                prefs[Keys.DIAMONDS] = diamonds - character.diamondPrice
                prefs[Keys.UNLOCKED_CHARACTERS] = (owned + character).map { it.name }.toSet()
                bought = true
            }
        }
        return bought
    }

    /** Chooses an owned character for the next run; ignored if the player does not own it. */
    suspend fun selectCharacter(character: GameCharacter) {
        store.edit { prefs ->
            if (character in prefs.toCharacters().unlocked) prefs[Keys.SELECTED_CHARACTER] = character.name
        }
    }

    // ---------------- Statistics and run saving (Phase 10) ----------------

    private fun Preferences.toStats() = PlayerStats(
        runs = (this[Keys.STAT_RUNS] ?: 0).coerceAtLeast(0),
        kills = (this[Keys.STAT_KILLS] ?: 0).coerceAtLeast(0),
        shotsFired = (this[Keys.STAT_SHOTS] ?: 0).coerceAtLeast(0),
        shotsHit = (this[Keys.STAT_HITS] ?: 0).coerceAtLeast(0),
        bossKills = (this[Keys.STAT_BOSSES] ?: 0).coerceAtLeast(0),
        seconds = (this[Keys.STAT_SECONDS] ?: 0).coerceAtLeast(0),
        coinsEarned = (this[Keys.STAT_COINS] ?: 0).coerceAtLeast(0)
    )

    private fun MutablePreferences.putStats(s: PlayerStats) {
        this[Keys.STAT_RUNS] = s.runs
        this[Keys.STAT_KILLS] = s.kills
        this[Keys.STAT_SHOTS] = s.shotsFired
        this[Keys.STAT_HITS] = s.shotsHit
        this[Keys.STAT_BOSSES] = s.bossKills
        this[Keys.STAT_SECONDS] = s.seconds
        this[Keys.STAT_COINS] = s.coinsEarned
    }

    val stats: Flow<PlayerStats> = data.map { it.toStats() }.distinctUntilChanged()

    // ---------------- Campaign ----------------

    /** The highest campaign level cleared so far (0 = none). Level [campaignCleared] + 1 is the next one to unlock. */
    val campaignCleared: Flow<Int> = data
        .map { (it[Keys.CAMPAIGN_CLEARED] ?: 0).coerceIn(0, Campaign.count) }
        .distinctUntilChanged()

    /** Records that [level] was cleared; never lowers the progress. */
    suspend fun completeLevel(level: Int) {
        store.edit { prefs ->
            val cleared = (prefs[Keys.CAMPAIGN_CLEARED] ?: 0).coerceIn(0, Campaign.count)
            if (level.coerceIn(0, Campaign.count) > cleared) prefs[Keys.CAMPAIGN_CLEARED] = level.coerceIn(0, Campaign.count)
        }
    }

    /** The saved wave to continue from, or null if there is none (or it is damaged). */
    val checkpoint: Flow<RunCheckpoint?> = data.map { RunCheckpoint.decode(it[Keys.CHECKPOINT]) }.distinctUntilChanged()

    /**
     * A new run begins. [fresh] runs (Play, Retry) count as a run played and throw away any saved
     * checkpoint; a continued run does neither.
     */
    suspend fun beginRun(fresh: Boolean) {
        if (!fresh) return
        store.edit { prefs ->
            val current = prefs.toStats()
            prefs.putStats(current.copy(runs = if (current.runs == Int.MAX_VALUE) current.runs else current.runs + 1))
            prefs.remove(Keys.CHECKPOINT)
        }
    }

    /**
     * Banks what [delta] says was earned since the last bank: coins, lifetime stats and the highest
     * wave, in one transaction. Then either stores [checkpoint], clears it ([clearCheckpoint]) or
     * leaves the stored one alone.
     */
    suspend fun bankRun(delta: RunTotals, wave: Int, checkpoint: RunCheckpoint? = null, clearCheckpoint: Boolean = false) {
        store.edit { prefs ->
            prefs[Keys.COINS] = ((prefs[Keys.COINS] ?: 0).coerceAtLeast(0).toLong() + delta.coins).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            if (delta.diamonds > 0) {
                prefs[Keys.DIAMONDS] = ((prefs[Keys.DIAMONDS] ?: 0).coerceAtLeast(0).toLong() + delta.diamonds).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
            if (wave > (prefs[Keys.HIGHEST_WAVE] ?: 0)) prefs[Keys.HIGHEST_WAVE] = wave
            prefs.putStats(prefs.toStats() + delta)
            when {
                clearCheckpoint -> prefs.remove(Keys.CHECKPOINT)
                checkpoint != null -> prefs[Keys.CHECKPOINT] = checkpoint.encode()
            }
        }
    }

    /** Erases coins, diamonds, best wave, weapons, characters, statistics and the checkpoint. Options are kept. */
    suspend fun resetProgress() {
        store.edit { prefs ->
            val doomed = prefs.asMap().keys.filter { it.name !in Keys.KEPT_ON_RESET }
            doomed.forEach { prefs.remove(it) }
        }
    }

    // ---------------- Weapons (Phase 7) ----------------

    val weapons: Flow<WeaponInventory> = data
        .map { prefs ->
            val unlocked = (prefs[Keys.UNLOCKED_WEAPONS] ?: emptySet())
                .mapNotNull { name -> WeaponType.entries.firstOrNull { it.name == name } }
                .toSet() + WeaponType.PISTOL
            val equipped = WeaponType.entries.firstOrNull { it.name == prefs[Keys.EQUIPPED_WEAPON] }
                ?.takeIf { it in unlocked } ?: WeaponType.PISTOL
            val levels = WeaponType.entries.associateWith { type ->
                UpgradeTrack.entries.associateWith { track -> (prefs[Keys.level(type, track)] ?: 0).coerceIn(0, UpgradeConfig.MAX_LEVEL) }
            }
            WeaponInventory(unlocked, equipped, levels)
        }

    /** Buys [type] if the player has enough coins and does not own it yet. Returns true on success. */
    suspend fun unlockWeapon(type: WeaponType): Boolean {
        var bought = false
        store.edit { prefs ->
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
        store.edit { prefs ->
            val unlocked = prefs[Keys.UNLOCKED_WEAPONS] ?: emptySet()
            if (type == WeaponType.PISTOL || type.name in unlocked) prefs[Keys.EQUIPPED_WEAPON] = type.name
        }
    }

    /** Buys the next level of [track] for an owned weapon. Returns true on success. */
    suspend fun upgradeWeapon(type: WeaponType, track: UpgradeTrack): Boolean {
        var upgraded = false
        store.edit { prefs ->
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
