package com.kdevs.lastsurvivor.game.engine

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.kdevs.lastsurvivor.game.render.GameRenderer

/**
 * Bridge between the Compose UI and the engine. Compose hosts this view with AndroidView;
 * all per-frame work happens on the GameLoop thread, so Compose never recomposes per frame.
 */
@SuppressLint("ViewConstructor")
class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val world = GameWorld()
    private var renderer: GameRenderer? = null
    private var loop: GameLoop? = null

    @Volatile private var paused = false

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    fun setPaused(value: Boolean) {
        paused = value
        loop?.paused = value
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val r = renderer ?: GameRenderer().also { renderer = it }
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

    /**
     * TEMPORARY test input for Phase 2: touch anywhere and drag away from the screen centre
     * to move the player. Phase 3 replaces this with real virtual joysticks.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - width / 2f
                val dy = event.y - height / 2f
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist > 40f) {
                    world.input.moveX = dx / dist
                    world.input.moveY = dy / dist
                } else {
                    world.input.moveX = 0f
                    world.input.moveY = 0f
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                world.input.moveX = 0f
                world.input.moveY = 0f
            }
        }
        return true
    }
}
