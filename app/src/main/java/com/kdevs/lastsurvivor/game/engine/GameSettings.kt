package com.kdevs.lastsurvivor.game.engine

/** How much blood and goo is drawn. */
enum class GoreLevel(val amount: Float) { OFF(0f), LOW(0.5f), NORMAL(1f) }

/** Size of the on-screen joysticks and buttons. */
enum class ControlSize(val scale: Float) { SMALL(0.85f), NORMAL(1f), LARGE(1.2f) }

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
    val showFps: Boolean = false,
    /** Music loudness 0..1 (the Sound switch above mutes everything). */
    val musicVolume: Float = 1f,
    /** Sound effects loudness 0..1. */
    val sfxVolume: Float = 1f,
    /** Short buzzes when the player is hurt, something explodes, a boss roars. */
    val vibration: Boolean = true,
    /** How big the touch controls are drawn. */
    val controlSize: ControlSize = ControlSize.NORMAL
)
