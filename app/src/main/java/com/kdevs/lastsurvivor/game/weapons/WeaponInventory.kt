package com.kdevs.lastsurvivor.game.weapons

/** What the player owns: unlocked weapons, the equipped one, and every upgrade level. */
data class WeaponInventory(
    val unlocked: Set<WeaponType> = setOf(WeaponType.PISTOL),
    val equipped: WeaponType = WeaponType.PISTOL,
    val levels: Map<WeaponType, Map<UpgradeTrack, Int>> = emptyMap()
) {
    fun owns(type: WeaponType): Boolean = type == WeaponType.PISTOL || type in unlocked
    fun levelsOf(type: WeaponType): Map<UpgradeTrack, Int> = levels[type] ?: emptyMap()
    fun level(type: WeaponType, track: UpgradeTrack): Int = levelsOf(type)[track] ?: 0
    fun statsFor(type: WeaponType): WeaponStats = WeaponStats.of(type, levelsOf(type))
    fun equippedStats(): WeaponStats = statsFor(equipped)
}
