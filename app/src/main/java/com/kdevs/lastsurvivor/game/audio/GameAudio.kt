package com.kdevs.lastsurvivor.game.audio

/**
 * What the simulation needs from the sound system. GameWorld only knows this interface, so it
 * stays free of Android types; SoundManager is the Android implementation. Calls may come from
 * any thread. Implementations must never block.
 */
interface GameAudio {
    /**
     * @param volume 0..1, on top of the effect's own base volume
     * @param pan -1 (left) .. 1 (right)
     * @param pitch playback rate, 1 = normal; lower sounds bigger, higher sounds smaller
     */
    fun play(effect: SoundEffect, volume: Float = 1f, pan: Float = 0f, pitch: Float = 1f)
}
