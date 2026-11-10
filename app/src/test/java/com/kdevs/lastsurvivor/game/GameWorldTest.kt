package com.kdevs.lastsurvivor.game

import com.kdevs.lastsurvivor.game.engine.GameConfig
import com.kdevs.lastsurvivor.game.engine.GameSettings
import com.kdevs.lastsurvivor.game.engine.GameWorld
import com.kdevs.lastsurvivor.game.engine.GoreLevel
import com.kdevs.lastsurvivor.game.engine.RunTotals
import com.kdevs.lastsurvivor.game.engine.WavePhase
import com.kdevs.lastsurvivor.game.entities.CharacterSkin
import com.kdevs.lastsurvivor.game.entities.ZombieType
import com.kdevs.lastsurvivor.game.weapons.WeaponStats
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs the real simulation headlessly: no audio, no canvas, 60 ticks per simulated second. */
class GameWorldTest {

    private val dt = 1f / 60f

    private fun GameWorld.tick(seconds: Float) = repeat((seconds * 60f).toInt()) { update(dt) }

    private fun newWorld() = GameWorld().also { it.requestEquip(WeaponStats.of(WeaponType.PISTOL, emptyMap())) }

    // ---------------------------------------------------------------- waves

    @Test fun `a new run counts down, then starts wave 1`() {
        val w = newWorld()
        w.update(dt)
        assertEquals(1, w.hud.value.wave)
        assertEquals(WavePhase.COUNTDOWN, w.hud.value.phase)
        w.tick(GameConfig.FIRST_WAVE_COUNTDOWN + 1f)
        assertEquals(WavePhase.ACTIVE, w.hud.value.phase)
        assertTrue(w.hud.value.zombiesLeft > 0)
    }

    @Test fun `zombie types unlock at the documented waves`() {
        val unlock = ZombieType.entries.filter { !it.isBoss }.associate { it to it.unlockWave }
        assertEquals(1, unlock[ZombieType.WALKER])
        assertEquals(3, unlock[ZombieType.RUNNER])
        assertEquals(5, unlock[ZombieType.TOXIC])
        assertEquals(7, unlock[ZombieType.TANK])
    }

    // ---------------------------------------------------------------- continue (checkpoint)

    @Test fun `continue starts at the saved wave with a countdown`() {
        val w = newWorld()
        w.requestContinue(wave = 7, health = 80, grenades = 3)
        w.update(dt)
        val hud = w.hud.value
        assertEquals(7, hud.wave)
        assertEquals(WavePhase.COUNTDOWN, hud.phase)
        assertEquals(80, hud.health)
        assertEquals(3, hud.grenades)
        assertEquals(0, hud.kills)
        assertEquals(0, hud.coins)
    }

    @Test fun `continue never restores less than the minimum health`() {
        val w = newWorld()
        w.requestContinue(wave = 4, health = 3, grenades = 0)
        w.update(dt)
        assertEquals((GameConfig.PLAYER_MAX_HEALTH * GameConfig.CHECKPOINT_MIN_HEALTH_FRACTION).toInt(), w.hud.value.health)
    }

    @Test fun `continue clamps damaged values`() {
        val w = newWorld()
        w.requestContinue(wave = -5, health = 5000, grenades = 99)
        w.update(dt)
        assertEquals(1, w.hud.value.wave)
        assertEquals(GameConfig.PLAYER_MAX_HEALTH.toInt(), w.hud.value.health)
        assertEquals(GameConfig.GRENADE_MAX_COUNT, w.hud.value.grenades)
    }

    @Test fun `continuing at a boss wave spawns the first boss`() {
        val w = newWorld()
        w.requestContinue(wave = GameConfig.BOSS_WAVE_INTERVAL, health = 100, grenades = 2)
        w.tick(GameConfig.WAVE_COUNTDOWN + 0.5f)
        assertTrue(w.hud.value.bossWave)
        assertEquals(ZombieType.BRUTE, w.hud.value.boss)
        assertEquals(100, w.hud.value.bossPercent)
    }

    @Test fun `restart goes back to wave 1 and clears the totals`() {
        val w = newWorld()
        w.requestContinue(wave = 9, health = 100, grenades = 2)
        w.update(dt)
        w.requestRestart()
        w.update(dt)
        assertEquals(1, w.hud.value.wave)
        assertEquals(RunTotals(), w.runTotals())
    }

    // ---------------------------------------------------------------- totals for saving

    @Test fun `shooting a zombie shows up in the run totals`() {
        val w = newWorld()
        w.update(dt)
        w.spawnZombie(ZombieType.WALKER, w.player.x + 120f, w.player.y)
        w.input.fireHeld = true // the player faces +X, where the zombie is
        w.tick(2f)
        val t = w.runTotals()
        assertEquals(1, t.kills)
        assertEquals(ZombieType.WALKER.coinReward, t.coins)
        assertTrue(t.shotsFired >= 2)
        assertTrue(t.shotsHit >= 2)
        assertTrue(t.seconds in 1..2)
        assertEquals(0, t.bossKills)
    }

    @Test fun `time stops counting after the player dies`() {
        val w = newWorld()
        w.tick(1f)
        w.player.health = 0f
        w.update(dt)
        val atDeath = w.runTotals().seconds
        w.tick(5f)
        assertTrue(w.gameOver)
        assertTrue(w.hud.value.gameOver)
        assertEquals(atDeath, w.runTotals().seconds)
    }

    // ---------------------------------------------------------------- settings and outfit

    @Test fun `screen shake follows the setting`() {
        val on = newWorld()
        on.addShake(1f)
        on.update(dt)
        assertTrue(on.shakeX != 0f || on.shakeY != 0f)

        val off = newWorld()
        off.settings = GameSettings(screenShake = false)
        off.addShake(1f)
        off.update(dt)
        assertEquals(0f, off.shakeX, 0f)
        assertEquals(0f, off.shakeY, 0f)
    }

    @Test fun `gore setting scales the blood`() {
        fun particlesAfterKill(level: GoreLevel): Int {
            val w = newWorld()
            w.settings = GameSettings(gore = level)
            w.update(dt)
            w.spawnZombie(ZombieType.WALKER, w.player.x + 120f, w.player.y)
            w.input.fireHeld = true
            var peak = 0
            repeat(90) {
                w.update(dt)
                peak = maxOf(peak, w.particles.count)
            }
            return peak
        }
        assertTrue(particlesAfterKill(GoreLevel.OFF) < particlesAfterKill(GoreLevel.NORMAL))
    }

    @Test fun `the outfit is a plain property the renderer reads`() {
        val w = newWorld()
        assertEquals(CharacterSkin.SURVIVOR, w.skin)
        w.skin = CharacterSkin.SHADOW
        assertNotEquals(CharacterSkin.SURVIVOR, w.skin)
        assertFalse(w.gameOver)
    }
}
