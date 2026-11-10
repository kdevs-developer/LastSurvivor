package com.kdevs.lastsurvivor.data

import com.kdevs.lastsurvivor.game.engine.RunTotals
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerStatsTest {

    @Test fun `adding a run accumulates every total but not the run count`() {
        val before = PlayerStats(runs = 2, kills = 10, shotsFired = 100, shotsHit = 40, bossKills = 1, seconds = 90, coinsEarned = 50)
        val after = before + RunTotals(coins = 7, kills = 5, shotsFired = 30, shotsHit = 20, bossKills = 1, seconds = 30)
        assertEquals(PlayerStats(runs = 2, kills = 15, shotsFired = 130, shotsHit = 60, bossKills = 2, seconds = 120, coinsEarned = 57), after)
    }

    @Test fun `adding saturates instead of overflowing`() {
        val after = PlayerStats(kills = Int.MAX_VALUE - 1) + RunTotals(kills = 10)
        assertEquals(Int.MAX_VALUE, after.kills)
    }

    @Test fun `accuracy is a whole percentage and safe with no shots`() {
        assertEquals(0, PlayerStats().accuracyPercent)
        assertEquals(33, PlayerStats(shotsFired = 3, shotsHit = 1).accuracyPercent)
        assertEquals(100, PlayerStats(shotsFired = 8, shotsHit = 8).accuracyPercent)
    }

    @Test fun `accuracy does not overflow with huge totals`() {
        assertEquals(50, PlayerStats(shotsFired = 2_000_000_000, shotsHit = 1_000_000_000).accuracyPercent)
    }

    @Test fun `play time splits into hours and minutes`() {
        assertEquals(0, PlayerStats(seconds = 59).playMinutes)
        val s = PlayerStats(seconds = 2 * 3600 + 5 * 60 + 30)
        assertEquals(2, s.playHours)
        assertEquals(5, s.playMinutes)
    }

    @Test fun `RunTotals minus gives what is new and never goes negative`() {
        val now = RunTotals(coins = 30, kills = 12, shotsFired = 80, shotsHit = 30, bossKills = 1, seconds = 100)
        val earlier = RunTotals(coins = 10, kills = 4, shotsFired = 50, shotsHit = 10, bossKills = 0, seconds = 40)
        assertEquals(RunTotals(coins = 20, kills = 8, shotsFired = 30, shotsHit = 20, bossKills = 1, seconds = 60), now - earlier)
        // the engine restarted from zero after the last bank: no negative amounts
        assertEquals(RunTotals(), RunTotals() - now)
    }
}
