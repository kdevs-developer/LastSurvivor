package com.kdevs.lastsurvivor.game.entities

import com.kdevs.lastsurvivor.game.engine.GameConfig

/**
 * The playable characters. Survivor is free; the others are bought with diamonds ([diamondPrice]).
 * Each has small stat changes (multipliers on the base values), one active [ability] (a button in the
 * game) and one [passive] skill (always on). Names, descriptions and colours live in XML resources
 * (see ui/character/CharacterLabels.kt). The list is in order of price, which is the order the
 * Characters screen shows them in.
 */
enum class GameCharacter(
    val diamondPrice: Int,
    val ability: AbilityType,
    val passive: PassiveType,
    /** Multiplies maximum health. */
    val healthMult: Float = 1f,
    /** Multiplies walking and sprinting speed. */
    val speedMult: Float = 1f,
    /** Multiplies damage taken from zombie hits (below 1 = armor). */
    val damageTakenMult: Float = 1f,
    /** Health regained per second without any power-up. */
    val regenPerSecond: Float = 0f,
    /** Multiplies the weapon's reload time (below 1 = faster). */
    val reloadTimeMult: Float = 1f,
    /** Multiplies the weapon's magazine size. */
    val magazineMult: Float = 1f
) {
    SURVIVOR(diamondPrice = 0, ability = AbilityType.ADRENALINE, passive = PassiveType.LAST_STAND),
    RUNNER(
        diamondPrice = 50, ability = AbilityType.DASH, passive = PassiveType.NIMBLE,
        healthMult = 0.75f, speedMult = 1.25f
    ),
    TANK(
        diamondPrice = 100, ability = AbilityType.SHOCKWAVE, passive = PassiveType.THORNS,
        healthMult = 1.5f, speedMult = 0.85f, damageTakenMult = 0.8f
    ),
    MEDIC(
        diamondPrice = 150, ability = AbilityType.HEAL_PULSE, passive = PassiveType.TRIAGE,
        regenPerSecond = 0.8f
    ),
    SCAVENGER(
        diamondPrice = 175, ability = AbilityType.JACKPOT, passive = PassiveType.SCAVENGER,
        healthMult = 0.9f, speedMult = 1.1f, magazineMult = 1.1f
    ),
    ENGINEER(
        diamondPrice = 200, ability = AbilityType.TURRET, passive = PassiveType.OVERCLOCK,
        healthMult = 0.9f, reloadTimeMult = 0.9f
    ),
    MARKSMAN(
        diamondPrice = 250, ability = AbilityType.DEADEYE, passive = PassiveType.SHARPSHOOTER,
        healthMult = 0.85f, reloadTimeMult = 0.85f
    ),
    COMMANDO(
        diamondPrice = 300, ability = AbilityType.RAGE, passive = PassiveType.VETERAN,
        reloadTimeMult = 0.7f, magazineMult = 1.3f
    ),
    CRYO(
        diamondPrice = 350, ability = AbilityType.FREEZE, passive = PassiveType.FROSTBITE,
        healthMult = 1.1f, speedMult = 1.05f, damageTakenMult = 0.9f
    ),
    DEMOLITIONIST(
        diamondPrice = 400, ability = AbilityType.AIRSTRIKE, passive = PassiveType.BLAST_EXPERT,
        healthMult = 1.15f, speedMult = 0.95f, damageTakenMult = 0.9f, magazineMult = 0.9f
    );

    val isFree: Boolean get() = diamondPrice == 0

    /** The wait after using the ability, with the Engineer's Overclock applied. */
    val abilityCooldownSeconds: Float
        get() = ability.cooldownSeconds * (if (passive == PassiveType.OVERCLOCK) GameConfig.OVERCLOCK_COOLDOWN_MULT else 1f)

    /** Multiplies the damage of every shot (Commando's Veteran). */
    val damageMult: Float get() = if (passive == PassiveType.VETERAN) GameConfig.VETERAN_DAMAGE_MULT else 1f

    /** Multiplies the coins earned by kills (Scavenger). */
    val coinMult: Float get() = if (passive == PassiveType.SCAVENGER) GameConfig.SCAVENGER_COIN_MULT else 1f

    /** Multiplies how long collected power-ups last (Scavenger). */
    val buffDurationMult: Float get() = if (passive == PassiveType.SCAVENGER) GameConfig.SCAVENGER_BUFF_MULT else 1f

    /** Multiplies the damage of blasts: grenades, barrels, airstrikes (Demolitionist). */
    val blastDamageMult: Float get() = if (passive == PassiveType.BLAST_EXPERT) GameConfig.BLAST_EXPERT_DAMAGE_MULT else 1f

    companion object {
        /** The character saved under [name], or the default if it is missing or unknown. */
        fun fromName(name: String?): GameCharacter = entries.firstOrNull { it.name == name } ?: SURVIVOR
    }
}
