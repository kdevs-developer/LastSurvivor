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

    // ---- Phase 8: power-ups ----
    const val POWERUP_RADIUS = 22f
    /** How long a dropped power-up stays on the ground, and how many of those seconds it blinks. */
    const val POWERUP_LIFETIME = 14f
    const val POWERUP_BLINK_SECONDS = 4f
    const val MEDKIT_HEAL = 40f
    const val RAPID_FIRE_SECONDS = 8f
    /** Rapid Fire multiplies the time between shots by this (0.5 = twice as fast). */
    const val RAPID_FIRE_COOLDOWN_SCALE = 0.5f
    const val DOUBLE_DAMAGE_SECONDS = 8f
    const val DOUBLE_DAMAGE_MULT = 2f
    const val SHIELD_SECONDS = 6f
    /** Grenades given by one Grenade pickup. */
    const val GRENADE_PICKUP_AMOUNT = 2

    // ---- Phase 8: grenades ----
    const val GRENADE_START_COUNT = 2
    const val GRENADE_MAX_COUNT = 5
    const val GRENADE_RADIUS = 10f
    const val GRENADE_FUSE_SECONDS = 1.4f
    /** Initial speed; it slows to zero over the fuse, so it lands about speed x fuse / 2 away. */
    const val GRENADE_THROW_SPEED = 700f
    const val GRENADE_BLAST_RADIUS = 230f
    /** Damage at the centre; it falls off to GRENADE_EDGE_DAMAGE_FRACTION of this at the edge. */
    const val GRENADE_DAMAGE = 140f
    const val GRENADE_EDGE_DAMAGE_FRACTION = 0.4f
    const val GRENADE_KNOCKBACK = 70f

    // ---- Phase 8: boss waves ----
    /** Every Nth wave is a boss wave (5, 10, 15...). Bosses alternate Brute, Brood Mother. */
    const val BOSS_WAVE_INTERVAL = 5
    /** Each later boss has this much more health than the first (0.5 = +50% per boss). */
    const val BOSS_HEALTH_PER_BOSS = 0.5f
    /** On a boss wave the usual zombie count is multiplied by this: the boss is the main event. */
    const val BOSS_WAVE_ESCORT_FRACTION = 0.5f
    const val BOSS_CLEAR_COIN_MULT = 2
    const val BOSS_ENRAGE_FRACTION = 0.35f
    const val BOSS_ENRAGE_SPEED_MULT = 1.3f
    const val BOSS_ENRAGE_ABILITY_MULT = 0.6f
    /** Brute charge: warning time, charge time, speed multiplier and damage multiplier. */
    const val BOSS_CHARGE_WINDUP = 0.8f
    const val BOSS_CHARGE_SECONDS = 0.7f
    const val BOSS_CHARGE_SPEED_MULT = 5f
    const val BOSS_CHARGE_DAMAGE_MULT = 1.5f
    /** Brood Mother: Walkers summoned per ability use, and the most zombies allowed alive for it to summon. */
    const val BOSS_SUMMON_COUNT = 3
    const val BOSS_SUMMON_ALIVE_LIMIT = 45

    /** Draws FPS, alive zombies and shot stats in the top-left corner. The real HUD is in Compose. */
    const val SHOW_DEBUG_OVERLAY = false
}
