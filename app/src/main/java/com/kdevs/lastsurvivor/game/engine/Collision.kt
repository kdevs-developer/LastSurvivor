package com.kdevs.lastsurvivor.game.engine

/** Allocation-free collision helpers. Everything is a plain function on floats. */
object Collision {

    /**
     * Swept circle test: does the segment (ax,ay)->(bx,by) pass within [radius] of the circle
     * centre (cx,cy)? Returns the position along the segment (0 = start, 1 = end) of the point
     * closest to the centre, or -1 if there is no hit. Smaller values were reached earlier,
     * so the caller can pick the first zombie a bullet really touched.
     */
    fun segmentHitsCircle(
        ax: Float, ay: Float, bx: Float, by: Float,
        cx: Float, cy: Float, radius: Float
    ): Float {
        val abx = bx - ax
        val aby = by - ay
        val lenSq = abx * abx + aby * aby
        var t = 0f
        if (lenSq > 0.000001f) {
            t = (((cx - ax) * abx + (cy - ay) * aby) / lenSq).coerceIn(0f, 1f)
        }
        val px = ax + abx * t
        val py = ay + aby * t
        val dx = cx - px
        val dy = cy - py
        return if (dx * dx + dy * dy <= radius * radius) t else -1f
    }
}
