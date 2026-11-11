package com.kdevs.lastsurvivor.game.entities

/**
 * The active ability of a character. [cooldownSeconds] is the wait after use; the numbers for what an
 * ability does (distance, radius, damage, duration) are in GameConfig (ABILITY_*).
 */
enum class AbilityType(val cooldownSeconds: Float) {
    /** Survivor: a short burst of extra running speed. */
    ADRENALINE(20f),
    /** Runner: a quick burst of movement during which nothing can hurt the player. */
    DASH(6f),
    /** Tank: a blast around the player that damages, shoves back and stuns zombies. */
    SHOCKWAVE(14f),
    /** Medic: heals over a few seconds and cures poison. */
    HEAL_PULSE(18f),
    /** Engineer: drops an auto-firing turret for a while. */
    TURRET(25f),
    /** Commando: rapid fire and double damage together for a few seconds. */
    RAGE(25f)
}
