package com.kdevs.lastsurvivor.game.engine

import android.view.SurfaceHolder
import com.kdevs.lastsurvivor.game.render.GameRenderer

/**
 * Dedicated game thread: fixed 60 Hz simulation, render once per loop iteration.
 * Update and render are fully separated: world.update() never touches a Canvas.
 */
class GameLoop(
    private val holder: SurfaceHolder,
    private val world: GameWorld,
    private val renderer: GameRenderer
) : Thread("LastSurvivor-GameLoop") {

    companion object {
        private const val STEP_NS = 1_000_000_000L / 60L
        private const val STEP_S = 1f / 60f
        private const val MAX_FRAME_NS = 250_000_000L // avoids the "spiral of death" after stalls
        private const val TARGET_FRAME_NS = STEP_NS
    }

    @Volatile private var running = true
    @Volatile var paused = false
    @Volatile var fps = 0
        private set

    override fun run() {
        var previous = System.nanoTime()
        var accumulator = 0L
        var frames = 0
        var fpsTimer = 0L

        while (running) {
            val frameStart = System.nanoTime()
            var elapsed = frameStart - previous
            previous = frameStart
            if (elapsed > MAX_FRAME_NS) elapsed = MAX_FRAME_NS

            if (!paused) {
                accumulator += elapsed
                while (accumulator >= STEP_NS) {
                    world.update(STEP_S)
                    accumulator -= STEP_NS
                }
            }

            draw()

            frames++
            fpsTimer += elapsed
            if (fpsTimer >= 1_000_000_000L) {
                fps = frames
                frames = 0
                fpsTimer -= 1_000_000_000L
            }

            val spentMs = (TARGET_FRAME_NS - (System.nanoTime() - frameStart)) / 1_000_000L
            if (spentMs > 0) {
                try { sleep(spentMs) } catch (e: InterruptedException) { break }
            }
        }
    }

    private fun draw() {
        val canvas = try {
            holder.lockHardwareCanvas()
        } catch (e: IllegalStateException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } ?: return
        try {
            renderer.render(canvas, world, fps, paused)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    /** Stops the thread and waits for it to finish. Call from surfaceDestroyed. */
    fun shutdown() {
        running = false
        interrupt()
        var joined = false
        while (!joined) {
            try { join(); joined = true } catch (_: InterruptedException) { }
        }
    }
}
