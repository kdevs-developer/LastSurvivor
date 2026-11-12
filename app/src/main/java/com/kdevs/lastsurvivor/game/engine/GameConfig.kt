package com.kdevs.lastsurvivor.game.engine

/** Central tuning values for the engine. Weapon balance is in WeaponType / UpgradeConfig. */
object GameConfig {
    /** Speed multiplier while sprint is switched on (tap the sprint button) and the player is moving. */
    const val SPRINT_MULTIPLIER = 1.6f

    /**
     * The aim stick is also the fire control: the weapon fires automatically while the stick is dragged
     * further than this (0..1 of its radius). The small dead zone stops a touch on the centre from
     * firing in an undefined direction.
     */
    const val AIM_AUTOFIRE_THRESHOLD = 0.15f

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
    const val FIRST_WAVE_COUNTDOWN = 5f
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

    // ---- Area terrain (MapLayout) ----
    /** Movement factor inside quicksand and snow drifts, for the player and the zombies. */
    const val SLOW_ZONE_FACTOR = 0.5f
    /** Poison pools: damage per second and how long the poison lasts after stepping out. */
    const val POISON_POOL_DPS = 4f
    const val POISON_POOL_SECONDS = 1.2f
    /** Explosive barrels (Ruined City): damage a barrel takes before it blows up. */
    const val BARREL_HEALTH = 40f

    // ---- Campaign levels ----
    /** Each campaign level starts this many "waves" tougher than the one before (count, health, speed, zombie types). */
    const val CAMPAIGN_OFFSET_PER_LEVEL = 2
    /** Level complete bonus: coins = this x level number, plus a few diamonds. */
    const val LEVEL_COMPLETE_COINS_PER_LEVEL = 50
    const val LEVEL_COMPLETE_DIAMONDS = 3

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
    /** Farthest a grenade can land: it travels speed x fuse / 2. */
    const val GRENADE_MAX_RANGE = GRENADE_THROW_SPEED * GRENADE_FUSE_SECONDS / 2f
    /** Closest an auto-aimed grenade lands, so it is never dropped at the player's feet. */
    const val GRENADE_MIN_RANGE = 90f

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

    // ---- Graveyard lightning ----
    /** A lightning flash every this many seconds; the thunder follows THUNDER_DELAY seconds after it. */
    const val LIGHTNING_PERIOD = 10f
    const val THUNDER_DELAY = 0.9f

    // ---- Music ----
    /** Background music volume (0..1). Kept low so it stays mild under the effects. */
    const val MUSIC_VOLUME = 0.22f

    // ---- Zombie pathing: how they walk around props ----
    /** How far ahead (world units) a zombie looks for a prop in its way. */
    const val ZOMBIE_STEER_LOOKAHEAD = 170f
    /** Extra clearance kept from a prop's edge while walking around it. */
    const val ZOMBIE_STEER_MARGIN = 10f
    /** A zombie keeps going round the same side of a prop for this long after it was last blocked. */
    const val ZOMBIE_STEER_MEMORY = 0.7f

    // ---- Characters and diamonds ----
    /** A diamond pickup stays on the ground this long (longer than other pickups: they are precious). */
    const val DIAMOND_LIFETIME = 20f
    /** After the last boss dies the level waits at least this long (and until the diamonds are gone) before ending. */
    const val LOOT_MIN_SECONDS = 1.5f
    /** Bosses drop this many diamonds. */
    const val BOSS_DIAMOND_DROPS = 5
    /** Extra diamonds for clearing a boss wave are the drops above; normal zombies use ZombieType.diamondChance. */

    // ---- Character abilities (cooldowns are in AbilityType) ----
    const val ADRENALINE_SECONDS = 5f
    /** Adrenaline also patches the survivor up a little. */
    const val ADRENALINE_HEAL = 15f
    const val ADRENALINE_SPEED_MULT = 1.5f
    const val DASH_SECONDS = 0.2f
    /** Dash speed; the distance covered is DASH_SPEED x DASH_SECONDS (300 units). */
    const val DASH_SPEED = 1500f
    const val SHOCKWAVE_RADIUS = 320f
    const val SHOCKWAVE_DAMAGE = 45f
    const val SHOCKWAVE_KNOCKBACK = 140f
    const val SHOCKWAVE_STUN_SECONDS = 1.5f
    /** Bosses are stunned for this fraction of the time. */
    const val SHOCKWAVE_BOSS_STUN_FRACTION = 0.4f
    const val HEAL_PULSE_TOTAL = 45f
    const val HEAL_PULSE_SECONDS = 3f
    const val TURRET_RADIUS = 24f
    const val TURRET_SECONDS = 10f
    const val TURRET_RANGE = 650f
    const val TURRET_FIRE_INTERVAL = 0.25f
    const val TURRET_DAMAGE = 14f
    const val TURRET_BULLET_SPEED = 1300f
    /** How far in front of the player the turret is placed. */
    const val TURRET_PLACE_DISTANCE = 70f
    const val RAGE_SECONDS = 6f
    /** Marksman: Deadeye makes every shot hit harder and punch through extra zombies. */
    const val DEADEYE_SECONDS = 6f
    const val DEADEYE_DAMAGE_MULT = 2f
    const val DEADEYE_EXTRA_PIERCE = 2
    /** Cryo: Freeze stuns every zombie within this radius (bosses only for BOSS_STUN_FRACTION of the time). */
    const val FREEZE_RADIUS = 560f
    const val FREEZE_SECONDS = 3f
    const val FREEZE_BOSS_STUN_FRACTION = 0.4f
    /** Demolitionist: Airstrike drops this many blasts, one every AIRSTRIKE_INTERVAL seconds, on the zombies. */
    const val AIRSTRIKE_BLASTS = 5
    const val AIRSTRIKE_INTERVAL = 0.22f
    const val AIRSTRIKE_RANGE = 900f
    /** Scavenger: Jackpot drops this many random pickups around the player. */
    const val JACKPOT_DROPS = 3
    const val JACKPOT_SCATTER = 90f

    // ---- Character passives (always on; which character has which is in GameCharacter) ----
    /** Survivor, Last Stand: the first deadly hit of a run leaves this fraction of health and a moment of safety. */
    const val LAST_STAND_HEALTH_FRACTION = 0.35f
    const val LAST_STAND_INVULN_SECONDS = 2.5f
    /** Runner, Nimble: chance that a zombie's swing misses completely. */
    const val NIMBLE_DODGE_CHANCE = 0.15f
    /** Tank, Spiked Armor: damage dealt back to a zombie whose swing lands. */
    const val THORNS_DAMAGE = 14f
    /** Medic, Triage: health restored by every kill. */
    const val TRIAGE_KILL_HEAL = 2f
    /** Engineer, Overclock: abilities recharge this much faster (multiplies the cooldown). */
    const val OVERCLOCK_COOLDOWN_MULT = 0.75f
    /** Commando, Veteran: weapon damage multiplier. */
    const val VETERAN_DAMAGE_MULT = 1.15f
    /** Scavenger: coins from kills, and how much longer collected power-ups last. */
    const val SCAVENGER_COIN_MULT = 1.35f
    const val SCAVENGER_BUFF_MULT = 1.3f
    /** Marksman, Sharpshooter: chance for a shot to crit, and the crit multiplier. */
    const val CRIT_CHANCE = 0.2f
    const val CRIT_DAMAGE_MULT = 2.5f
    /** Cryo, Frostbite: chance for a bullet to freeze a (non-boss) zombie, and for how long. */
    const val FROSTBITE_CHANCE = 0.12f
    const val FROSTBITE_SECONDS = 1.0f
    /** Demolitionist, Blast Expert: multiplier on grenade, barrel and airstrike blast damage. */
    const val BLAST_EXPERT_DAMAGE_MULT = 1.5f

    // ---- Phase 10: saving ----
    /** Continue restores at least this fraction of the maximum health, so a checkpoint is never a death trap. */
    const val CHECKPOINT_MIN_HEALTH_FRACTION = 0.5f
    /** While paused the game redraws this many times a second (it only needs to keep the dimmed picture alive). */
    const val PAUSED_FPS = 15

    // ---- Phase 9: screen shake ----
    // Shake, gore, damage numbers and the debug overlay are now options on the Settings screen (GameSettings).
    /** Shake "trauma" (0..1) lost per second; the offset grows with trauma squared. */
    const val SHAKE_DECAY = 2.4f
    /** Largest camera offset in pixels (at full trauma). */
    const val SHAKE_MAX_OFFSET = 20f
    /** Trauma added by each event (trauma is capped at 1). */
    const val SHAKE_ON_HURT = 0.30f
    const val SHAKE_ON_SHOTGUN = 0.12f
    const val SHAKE_ON_EXPLOSION = 0.55f
    const val SHAKE_ON_BOSS_CHARGE = 0.40f
    const val SHAKE_ON_BOSS_ROAR = 0.35f
    const val SHAKE_ON_BOSS_DEATH = 0.80f
    const val SHAKE_ON_GAME_OVER = 0.50f

    // ---- Phase 9: particles, decals and damage numbers ----
    const val PARTICLE_CAPACITY = 480
    /** Blood and scorch stains on the floor: how many are kept, and how long each lasts. */
    const val DECAL_CAPACITY = 96
    const val DECAL_LIFETIME = 28f
    const val DECAL_FADE_SECONDS = 6f
    const val FLOATING_TEXT_CAPACITY = 40
    const val DAMAGE_NUMBER_LIFE = 0.7f
    const val DAMAGE_NUMBER_RISE_SPEED = 70f
    /** A zombie shows at most one number per this many seconds (hits in between are summed). */
    const val DAMAGE_NUMBER_INTERVAL = 0.12f
    /** Zombies fade and grow in over this long after spawning (matters for summoned minions). */
    const val ZOMBIE_SPAWN_ANIM_SECONDS = 0.35f

    // ---- Phase 9: screen overlays ----
    /** Below this fraction of health the screen edges pulse red. */
    const val LOW_HEALTH_FRACTION = 0.3f
    /** Pulses per second at full health loss; the pulse slows as health recovers. */
    const val LOW_HEALTH_PULSE_HZ = 1.6f

    // ---- Phase 9: sound ----
    const val SFX_MASTER_VOLUME = 0.9f
    /** Most sounds playing at once (SoundPool drops the lowest priority beyond this). */
    const val SFX_MAX_STREAMS = 14
    /** Sounds quieter than this are not played at all. */
    const val SFX_MIN_AUDIBLE = 0.03f
    /** A sound this far from the player (world units) is silent; volume falls off with distance. */
    const val SFX_HEARING_RANGE = 1500f
    /** Horizontal distance at which a sound is panned fully to one side, and how much of that pan is applied. */
    const val SFX_PAN_RANGE = 800f
    const val SFX_PAN_STRENGTH = 0.7f
    /** Zombies of this radius sound at normal pitch; bigger ones sound deeper. */
    const val SFX_PITCH_REF_RADIUS = 26f
    /** Seconds between ambient zombie groans (random in this range, shorter with more zombies). */
    const val GROAN_MIN_INTERVAL = 2.5f
    const val GROAN_MAX_INTERVAL = 6f
    /** Below LOW_HEALTH_FRACTION a heartbeat plays; this is the gap at the lowest health. */
    const val HEARTBEAT_INTERVAL = 0.75f
}