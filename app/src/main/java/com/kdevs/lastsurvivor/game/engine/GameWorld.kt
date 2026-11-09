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
        private const val FLASH_SECONDS = 0.6f
    }

    val input = InputState()
    val camera = Camera()
    val player = Player(WIDTH / 2f, HEIGHT / 2f)

    @Volatile var viewportWidth: Float = 1f
    @Volatile var viewportHeight: Float = 1f

    fun update(dt: Float) {
        applyInput()
        player.update(dt)

        player.x = player.x.coerceIn(player.radius, WIDTH - player.radius)
        player.y = player.y.coerceIn(player.radius, HEIGHT - player.radius)

        camera.follow(player.x, player.y, viewportWidth, viewportHeight, WIDTH, HEIGHT)
    }

    private fun applyInput() {
        val p = player
        p.moveX = input.moveX
        p.moveY = input.moveY
        p.aimX = input.aimX
        p.aimY = input.aimY
        p.aiming = input.aimActive
        p.sprinting = input.sprintHeld
        p.firing = input.fireHeld ||
            (input.aimActive && input.aimStrength >= GameConfig.AIM_AUTOFIRE_THRESHOLD)

        if (input.consumeReload()) p.reloadFlash = FLASH_SECONDS
        if (input.consumeGrenade()) p.grenadeFlash = FLASH_SECONDS
    }
}
