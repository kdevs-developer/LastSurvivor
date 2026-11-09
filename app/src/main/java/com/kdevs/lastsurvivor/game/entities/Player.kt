package com.kdevs.lastsurvivor.game.entities

import kotlin.math.atan2
import kotlin.math.sqrt

class Player(x: Float, y: Float) : Entity(x, y, radius = 28f) {

    /** Movement direction, each axis -1..1. Written by GameWorld before update(). */
    var moveX: Float = 0f
    var moveY: Float = 0f

    var speed: Float = 260f // world units per second

    override fun update(dt: Float) {
        val len = sqrt(moveX * moveX + moveY * moveY)
        if (len > 0.01f) {
            val scale = if (len > 1f) 1f / len else 1f
            x += moveX * scale * speed * dt
            y += moveY * scale * speed * dt
            rotation = atan2(moveY, moveX) // Phase 3 replaces this with the aim joystick angle
        }
    }
}
