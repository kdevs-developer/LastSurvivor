package com.kdevs.lastsurvivor.game.entities

/**
 * A character's passive skill: always on, no button. Each character has exactly one. What each one does
 * in numbers is in GameConfig (the passive constants), and the engine applies them (GameWorld, Player).
 */
enum class PassiveType {
    /** Survivor: the first deadly hit of a run is survived with part of the health back. */
    LAST_STAND,
    /** Runner: some zombie swings miss completely. */
    NIMBLE,
    /** Tank: a zombie whose swing lands is hurt in return. */
    THORNS,
    /** Medic: every kill restores a little health. */
    TRIAGE,
    /** Engineer: abilities recharge faster. */
    OVERCLOCK,
    /** Commando: all weapons deal more damage. */
    VETERAN,
    /** Scavenger: more coins from kills, and power-ups last longer. */
    SCAVENGER,
    /** Marksman: shots sometimes crit for big damage. */
    SHARPSHOOTER,
    /** Cryo: bullets sometimes freeze the zombie they hit. */
    FROSTBITE,
    /** Demolitionist: grenades, barrels and airstrikes hit much harder. */
    BLAST_EXPERT
}
