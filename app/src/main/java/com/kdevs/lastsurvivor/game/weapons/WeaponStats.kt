package com.kdevs.lastsurvivor.game.weapons

import kotlin.math.pow
import kotlin.math.roundToInt

enum class UpgradeTrack { DAMAGE, FIRE_RATE, MAGAZINE, RELOAD }

/** Upgrade tuning. Every track goes from level 0 to [MAX_LEVEL]. */
object UpgradeConfig {
    const val MAX_LEVEL = 5
    const val DAMAGE_PER_LEVEL = 0.15f     // +15% damage per level
    const val FIRE_INTERVAL_FACTOR = 0.92f // each level multiplies the time between shots by this
    const val MAGAZINE_PER_LEVEL = 0.2f    // +20% of the base magazine per level
    const val RELOAD_PER_LEVEL = 0.08f     // -8% of the base reload time per level

    /** Coins to buy the next level of any track of [type], given its current [level]. */
    fun cost(type: WeaponType, level: Int): Int = type.upgradeBaseCost * (level + 1)
}

/** A weapon's real stats once its upgrade levels are applied. */
data class WeaponStats(
    val type: WeaponType,
    val damage: Float,
    val fireInterval: Float,
    val magazine: Int,
    val reloadSeconds: Float
) {
    val shotsPerSecond: Float get() = 1f / fireInterval
    val bulletSpeed: Float get() = type.bulletSpeed
    val range: Float get() = type.range
    val pellets: Int get() = type.pellets
    val spreadDegrees: Float get() = type.spreadDegrees
    val pierce: Int get() = type.pierce

    companion object {
        fun of(type: WeaponType, levels: Map<UpgradeTrack, Int>): WeaponStats {
            val dmg = levels[UpgradeTrack.DAMAGE] ?: 0
            val rate = levels[UpgradeTrack.FIRE_RATE] ?: 0
            val mag = levels[UpgradeTrack.MAGAZINE] ?: 0
            val rel = levels[UpgradeTrack.RELOAD] ?: 0
            return WeaponStats(
                type = type,
                damage = type.damage * (1f + UpgradeConfig.DAMAGE_PER_LEVEL * dmg),
                fireInterval = type.fireInterval * UpgradeConfig.FIRE_INTERVAL_FACTOR.pow(rate),
                magazine = (type.magazine * (1f + UpgradeConfig.MAGAZINE_PER_LEVEL * mag)).roundToInt(),
                reloadSeconds = type.reloadSeconds * (1f - UpgradeConfig.RELOAD_PER_LEVEL * rel)
            )
        }
    }
}
