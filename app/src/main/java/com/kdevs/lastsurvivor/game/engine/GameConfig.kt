package com.kdevs.lastsurvivor.game.engine

/** Central tuning values for the engine. Weapon balance is in WeaponType / UpgradeConfig. */
object GameConfig {
    /** Speed multiplier while the sprint button is held and the player is moving. */
    const val SPRINT_MULTIPLIER = 1.6f

    /**
     * Twin-stick convenience: pushing the aim stick to its edge also fires.
     * Set to 2f to disable and fire only with the Fire button.
     */
    const val AIM_AUTOFIRE_THRESHOLD = 0.9f

    // ---- Shooting. Per-weapon stats (damage, fire rate, range...) live in game/weapons/WeaponType. ----
    const val BULLET_RADIUS = 5f
    /** How long the muzzle flash stays visible after a shot. */
    const val MUZZLE_FLASH_SECONDS = 0.06f
    /** World units a zombie is pushed back by a hit. */
    const val HIT_KNOCKBACK = 14f

    // ---- Phase 6: player health ----
    const val PLAYER_MAX_HEALTH = 100f
    /** After a zombie hit the player cannot be hit again for this long (poison still ticks). */
    const val PLAYER_INVULN_SECONDS = 0.35f
    /** Length of the red screen flash after a hit. */
    const val HURT_FLASH_SECONDS = 0.3f

    // ---- Phase 6: waves ----
    const val FIRST_WAVE_COUNTDOWN = 3f
    const val WAVE_COUNTDOWN = 5f
    /** Zombies in a wave = WAVE_BASE_COUNT + WAVE_COUNT_PER_WAVE * wave, capped at WAVE_MAX_COUNT. */
    const val WAVE_BASE_COUNT = 5
    const val WAVE_COUNT_PER_WAVE = 3
    const val WAVE_MAX_COUNT = 80
    /** Seconds between spawns: shrinks every wave down to WAVE_MIN_SPAWN_INTERVAL. */
    const val WAVE_SPAWN_INTERVAL = 1.3f
    const val WAVE_SPAWN_INTERVAL_STEP = 0.06f
    const val WAVE_MIN_SPAWN_INTERVAL = 0.3f
    /** Most zombies alive at once: 15 + 2 per wave, capped at WAVE_MAX_ALIVE_CAP. */
    const val WAVE_MAX_ALIVE_BASE = 15
    const val WAVE_MAX_ALIVE_PER_WAVE = 2
    const val WAVE_MAX_ALIVE_CAP = 40
    /** Each wave after the first adds this much to zombie health (and speed, up to the cap). */
    const val WAVE_HEALTH_STEP = 0.10f
    const val WAVE_SPEED_STEP = 0.02f
    const val WAVE_SPEED_CAP = 1.4f
    /** Coins awarded for clearing a wave: this times the wave number. */
    const val WAVE_CLEAR_COINS_PER_WAVE = 5

    /** Draws FPS, alive zombies and shot stats in the top-left corner. The real HUD is in Compose. */
    const val SHOW_DEBUG_OVERLAY = false
}
