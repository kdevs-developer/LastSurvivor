package com.kdevs.lastsurvivor.game.weapons

/**
 * Base stats of every weapon (level 0, no upgrades). Pure data: the upgrade effects are applied
 * by [WeaponStats.of]. Speeds and ranges are world units; the map is 3200 units wide.
 */
enum class WeaponType(
    val price: Int,             // coins to unlock (0 = owned from the start)
    val upgradeBaseCost: Int,   // an upgrade from level L costs upgradeBaseCost * (L + 1)
    val damage: Float,          // per bullet (per pellet for the shotgun)
    val fireInterval: Float,    // seconds between shots while fire is held
    val magazine: Int,
    val reloadSeconds: Float,
    val bulletSpeed: Float,
    val range: Float,
    val pellets: Int,           // bullets per shot
    val spreadDegrees: Float,   // total cone the bullets are spread over
    val pierce: Int,            // zombies one bullet can pass through
    val muzzleOffset: Float     // distance from the player's centre to the barrel tip (muzzle flash, smoke)
) {
    PISTOL(price = 0, upgradeBaseCost = 30, damage = 20f, fireInterval = 0.22f, magazine = 12, reloadSeconds = 1.1f,
        bulletSpeed = 1500f, range = 900f, pellets = 1, spreadDegrees = 0f, pierce = 1, muzzleOffset = 36f),
    SMG(price = 120, upgradeBaseCost = 40, damage = 11f, fireInterval = 0.09f, magazine = 30, reloadSeconds = 1.5f,
        bulletSpeed = 1600f, range = 800f, pellets = 1, spreadDegrees = 5f, pierce = 1, muzzleOffset = 42f),
    SHOTGUN(price = 250, upgradeBaseCost = 50, damage = 12f, fireInterval = 0.8f, magazine = 6, reloadSeconds = 1.8f,
        bulletSpeed = 1300f, range = 450f, pellets = 6, spreadDegrees = 26f, pierce = 1, muzzleOffset = 52f),
    RIFLE(price = 400, upgradeBaseCost = 60, damage = 38f, fireInterval = 0.35f, magazine = 20, reloadSeconds = 1.8f,
        bulletSpeed = 2000f, range = 1200f, pellets = 1, spreadDegrees = 0f, pierce = 3, muzzleOffset = 52f)
}
