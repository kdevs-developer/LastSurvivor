package com.kdevs.lastsurvivor.game.effects

import com.kdevs.lastsurvivor.game.engine.GameConfig

/**
 * Damage numbers that float up from a hit zombie and fade. Parallel arrays, like
 * [ParticleSystem]. [kind] chooses the colour and size in the renderer. Game thread only.
 */
class FloatingTexts(val capacity: Int) {

    companion object {
        const val NORMAL = 0
        const val BOOSTED = 1  // dealt while Double Damage is active
        const val KILL = 2     // the hit that killed the zombie
    }

    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val age = FloatArray(capacity)
    val value = IntArray(capacity)
    val kind = IntArray(capacity)

    var count: Int = 0
        private set

    fun spawn(x: Float, y: Float, value: Int, kind: Int) {
        if (count >= capacity) return
        val i = count++
        this.x[i] = x
        this.y[i] = y
        this.value[i] = value
        this.kind[i] = kind
        age[i] = 0f
    }

    fun update(dt: Float) {
        var i = 0
        while (i < count) {
            val a = age[i] + dt
            if (a >= GameConfig.DAMAGE_NUMBER_LIFE) {
                val last = count - 1
                if (i != last) {
                    x[i] = x[last]; y[i] = y[last]; age[i] = age[last]
                    value[i] = value[last]; kind[i] = kind[last]
                }
                count--
                continue
            }
            age[i] = a
            y[i] -= GameConfig.DAMAGE_NUMBER_RISE_SPEED * dt
            i++
        }
    }

    fun clear() { count = 0 }
}
