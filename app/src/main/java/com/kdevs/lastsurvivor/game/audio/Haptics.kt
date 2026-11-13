package com.kdevs.lastsurvivor.game.audio

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Short vibration buzzes that go with the loudest moments. [SoundManager] calls [onEffect] for every
 * sound, so the game logic needs no separate haptic calls; only the effects in the table below buzz.
 * Never throws and never blocks: a phone without a vibrator simply does nothing.
 */
class Haptics(context: Context) {

    @Volatile var enabled: Boolean = true

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private var lastMs = 0L

    fun onEffect(effect: SoundEffect) {
        if (!enabled) return
        val v = vibrator ?: return
        val millis: Long
        val strength: Int
        when (effect) {
            SoundEffect.PLAYER_HURT -> { millis = 45; strength = 200 }
            SoundEffect.EXPLOSION -> { millis = 55; strength = 160 }
            SoundEffect.SHOT_SHOTGUN -> { millis = 22; strength = 120 }
            SoundEffect.BOSS_ROAR -> { millis = 110; strength = 150 }
            SoundEffect.BOSS_DEATH -> { millis = 160; strength = 255 }
            SoundEffect.GAME_OVER -> { millis = 200; strength = 180 }
            else -> return
        }
        val now = SystemClock.uptimeMillis()
        if (now - lastMs < 90) return // never a constant buzz
        lastMs = now
        try {
            if (v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(millis, strength))
        } catch (_: RuntimeException) {
            // haptics are a nicety
        }
    }
}
