package com.kdevs.lastsurvivor.game.engine

enum class WavePhase { COUNTDOWN, ACTIVE }

/**
 * Snapshot of what the Compose HUD and game-over screen show. Published by the game thread
 * through GameWorld.hud, and only when a value actually changes (so Compose never recomposes
 * 60 times a second).
 */
data class HudState(
    val health: Int = GameConfig.PLAYER_MAX_HEALTH.toInt(),
    val maxHealth: Int = GameConfig.PLAYER_MAX_HEALTH.toInt(),
    val poisoned: Boolean = false,
    val wave: Int = 1,
    val phase: WavePhase = WavePhase.COUNTDOWN,
    val countdownSeconds: Int = 0,
    /** Coins from clearing the previous wave; shown during the countdown, 0 before wave 1. */
    val clearedBonus: Int = 0,
    val zombiesLeft: Int = 0,
    val kills: Int = 0,
    val coins: Int = 0,
    val gameOver: Boolean = false
)
