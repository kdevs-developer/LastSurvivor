package com.kdevs.lastsurvivor.game.entities

/**
 * The active ability of a character. [cooldownSeconds] is the wait after use; the numbers for what an
 * ability does (distance, radius, damage, duration) are in GameConfig (ABILITY_*).
 */
enum class AbilityType(val cooldownSeconds: Float) {
    /** Survivor: a burst of extra running speed, plus a little healing. */
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
    RAGE(25f),
    /** Marksman: shots hit twice as hard and pierce extra zombies for a few seconds. */
    DEADEYE(22f),
    /** Cryo: freezes (stuns) every zombie in a wide area. */
    FREEZE(20f),
    /** Demolitionist: a rapid series of blasts on the zombies around. */
    AIRSTRIKE(28f),
    /** Scavenger: drops a few random power-ups around the player. */
    JACKPOT(30f)
}
