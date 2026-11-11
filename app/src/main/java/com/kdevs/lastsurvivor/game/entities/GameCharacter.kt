package com.kdevs.lastsurvivor.game.entities

/**
 * The playable characters. Survivor is free; the others are bought with diamonds ([diamondPrice]).
 * Each has small stat changes (multipliers on the base values) and one active [ability]. Names,
 * descriptions and colours live in XML resources (see ui/character/CharacterLabels.kt).
 */
enum class GameCharacter(
    val diamondPrice: Int,
    val ability: AbilityType,
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
    SURVIVOR(diamondPrice = 0, ability = AbilityType.ADRENALINE),
    RUNNER(diamondPrice = 50, ability = AbilityType.DASH, healthMult = 0.75f, speedMult = 1.25f),
    TANK(diamondPrice = 100, ability = AbilityType.SHOCKWAVE, healthMult = 1.5f, speedMult = 0.85f, damageTakenMult = 0.8f),
    MEDIC(diamondPrice = 150, ability = AbilityType.HEAL_PULSE, regenPerSecond = 0.8f),
    ENGINEER(diamondPrice = 200, ability = AbilityType.TURRET, healthMult = 0.9f),
    COMMANDO(diamondPrice = 300, ability = AbilityType.RAGE, reloadTimeMult = 0.7f, magazineMult = 1.3f);

    val isFree: Boolean get() = diamondPrice == 0

    companion object {
        /** The character saved under [name], or the default if it is missing or unknown. */
        fun fromName(name: String?): GameCharacter = entries.firstOrNull { it.name == name } ?: SURVIVOR
    }
}
