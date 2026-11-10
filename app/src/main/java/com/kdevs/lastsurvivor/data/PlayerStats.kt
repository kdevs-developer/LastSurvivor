package com.kdevs.lastsurvivor.data

import com.kdevs.lastsurvivor.game.engine.RunTotals

/** Lifetime statistics shown on the Character screen. */
data class PlayerStats(
    val runs: Int = 0,
    val kills: Int = 0,
    val shotsFired: Int = 0,
    val shotsHit: Int = 0,
    val bossKills: Int = 0,
    val seconds: Int = 0,
    val coinsEarned: Int = 0
) {
    /** Hits as a whole percentage of shots fired, 0 when nothing has been fired. */
    val accuracyPercent: Int get() = if (shotsFired > 0) (shotsHit.toLong() * 100 / shotsFired).toInt() else 0
    val playHours: Int get() = seconds / 3600
    val playMinutes: Int get() = (seconds % 3600) / 60

    /** Adds one banked stretch of play. Saturates instead of overflowing. */
    operator fun plus(t: RunTotals) = PlayerStats(
        runs = runs,
        kills = add(kills, t.kills),
        shotsFired = add(shotsFired, t.shotsFired),
        shotsHit = add(shotsHit, t.shotsHit),
        bossKills = add(bossKills, t.bossKills),
        seconds = add(seconds, t.seconds),
        coinsEarned = add(coinsEarned, t.coins)
    )

    private fun add(a: Int, b: Int): Int = (a.toLong() + b.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
