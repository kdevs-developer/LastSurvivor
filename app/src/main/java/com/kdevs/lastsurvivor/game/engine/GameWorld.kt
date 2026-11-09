package com.kdevs.lastsurvivor.game.engine

import com.kdevs.lastsurvivor.game.entities.Player

/**
 * All simulation state. Knows nothing about Compose, Canvas or Android views.
 * update() is called only from the game thread with a fixed timestep.
 */
class GameWorld {

    companion object {
        const val WIDTH = 3200f
        const val HEIGHT = 3200f
    }

    val input = InputState()
    val camera = Camera()
    val player = Player(WIDTH / 2f, HEIGHT / 2f)

    @Volatile var viewportWidth: Float = 1f
    @Volatile var viewportHeight: Float = 1f

    fun update(dt: Float) {
        player.moveX = input.moveX
        player.moveY = input.moveY
        player.update(dt)

        player.x = player.x.coerceIn(player.radius, WIDTH - player.radius)
        player.y = player.y.coerceIn(player.radius, HEIGHT - player.radius)

        camera.follow(player.x, player.y, viewportWidth, viewportHeight, WIDTH, HEIGHT)
    }
}
