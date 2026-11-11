package com.kdevs.lastsurvivor.game.engine

/**
 * What one stretch of play has produced so far. The engine counts it from zero each time a run
 * starts or is continued; the app banks only the difference since the last bank ([minus]), so
 * nothing is saved twice.
 */
data class RunTotals(
    val coins: Int = 0,
    val kills: Int = 0,
    val shotsFired: Int = 0,
    val shotsHit: Int = 0,
    val bossKills: Int = 0,
    val seconds: Int = 0,
    val diamonds: Int = 0
) {
    /** What happened since [earlier]. Never negative, even if the engine was reset in between. */
    operator fun minus(earlier: RunTotals) = RunTotals(
        coins = (coins - earlier.coins).coerceAtLeast(0),
        kills = (kills - earlier.kills).coerceAtLeast(0),
        shotsFired = (shotsFired - earlier.shotsFired).coerceAtLeast(0),
        shotsHit = (shotsHit - earlier.shotsHit).coerceAtLeast(0),
        bossKills = (bossKills - earlier.bossKills).coerceAtLeast(0),
        seconds = (seconds - earlier.seconds).coerceAtLeast(0),
        diamonds = (diamonds - earlier.diamonds).coerceAtLeast(0)
    )
}
