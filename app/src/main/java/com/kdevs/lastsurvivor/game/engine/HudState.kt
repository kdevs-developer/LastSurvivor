package com.kdevs.lastsurvivor.game.engine

import com.kdevs.lastsurvivor.game.weapons.WeaponType

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
    val gameOver: Boolean = false,
    val weapon: WeaponType = WeaponType.PISTOL,
    val ammo: Int = 0,
    val magazine: Int = 0,
    val reloading: Boolean = false,
    /** 0..100 in steps of 5, so the HUD recomposes at most 20 times per reload. */
    val reloadPercent: Int = 0
)
