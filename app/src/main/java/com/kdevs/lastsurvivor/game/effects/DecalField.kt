package com.kdevs.lastsurvivor.game.effects

import com.kdevs.lastsurvivor.game.engine.GameConfig

/**
 * Stains left on the floor (blood pools, toxic puddles, scorch marks). A ring buffer: when it
 * is full the oldest decal is overwritten. A decal fades out over the last
 * [GameConfig.DECAL_FADE_SECONDS] of its [GameConfig.DECAL_LIFETIME]. Game thread only.
 */
class DecalField(val capacity: Int) {

    companion object {
        const val BLOOD = 0
        const val GOO = 1
        const val SCORCH = 2

        /** How many pre-drawn bitmaps exist per kind (see SpriteAtlas); GameWorld picks one at random. */
        const val BLOOD_VARIANTS = 4
        const val GOO_VARIANTS = 2
    }

    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val size = FloatArray(capacity)   // diameter in world units
    val rot = FloatArray(capacity)
    val kind = IntArray(capacity)
    val variant = IntArray(capacity)  // which pre-drawn bitmap of that kind
    val age = FloatArray(capacity)

    private var next = 0
    var count: Int = 0
        private set

    fun add(kind: Int, x: Float, y: Float, size: Float, rot: Float, variant: Int) {
        val i = next
        next = (next + 1) % capacity
        if (count < capacity) count++
        this.kind[i] = kind
        this.x[i] = x
        this.y[i] = y
        this.size[i] = size
        this.rot[i] = rot
        this.variant[i] = variant
        age[i] = 0f
    }

    fun update(dt: Float) {
        var i = 0
        while (i < count) {
            age[i] += dt
            i++
        }
    }

    /** 1 = fully visible, 0 = gone (and should not be drawn). */
    fun alpha(i: Int): Float {
        val left = GameConfig.DECAL_LIFETIME - age[i]
        return if (left <= 0f) 0f else (left / GameConfig.DECAL_FADE_SECONDS).coerceAtMost(1f)
    }

    fun clear() {
        count = 0
        next = 0
    }
}
