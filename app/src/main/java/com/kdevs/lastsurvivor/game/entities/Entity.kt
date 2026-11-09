package com.kdevs.lastsurvivor.game.entities

/** Base class for everything that lives in the game world. Pure Kotlin, no Android/Compose types. */
abstract class Entity(
    var x: Float,
    var y: Float,
    var radius: Float
) {
    /** Facing angle in radians (0 = +X axis, increases clockwise on screen). */
    var rotation: Float = 0f
    var active: Boolean = true

    abstract fun update(dt: Float)
}
