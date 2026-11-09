package com.kdevs.lastsurvivor.game.engine

/** Central tuning values for the engine. Later phases add weapon/zombie balance elsewhere. */
object GameConfig {
    /** Speed multiplier while the sprint button is held and the player is moving. */
    const val SPRINT_MULTIPLIER = 1.6f

    /**
     * Twin-stick convenience: pushing the aim stick to its edge also fires.
     * Set to 2f to disable and fire only with the Fire button.
     */
    const val AIM_AUTOFIRE_THRESHOLD = 0.9f
}
