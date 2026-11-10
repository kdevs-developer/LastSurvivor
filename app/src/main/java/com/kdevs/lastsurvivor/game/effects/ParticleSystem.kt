package com.kdevs.lastsurvivor.game.effects

/**
 * Many tiny short-lived particles (blood, sparks, smoke, fire, shell casings...) stored as
 * parallel float arrays: no object per particle, nothing allocated while playing.
 * Game thread only. When the system is full new particles are simply dropped.
 *
 * The renderer picks colour and shape from [kind]; for PICKUP, [tag] is the PowerUpType ordinal.
 */
class ParticleSystem(val capacity: Int) {

    companion object {
        const val BLOOD = 0
        const val GOO = 1      // toxic green blood
        const val SPARK = 2    // bright streaks: bullets hitting the wall, shield hits
        const val DUST = 3
        const val SMOKE = 4
        const val FIRE = 5
        const val CASING = 6   // ejected brass
        const val PICKUP = 7   // sparkle in the colour of the collected power-up

        /** How fast each kind loses speed (1/second). Higher = stops sooner. */
        private val DRAG = floatArrayOf(5f, 5f, 2.5f, 4f, 2f, 3f, 7f, 3f)
    }

    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val vx = FloatArray(capacity)
    val vy = FloatArray(capacity)
    val life = FloatArray(capacity)
    val maxLife = FloatArray(capacity)
    val size = FloatArray(capacity)
    val rot = FloatArray(capacity)
    val kind = IntArray(capacity)
    val tag = IntArray(capacity)

    var count: Int = 0
        private set

    fun spawn(
        kind: Int, x: Float, y: Float, vx: Float, vy: Float,
        life: Float, size: Float, tag: Int = 0, rot: Float = 0f
    ) {
        if (count >= capacity) return
        val i = count++
        this.kind[i] = kind
        this.x[i] = x
        this.y[i] = y
        this.vx[i] = vx
        this.vy[i] = vy
        this.life[i] = life
        this.maxLife[i] = life
        this.size[i] = size
        this.tag[i] = tag
        this.rot[i] = rot
    }

    fun update(dt: Float) {
        var i = 0
        while (i < count) {
            val l = life[i] - dt
            if (l <= 0f) {
                // swap-remove: copy the last particle into this slot and re-check the slot
                val last = count - 1
                if (i != last) copy(last, i)
                count--
                continue
            }
            life[i] = l
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            val damp = (1f - DRAG[kind[i]] * dt).coerceAtLeast(0f)
            vx[i] *= damp
            vy[i] *= damp
            i++
        }
    }

    fun clear() { count = 0 }

    private fun copy(from: Int, to: Int) {
        x[to] = x[from]; y[to] = y[from]; vx[to] = vx[from]; vy[to] = vy[from]
        life[to] = life[from]; maxLife[to] = maxLife[from]; size[to] = size[from]
        rot[to] = rot[from]; kind[to] = kind[from]; tag[to] = tag[from]
    }
}
