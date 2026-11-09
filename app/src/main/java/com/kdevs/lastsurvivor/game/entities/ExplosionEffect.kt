package com.kdevs.lastsurvivor.game.entities

/** Expanding blast ring drawn where a grenade went off. Purely visual; damage is dealt by GameWorld. */
class ExplosionEffect : Entity(0f, 0f, 0f) {

    companion object {
        const val DURATION = 0.45f
    }

    var age: Float = 0f
        private set

    /** 0 at the start, 1 when finished. */
    val progress: Float get() = (age / DURATION).coerceIn(0f, 1f)

    /** [radius] is the final size of the blast. */
    fun start(x: Float, y: Float, radius: Float) {
        this.x = x
        this.y = y
        this.radius = radius
        age = 0f
        active = true
    }

    override fun update(dt: Float) {
        age += dt
        if (age >= DURATION) active = false
    }
}
