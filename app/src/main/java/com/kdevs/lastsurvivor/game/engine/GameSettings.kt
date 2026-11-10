package com.kdevs.lastsurvivor.game.engine

/** How much blood and goo is drawn. */
enum class GoreLevel(val amount: Float) { OFF(0f), LOW(0.5f), NORMAL(1f) }

/**
 * Player-adjustable options (Phase 10). Immutable: the UI builds a new copy and hands it to the
 * engine with GameSurfaceView.applySettings, which swaps it in atomically.
 */
data class GameSettings(
    val soundEnabled: Boolean = true,
    val screenShake: Boolean = true,
    val gore: GoreLevel = GoreLevel.NORMAL,
    val damageNumbers: Boolean = true,
    /** Draws FPS, zombies alive and shot stats in the top-left corner. */
    val showFps: Boolean = false
)
