package com.kdevs.lastsurvivor.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.engine.GameConfig
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Plays [SoundEffect]s through one SoundPool (short samples, low latency). One instance serves
 * the whole app: get it with [get]. Safe to call from the game thread and the UI thread.
 *
 * Samples load on a SoundPool background thread; a sound requested before its sample has
 * finished loading is simply skipped (SoundPool returns 0), which only happens in the first
 * moments after launch.
 */
class SoundManager private constructor(context: Context) : GameAudio {

    companion object {
        @Volatile private var instance: SoundManager? = null

        fun get(context: Context): SoundManager =
            instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }

        /** Explicit R.raw references (not getIdentifier) so the resource shrinker keeps the files. */
        private fun rawId(effect: SoundEffect): Int = when (effect) {
            SoundEffect.SHOT_PISTOL -> R.raw.sfx_shot_pistol
            SoundEffect.SHOT_REVOLVER -> R.raw.sfx_shot_revolver
            SoundEffect.SHOT_SMG -> R.raw.sfx_shot_smg
            SoundEffect.SHOT_ASSAULT -> R.raw.sfx_shot_assault
            SoundEffect.SHOT_SHOTGUN -> R.raw.sfx_shot_shotgun
            SoundEffect.SHOT_RIFLE -> R.raw.sfx_shot_rifle
            SoundEffect.SHOT_SNIPER -> R.raw.sfx_shot_sniper
            SoundEffect.SHOT_MINIGUN -> R.raw.sfx_shot_minigun
            SoundEffect.RELOAD_PISTOL_A -> R.raw.sfx_reload_pistol_a
            SoundEffect.RELOAD_PISTOL_B -> R.raw.sfx_reload_pistol_b
            SoundEffect.RELOAD_PISTOL_C -> R.raw.sfx_reload_pistol_c
            SoundEffect.RELOAD_REVOLVER_A -> R.raw.sfx_reload_revolver_a
            SoundEffect.RELOAD_REVOLVER_B -> R.raw.sfx_reload_revolver_b
            SoundEffect.RELOAD_REVOLVER_C -> R.raw.sfx_reload_revolver_c
            SoundEffect.RELOAD_SMG_A -> R.raw.sfx_reload_smg_a
            SoundEffect.RELOAD_SMG_B -> R.raw.sfx_reload_smg_b
            SoundEffect.RELOAD_SMG_C -> R.raw.sfx_reload_smg_c
            SoundEffect.RELOAD_SHOTGUN_A -> R.raw.sfx_reload_shotgun_a
            SoundEffect.RELOAD_SHOTGUN_B -> R.raw.sfx_reload_shotgun_b
            SoundEffect.RELOAD_SHOTGUN_C -> R.raw.sfx_reload_shotgun_c
            SoundEffect.RELOAD_RIFLE_A -> R.raw.sfx_reload_rifle_a
            SoundEffect.RELOAD_RIFLE_B -> R.raw.sfx_reload_rifle_b
            SoundEffect.RELOAD_RIFLE_C -> R.raw.sfx_reload_rifle_c
            SoundEffect.RELOAD_SNIPER_A -> R.raw.sfx_reload_sniper_a
            SoundEffect.RELOAD_SNIPER_B -> R.raw.sfx_reload_sniper_b
            SoundEffect.RELOAD_SNIPER_C -> R.raw.sfx_reload_sniper_c
            SoundEffect.RELOAD_MINIGUN_A -> R.raw.sfx_reload_minigun_a
            SoundEffect.RELOAD_MINIGUN_B -> R.raw.sfx_reload_minigun_b
            SoundEffect.RELOAD_MINIGUN_C -> R.raw.sfx_reload_minigun_c
            SoundEffect.HIT_FLESH -> R.raw.sfx_hit_flesh
            SoundEffect.ZOMBIE_DEATH -> R.raw.sfx_zombie_death
            SoundEffect.ZOMBIE_GROAN_1 -> R.raw.sfx_zombie_groan_1
            SoundEffect.ZOMBIE_GROAN_2 -> R.raw.sfx_zombie_groan_2
            SoundEffect.ZOMBIE_SWING -> R.raw.sfx_zombie_swing
            SoundEffect.PLAYER_HURT -> R.raw.sfx_player_hurt
            SoundEffect.POISONED -> R.raw.sfx_poisoned
            SoundEffect.SHIELD_BLOCK -> R.raw.sfx_shield_block
            SoundEffect.HEARTBEAT -> R.raw.sfx_heartbeat
            SoundEffect.PICKUP_POWERUP -> R.raw.sfx_pickup_powerup
            SoundEffect.PICKUP_HEAL -> R.raw.sfx_pickup_heal
            SoundEffect.PICKUP_GRENADE -> R.raw.sfx_pickup_grenade
            SoundEffect.GRENADE_THROW -> R.raw.sfx_grenade_throw
            SoundEffect.EXPLOSION -> R.raw.sfx_explosion
            SoundEffect.BOSS_ROAR -> R.raw.sfx_boss_roar
            SoundEffect.BOSS_WINDUP -> R.raw.sfx_boss_windup
            SoundEffect.BOSS_CHARGE -> R.raw.sfx_boss_charge
            SoundEffect.BOSS_SUMMON -> R.raw.sfx_boss_summon
            SoundEffect.BOSS_DEATH -> R.raw.sfx_boss_death
            SoundEffect.WAVE_START -> R.raw.sfx_wave_start
            SoundEffect.WAVE_START_2 -> R.raw.sfx_wave_start_2
            SoundEffect.WAVE_START_3 -> R.raw.sfx_wave_start_3
            SoundEffect.GAME_START -> R.raw.sfx_game_start
            SoundEffect.THUNDER -> R.raw.sfx_thunder
            SoundEffect.WAVE_CLEAR -> R.raw.sfx_wave_clear
            SoundEffect.GAME_OVER -> R.raw.sfx_game_over
            SoundEffect.UI_CLICK -> R.raw.sfx_ui_click
            SoundEffect.UI_PURCHASE -> R.raw.sfx_ui_purchase
            SoundEffect.UI_DENIED -> R.raw.sfx_ui_denied
        }
    }

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(GameConfig.SFX_MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = IntArray(SoundEffect.entries.size)
    private val lastPlayedMs = LongArray(SoundEffect.entries.size)

    /** Master switch (the Sound toggle in the menu and pause screen). */
    @Volatile var enabled: Boolean = true

    /** Effects volume from Settings (0..1), on top of every effect's own loudness. */
    @Volatile var volume: Float = 1f

    private val haptics = Haptics(context)

    /** The Vibration option in Settings. */
    var vibration: Boolean
        get() = haptics.enabled
        set(value) { haptics.enabled = value }

    init {
        for (effect in SoundEffect.entries) {
            soundIds[effect.ordinal] = pool.load(context, rawId(effect), 1)
        }
    }

    override fun play(effect: SoundEffect, volume: Float, pan: Float, pitch: Float) {
        haptics.onEffect(effect) // buzzes even when the sound is muted
        if (!enabled) return
        val i = effect.ordinal
        val now = SystemClock.uptimeMillis()
        if (now - lastPlayedMs[i] < effect.minIntervalMs) return

        val loudness = (volume * this.volume * effect.volume * GameConfig.SFX_MASTER_VOLUME).coerceIn(0f, 1f)
        if (loudness < GameConfig.SFX_MIN_AUDIBLE) return
        lastPlayedMs[i] = now

        // pan -1..1 -> the far ear gets quieter, the near ear keeps full volume
        val left = loudness * (1f - max(0f, pan))
        val right = loudness * (1f + min(0f, pan))
        val jitter = 1f + (Random.nextFloat() * 2f - 1f) * effect.pitchJitter
        val rate = (pitch * jitter).coerceIn(0.5f, 2f)
        pool.play(soundIds[i], left, right, effect.priority, 0, rate)
    }
}
