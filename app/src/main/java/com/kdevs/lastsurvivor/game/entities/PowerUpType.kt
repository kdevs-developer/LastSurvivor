package com.kdevs.lastsurvivor.game.entities

/**
 * Pickups dropped by zombies. [weight] is the relative chance when a drop is rolled.
 * Durations, heal amounts and the drop rate are tuned in GameConfig.
 */
enum class PowerUpType(val weight: Int) {
    MEDKIT(30),
    RAPID_FIRE(20),
    DOUBLE_DAMAGE(20),
    SHIELD(15),
    GRENADE(25),
    /** Never rolled by the normal drop table (weight 0): diamonds have their own drop chance. */
    DIAMOND(0)
}
