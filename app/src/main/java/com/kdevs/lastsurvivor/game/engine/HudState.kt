package com.kdevs.lastsurvivor.game.engine

import com.kdevs.lastsurvivor.game.entities.AbilityType
import com.kdevs.lastsurvivor.game.entities.ZombieType
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
    val reloadPercent: Int = 0,
    // ---- Phase 8 ----
    val grenades: Int = GameConfig.GRENADE_START_COUNT,
    /** Whole seconds left on each buff, 0 when inactive (so the HUD updates once a second). */
    val rapidFireSeconds: Int = 0,
    val damageBoostSeconds: Int = 0,
    val shieldSeconds: Int = 0,
    /** True while the current wave (counting down or in progress) has a boss. */
    val bossWave: Boolean = false,
    /** The boss currently alive, or null. */
    val boss: ZombieType? = null,
    /** Boss health 0..100. */
    val bossPercent: Int = 0,
    // ---- Characters ----
    /** Diamonds picked up in this stretch of play. */
    val diamonds: Int = 0,
    /** The character's active ability. */
    val ability: AbilityType = AbilityType.ADRENALINE,
    /** 0..100 in steps of 5: how far the ability's cooldown has recovered. 100 = ready. */
    val abilityPercent: Int = 100,
    // ---- Modes ----
    /** Waves in the campaign level being played; 0 in the endless mode. */
    val totalWaves: Int = 0,
    /** Campaign level number; 0 in the endless mode. */
    val level: Int = 0,
    /** The last wave of the campaign level was cleared. */
    val levelComplete: Boolean = false
)
