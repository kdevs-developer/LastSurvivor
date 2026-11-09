package com.kdevs.lastsurvivor.game.engine

/** Central tuning values for the engine. Later phases add weapon/zombie balance elsewhere. */
object GameConfig {
    /** Speed multiplier while the sprint button is held and the player is moving. */
    const val SPRINT_MULTIPLIER = 1.6f

    /**
     * Twin-stick convenience: pushing the aim stick to its edge also fires.
     * Set to 2f to disable and fire only with the Fire button.
     */
    const val AIM_AUTOFIRE_THRESHOLD = 0.9f

    // ---- Phase 5: starter pistol. Phase 7 replaces these with per-weapon stats. ----
    /** Bullet speed in world units per second (the map is 3200 units wide). */
    const val BULLET_SPEED = 1500f
    const val BULLET_DAMAGE = 20f
    /** A bullet disappears after flying this far. */
    const val BULLET_RANGE = 900f
    const val BULLET_RADIUS = 5f
    /** Seconds between shots while fire is held (about 4.5 shots per second). */
    const val FIRE_INTERVAL = 0.22f
    /** How long the muzzle flash stays visible after a shot. */
    const val MUZZLE_FLASH_SECONDS = 0.06f
    /** World units a zombie is pushed back by a hit. */
    const val HIT_KNOCKBACK = 14f
}
