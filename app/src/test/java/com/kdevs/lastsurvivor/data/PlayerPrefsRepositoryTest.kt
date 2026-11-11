package com.kdevs.lastsurvivor.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kdevs.lastsurvivor.game.engine.GameSettings
import com.kdevs.lastsurvivor.game.engine.GoreLevel
import com.kdevs.lastsurvivor.game.engine.RunTotals
import com.kdevs.lastsurvivor.game.entities.GameCharacter
import com.kdevs.lastsurvivor.game.weapons.UpgradeConfig
import com.kdevs.lastsurvivor.game.weapons.UpgradeTrack
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Uses a real DataStore in a temporary file, so the saved format and the transactions are exercised. */
class PlayerPrefsRepositoryTest {

    @get:Rule val folder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var repo: PlayerPrefsRepository

    @Before fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "test.preferences_pb") }
        repo = PlayerPrefsRepository(store)
    }

    @After fun tearDown() { scope.cancel() }

    private val totals = RunTotals(coins = 25, kills = 9, shotsFired = 40, shotsHit = 30, bossKills = 1, seconds = 75)

    @Test fun `defaults on an empty store`() = runBlocking {
        assertEquals(PlayerProgress(), repo.progress.first())
        assertEquals(PlayerStats(), repo.stats.first())
        assertEquals(GameSettings(), repo.settings.first())
        assertEquals(GameCharacter.SURVIVOR, repo.character.first())
        assertEquals(CharacterInventory(), repo.characters.first())
        assertNull(repo.checkpoint.first())
    }

    @Test fun `banking a run adds coins, stats and the best wave in one go`() = runBlocking {
        repo.bankRun(totals, wave = 6)
        repo.bankRun(totals.copy(coins = 5), wave = 4) // a lower wave does not lower the best
        assertEquals(PlayerProgress(coins = 30, highestWave = 6), repo.progress.first())
        val s = repo.stats.first()
        assertEquals(18, s.kills)
        assertEquals(80, s.shotsFired)
        assertEquals(2, s.bossKills)
        assertEquals(150, s.seconds)
        assertEquals(30, s.coinsEarned)
    }

    @Test fun `the checkpoint is stored, kept by later banks, and cleared on request`() = runBlocking {
        val cp = RunCheckpoint(wave = 8, health = 70, grenades = 3)
        repo.bankRun(totals, wave = 8, checkpoint = cp)
        assertEquals(cp, repo.checkpoint.first())

        repo.bankRun(RunTotals(), wave = 8) // no checkpoint given: the stored one stays
        assertEquals(cp, repo.checkpoint.first())

        repo.bankRun(RunTotals(), wave = 8, clearCheckpoint = true)
        assertNull(repo.checkpoint.first())
    }

    @Test fun `a fresh run counts and clears the checkpoint, a continued run does not`() = runBlocking {
        repo.bankRun(RunTotals(), wave = 3, checkpoint = RunCheckpoint(3, 90, 2))
        repo.beginRun(fresh = false)
        assertEquals(0, repo.stats.first().runs)
        assertEquals(RunCheckpoint(3, 90, 2), repo.checkpoint.first())

        repo.beginRun(fresh = true)
        assertEquals(1, repo.stats.first().runs)
        assertNull(repo.checkpoint.first())
    }

    @Test fun `a damaged checkpoint is ignored`() = runBlocking {
        store.edit { it[stringPreferencesKey("run_checkpoint")] = "not a checkpoint" }
        assertNull(repo.checkpoint.first())
    }

    @Test fun `settings round trip and sound keeps working`() = runBlocking {
        repo.updateSettings { it.copy(screenShake = false, gore = GoreLevel.LOW, damageNumbers = false, showFps = true) }
        val s = repo.settings.first()
        assertFalse(s.screenShake)
        assertEquals(GoreLevel.LOW, s.gore)
        assertFalse(s.damageNumbers)
        assertTrue(s.showFps)
        assertTrue(s.soundEnabled)

        repo.setSoundEnabled(false)
        assertFalse(repo.soundEnabled.first())
        assertEquals(GoreLevel.LOW, repo.settings.first().gore) // other options untouched
    }

    @Test fun `an unknown gore value falls back to normal`() = runBlocking {
        store.edit { it[stringPreferencesKey("gore_level")] = "EXTREME" }
        assertEquals(GoreLevel.NORMAL, repo.settings.first().gore)
    }

    @Test fun `banking a run adds the diamonds found`() = runBlocking {
        repo.bankRun(RunTotals(diamonds = 7), wave = 3)
        repo.bankRun(RunTotals(diamonds = 5), wave = 3)
        assertEquals(12, repo.progress.first().diamonds)
    }

    @Test fun `a character is bought with diamonds once`() = runBlocking {
        repo.bankRun(RunTotals(diamonds = 60), wave = 1)
        assertTrue(repo.unlockCharacter(GameCharacter.RUNNER)) // costs 50
        assertFalse(repo.unlockCharacter(GameCharacter.RUNNER)) // already owned
        assertEquals(10, repo.progress.first().diamonds)
        assertFalse(repo.unlockCharacter(GameCharacter.TANK)) // cannot afford
        assertEquals(10, repo.progress.first().diamonds)
        assertTrue(GameCharacter.RUNNER in repo.characters.first().unlocked)
        assertFalse(GameCharacter.TANK in repo.characters.first().unlocked)
    }

    @Test fun `only an owned character can be chosen`() = runBlocking {
        repo.selectCharacter(GameCharacter.COMMANDO) // not owned: ignored
        assertEquals(GameCharacter.SURVIVOR, repo.character.first())

        repo.bankRun(RunTotals(diamonds = 50), wave = 1)
        repo.unlockCharacter(GameCharacter.RUNNER)
        repo.selectCharacter(GameCharacter.RUNNER)
        assertEquals(GameCharacter.RUNNER, repo.character.first())

        repo.selectCharacter(GameCharacter.SURVIVOR) // always owned
        assertEquals(GameCharacter.SURVIVOR, repo.character.first())
    }

    @Test fun `a saved choice that is not owned falls back to Survivor`() = runBlocking {
        store.edit { it[stringPreferencesKey("selected_character")] = GameCharacter.TANK.name }
        assertEquals(GameCharacter.SURVIVOR, repo.character.first())
    }

    @Test fun `damaged numbers are clamped when read`() = runBlocking {
        store.edit {
            it[intPreferencesKey("coins")] = -500
            it[intPreferencesKey("stat_kills")] = -1
            it[intPreferencesKey("weapon_pistol_damage")] = 99
        }
        assertEquals(0, repo.progress.first().coins)
        assertEquals(0, repo.stats.first().kills)
        assertEquals(UpgradeConfig.MAX_LEVEL, repo.weapons.first().level(WeaponType.PISTOL, UpgradeTrack.DAMAGE))
    }

    @Test fun `buying a weapon spends the coins once`() = runBlocking {
        repo.bankRun(RunTotals(coins = 150), wave = 1)
        assertTrue(repo.unlockWeapon(WeaponType.SMG))
        assertFalse(repo.unlockWeapon(WeaponType.SMG)) // already owned
        assertEquals(30, repo.progress.first().coins)
        assertFalse(repo.unlockWeapon(WeaponType.RIFLE)) // cannot afford
        assertEquals(30, repo.progress.first().coins)
    }

    @Test fun `reset erases progress but keeps the options`() = runBlocking {
        repo.bankRun(totals.copy(coins = 200, diamonds = 80), wave = 5, checkpoint = RunCheckpoint(5, 60, 1))
        repo.unlockCharacter(GameCharacter.RUNNER)
        repo.selectCharacter(GameCharacter.RUNNER)
        repo.unlockWeapon(WeaponType.SMG)
        repo.updateSettings { it.copy(soundEnabled = false, gore = GoreLevel.OFF) }

        repo.resetProgress()

        assertEquals(PlayerProgress(), repo.progress.first())
        assertEquals(PlayerStats(), repo.stats.first())
        assertEquals(CharacterInventory(), repo.characters.first())
        assertNull(repo.checkpoint.first())
        assertFalse(repo.weapons.first().owns(WeaponType.SMG))
        val s = repo.settings.first()
        assertFalse(s.soundEnabled)
        assertEquals(GoreLevel.OFF, s.gore)
    }
}
