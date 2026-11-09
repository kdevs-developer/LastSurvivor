package com.kdevs.lastsurvivor.game.weapons

/**
 * The equipped weapon during a run: ammo in the magazine, reload timer and fire cooldown.
 * Game thread only. Ammo is unlimited in reserve: only the magazine empties.
 */
class WeaponState {

    var stats: WeaponStats = WeaponStats.of(WeaponType.PISTOL, emptyMap())
        private set
    var ammo: Int = stats.magazine
        private set

    private var cooldown = 0f
    private var reloadTimer = 0f

    val reloading: Boolean get() = reloadTimer > 0f
    val reloadProgress: Float
        get() = if (reloading) (1f - reloadTimer / stats.reloadSeconds).coerceIn(0f, 1f) else 0f
    val canFire: Boolean get() = !reloading && ammo > 0 && cooldown <= 0f

    /** Switches to [newStats] with a full magazine. */
    fun equip(newStats: WeaponStats) {
        stats = newStats
        refill()
    }

    fun refill() {
        ammo = stats.magazine
        cooldown = 0f
        reloadTimer = 0f
    }

    /** Returns true if a reload started (not already reloading, magazine not full). */
    fun startReload(): Boolean {
        if (reloading || ammo >= stats.magazine) return false
        reloadTimer = stats.reloadSeconds
        return true
    }

    /** Uses one round and starts the cooldown; reloads automatically when the magazine empties. */
    fun consumeShot() {
        ammo--
        cooldown = stats.fireInterval
        if (ammo <= 0) startReload()
    }

    fun update(dt: Float) {
        if (cooldown > 0f) cooldown -= dt
        if (reloadTimer > 0f) {
            reloadTimer -= dt
            if (reloadTimer <= 0f) {
                reloadTimer = 0f
                ammo = stats.magazine
            }
        }
    }
}
