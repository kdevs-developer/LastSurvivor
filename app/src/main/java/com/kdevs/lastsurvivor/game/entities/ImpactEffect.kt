package com.kdevs.lastsurvivor.game.entities

/** Short-lived puff drawn where a bullet lands. Pooled like bullets. */
class ImpactEffect : Entity(0f, 0f, 0f) {

    companion object {
        const val DURATION = 0.2f
    }

    /** True for a hit on a zombie (blood), false for a hit on the map edge (dust). */
    var onZombie: Boolean = false
        private set

    var age: Float = 0f
        private set

    /** 0 at the start, 1 when finished. */
    val progress: Float get() = (age / DURATION).coerceIn(0f, 1f)

    fun start(x: Float, y: Float, onZombie: Boolean) {
        this.x = x
        this.y = y
        this.onZombie = onZombie
        age = 0f
        active = true
    }

    override fun update(dt: Float) {
        age += dt
        if (age >= DURATION) active = false
    }
}
