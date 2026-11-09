package com.kdevs.lastsurvivor.game.engine

import android.annotation.SuppressLint
import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.kdevs.lastsurvivor.game.render.GameRenderer
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridge between the Compose UI and the engine. Compose hosts this view with AndroidView;
 * all per-frame work happens on the GameLoop thread, so Compose never recomposes per frame.
 * Touch input is no longer handled here: Compose controls write into [input].
 */
@SuppressLint("ViewConstructor")
class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val world = GameWorld()
    private var renderer: GameRenderer? = null
    private var loop: GameLoop? = null

    @Volatile private var paused = false

    /** Shared input state that the on-screen controls write into. */
    val input: InputState get() = world.input

    /** Live HUD values (health, wave, coins, game over) for the Compose overlay. */
    val hud: StateFlow<HudState> get() = world.hud

    /** Starts a fresh run after game over. */
    fun restart() = world.requestRestart()

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    fun setPaused(value: Boolean) {
        paused = value
        loop?.paused = value
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val r = renderer ?: GameRenderer(context.applicationContext).also { renderer = it }
        loop = GameLoop(holder, world, r).also {
            it.paused = paused
            it.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        world.viewportWidth = width.toFloat()
        world.viewportHeight = height.toFloat()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        loop?.shutdown()
        loop = null
    }
}
