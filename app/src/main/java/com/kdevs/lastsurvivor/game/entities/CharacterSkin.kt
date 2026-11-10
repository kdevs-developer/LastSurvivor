package com.kdevs.lastsurvivor.game.entities

/** Outfit colours the player can choose (Phase 10). Names and colours live in XML resources. */
enum class CharacterSkin {
    SURVIVOR, RANGER, SCAVENGER, SHADOW;

    companion object {
        /** The skin saved under [name], or the default if it is missing or unknown. */
        fun fromName(name: String?): CharacterSkin = entries.firstOrNull { it.name == name } ?: SURVIVOR
    }
}
