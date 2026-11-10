package com.kdevs.lastsurvivor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunCheckpointTest {

    @Test fun `encode then decode gives the same checkpoint`() {
        val cp = RunCheckpoint(wave = 12, health = 63, grenades = 4)
        assertEquals(cp, RunCheckpoint.decode(cp.encode()))
    }

    @Test fun `missing or empty text is no checkpoint`() {
        assertNull(RunCheckpoint.decode(null))
        assertNull(RunCheckpoint.decode(""))
    }

    @Test fun `damaged text is no checkpoint`() {
        assertNull(RunCheckpoint.decode("garbage"))
        assertNull(RunCheckpoint.decode("v1|3|50"))            // too few fields
        assertNull(RunCheckpoint.decode("v1|3|50|2|9"))        // too many fields
        assertNull(RunCheckpoint.decode("v1|x|50|2"))          // not a number
        assertNull(RunCheckpoint.decode("v0|3|50|2"))          // unknown version
    }

    @Test fun `impossible values are rejected`() {
        assertNull(RunCheckpoint.decode("v1|0|50|2"))   // wave below 1
        assertNull(RunCheckpoint.decode("v1|3|0|2"))    // already dead
        assertNull(RunCheckpoint.decode("v1|3|50|-1"))  // negative grenades
    }
}
