package com.kdevs.lastsurvivor.game

import com.kdevs.lastsurvivor.game.engine.Collision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionTest {

    @Test fun `a segment through the circle hits`() {
        val t = Collision.segmentHitsCircle(0f, 0f, 100f, 0f, 50f, 5f, 10f)
        assertEquals(0.5f, t, 0.001f)
    }

    @Test fun `a segment that passes too far away misses`() {
        assertEquals(-1f, Collision.segmentHitsCircle(0f, 0f, 100f, 0f, 50f, 30f, 10f), 0f)
    }

    @Test fun `a fast bullet cannot skip over a small target`() {
        // 1500 units in one step, circle of radius 10 right in the middle
        assertTrue(Collision.segmentHitsCircle(0f, 0f, 1500f, 0f, 750f, 0f, 10f) >= 0f)
    }

    @Test fun `a segment that stops short misses`() {
        assertEquals(-1f, Collision.segmentHitsCircle(0f, 0f, 30f, 0f, 100f, 0f, 10f), 0f)
    }

    @Test fun `a segment that starts inside the circle is a hit near its start`() {
        // the result is the point of closest approach: the centre is 2 units along a 100 unit segment
        assertEquals(0.02f, Collision.segmentHitsCircle(50f, 0f, 150f, 0f, 52f, 0f, 10f), 0.001f)
    }

    @Test fun `a zero length segment is a point test`() {
        assertTrue(Collision.segmentHitsCircle(5f, 5f, 5f, 5f, 8f, 5f, 10f) >= 0f)
        assertEquals(-1f, Collision.segmentHitsCircle(5f, 5f, 5f, 5f, 50f, 5f, 10f), 0f)
    }

    @Test fun `the earlier of two targets has the smaller position`() {
        val near = Collision.segmentHitsCircle(0f, 0f, 100f, 0f, 20f, 0f, 5f)
        val far = Collision.segmentHitsCircle(0f, 0f, 100f, 0f, 80f, 0f, 5f)
        assertTrue(near in 0f..far)
    }
}
