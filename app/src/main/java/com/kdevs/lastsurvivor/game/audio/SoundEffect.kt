package com.kdevs.lastsurvivor.game.audio

import com.kdevs.lastsurvivor.game.weapons.WeaponType

/**
 * Every sound effect in one table (pure data, no Android types, like WeaponType).
 *
 * @param volume base loudness 0..1, multiplied by the call's volume and GameConfig.SFX_MASTER_VOLUME
 * @param minIntervalMs the same effect is skipped if it played less than this long ago, so a
 *        5-pellet shotgun blast or a crowd of hits cannot stack into noise
 * @param pitchJitter random pitch spread (+/-) so repeated sounds do not sound identical
 * @param priority SoundPool drops the lowest priority stream first when all streams are busy
 *
 * The audio files are res/raw/sfx_<name in lower case>.wav, made by tools/generate_sfx.py.
 * SoundManager maps each constant to its R.raw id.
 */
enum class SoundEffect(
    val volume: Float,
    val minIntervalMs: Int = 0,
    val pitchJitter: Float = 0.04f,
    val priority: Int = 1
) {
    // ---- weapons ----
    SHOT_PISTOL(0.70f, minIntervalMs = 40, priority = 3),
    SHOT_SMG(0.55f, minIntervalMs = 45, pitchJitter = 0.07f, priority = 3),
    SHOT_SHOTGUN(0.90f, minIntervalMs = 100, priority = 3),
    SHOT_RIFLE(0.85f, minIntervalMs = 60, priority = 3),
    RELOAD_START(0.60f, minIntervalMs = 150, priority = 2),
    RELOAD_DONE(0.65f, minIntervalMs = 150, priority = 2),

    // ---- zombies ----
    HIT_FLESH(0.50f, minIntervalMs = 45, pitchJitter = 0.12f, priority = 1),
    ZOMBIE_DEATH(0.70f, minIntervalMs = 60, pitchJitter = 0.10f, priority = 2),
    ZOMBIE_GROAN_1(0.45f, minIntervalMs = 300, pitchJitter = 0.10f, priority = 0),
    ZOMBIE_GROAN_2(0.45f, minIntervalMs = 300, pitchJitter = 0.10f, priority = 0),
    ZOMBIE_SWING(0.50f, minIntervalMs = 120, pitchJitter = 0.08f, priority = 1),

    // ---- player ----
    PLAYER_HURT(0.85f, minIntervalMs = 150, priority = 4),
    POISONED(0.60f, minIntervalMs = 400, priority = 2),
    SHIELD_BLOCK(0.65f, minIntervalMs = 150, priority = 2),
    HEARTBEAT(0.75f, minIntervalMs = 300, pitchJitter = 0.01f, priority = 2),

    // ---- pickups and grenades ----
    PICKUP_POWERUP(0.70f, priority = 3),
    PICKUP_HEAL(0.70f, priority = 3),
    PICKUP_GRENADE(0.65f, priority = 3),
    GRENADE_THROW(0.60f, priority = 2),
    EXPLOSION(1.00f, minIntervalMs = 80, pitchJitter = 0.06f, priority = 5),

    // ---- bosses ----
    BOSS_ROAR(0.95f, priority = 5),
    BOSS_WINDUP(0.80f, minIntervalMs = 400, priority = 4),
    BOSS_CHARGE(0.85f, minIntervalMs = 400, priority = 4),
    BOSS_SUMMON(0.75f, minIntervalMs = 400, priority = 4),
    BOSS_DEATH(1.00f, priority = 5),

    // ---- waves and game flow ----
    WAVE_START(0.55f, priority = 4),
    WAVE_START_2(0.55f, priority = 4),
    WAVE_START_3(0.50f, priority = 4),
    GAME_START(0.60f, priority = 4),
    THUNDER(0.70f, minIntervalMs = 2000, pitchJitter = 0.06f, priority = 3),
    WAVE_CLEAR(0.75f, priority = 4),
    GAME_OVER(0.90f, priority = 5),

    // ---- menus ----
    UI_CLICK(0.55f, minIntervalMs = 60, pitchJitter = 0.02f, priority = 2),
    UI_PURCHASE(0.70f, priority = 2),
    UI_DENIED(0.60f, minIntervalMs = 150, priority = 2);

    companion object {
        /** The firing sound of [type]. */
        fun forShot(type: WeaponType): SoundEffect = when (type) {
            WeaponType.PISTOL -> SHOT_PISTOL
            WeaponType.SMG -> SHOT_SMG
            WeaponType.SHOTGUN -> SHOT_SHOTGUN
            WeaponType.RIFLE, WeaponType.SNIPER -> SHOT_RIFLE
            WeaponType.REVOLVER -> SHOT_PISTOL
            WeaponType.ASSAULT, WeaponType.MINIGUN -> SHOT_SMG
            WeaponType.AUTO_SHOTGUN -> SHOT_SHOTGUN
        }
    }
}
