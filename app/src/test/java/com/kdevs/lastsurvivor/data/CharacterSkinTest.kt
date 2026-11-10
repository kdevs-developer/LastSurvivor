package com.kdevs.lastsurvivor.data

import com.kdevs.lastsurvivor.game.entities.CharacterSkin
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterSkinTest {
    @Test fun `every skin is found by its saved name`() {
        for (skin in CharacterSkin.entries) assertEquals(skin, CharacterSkin.fromName(skin.name))
    }

    @Test fun `unknown or missing name falls back to the default outfit`() {
        assertEquals(CharacterSkin.SURVIVOR, CharacterSkin.fromName(null))
        assertEquals(CharacterSkin.SURVIVOR, CharacterSkin.fromName("DELETED_SKIN"))
    }
}
