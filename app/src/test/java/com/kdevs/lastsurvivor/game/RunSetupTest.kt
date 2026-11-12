package com.kdevs.lastsurvivor.game

import com.kdevs.lastsurvivor.data.RunCheckpoint
import com.kdevs.lastsurvivor.game.engine.Campaign
import com.kdevs.lastsurvivor.game.engine.GameArea
import com.kdevs.lastsurvivor.game.engine.GameMode
import com.kdevs.lastsurvivor.game.engine.RunSetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSetupTest {

    @Test fun `every campaign level and every infinite area survives encode and decode`() {
        for (n in 1..Campaign.count) assertEquals(RunSetup.campaign(n), RunSetup.decode(RunSetup.campaign(n).encode()))
        for (a in GameArea.entries) assertEquals(RunSetup.infinite(a), RunSetup.decode(RunSetup.infinite(a).encode()))
    }

    @Test fun `bad setup text is rejected`() {
        assertNull(RunSetup.decode(null))
        assertNull(RunSetup.decode(""))
        assertNull(RunSetup.decode("C0"))
        assertNull(RunSetup.decode("C11"))
        assertNull(RunSetup.decode("Cx"))
        assertNull(RunSetup.decode("I-MOON"))
        assertNull(RunSetup.decode("Z3"))
    }

    @Test fun `encoded text is safe in a route and a checkpoint`() {
        for (s in Campaign.levels.map { RunSetup.campaign(it.number) } + GameArea.entries.map { RunSetup.infinite(it) }) {
            assertTrue(s.encode().none { it == '|' || it == '/' || it == ' ' })
        }
    }

    @Test fun `campaign levels use their area and wave count, endless has no end`() {
        val level3 = RunSetup.campaign(3)
        assertEquals(Campaign.level(3).area, level3.area)
        assertEquals(Campaign.level(3).waves, level3.totalWaves)
        assertEquals(GameArea.SWAMP, RunSetup.infinite(GameArea.SWAMP).area)
        assertEquals(0, RunSetup.infinite(GameArea.SWAMP).totalWaves)
        assertEquals(GameMode.INFINITE, RunSetup.DEFAULT.mode)
    }

    @Test fun `later levels start tougher and pay more`() {
        assertEquals(0, Campaign.level(1).difficultyOffset)
        assertTrue(Campaign.level(5).difficultyOffset > Campaign.level(2).difficultyOffset)
        assertTrue(Campaign.level(5).bonusCoins > Campaign.level(1).bonusCoins)
    }

    @Test fun `checkpoint keeps the level or area`() {
        val cp = RunCheckpoint(wave = 4, health = 70, grenades = 2, setup = RunSetup.campaign(6))
        assertEquals(cp, RunCheckpoint.decode(cp.encode()))
        val endless = RunCheckpoint(wave = 9, health = 40, grenades = 1, setup = RunSetup.infinite(GameArea.TUNDRA))
        assertEquals(endless, RunCheckpoint.decode(endless.encode()))
    }

    @Test fun `an old v1 checkpoint loads as endless in the city`() {
        assertEquals(RunCheckpoint(7, 55, 3, RunSetup.DEFAULT), RunCheckpoint.decode("v1|7|55|3"))
    }

    @Test fun `a v2 checkpoint with a bad setup is rejected`() {
        assertNull(RunCheckpoint.decode("v2|3|50|2|Q9"))
        assertNull(RunCheckpoint.decode("v2|3|50|2"))
    }

    @Test fun `an infinite area opens when the campaign reaches it`() {
        assertTrue(Campaign.isAreaUnlocked(GameArea.CITY, 1))
        assertFalse(Campaign.isAreaUnlocked(GameArea.SWAMP, 2))
        assertTrue(Campaign.isAreaUnlocked(GameArea.SWAMP, 3))
        assertFalse(Campaign.isAreaUnlocked(GameArea.GRAVEYARD, 8))
        assertTrue(Campaign.isAreaUnlocked(GameArea.GRAVEYARD, 9))
        for (area in GameArea.entries) assertTrue(Campaign.isAreaUnlocked(area, Campaign.count))
    }
}
