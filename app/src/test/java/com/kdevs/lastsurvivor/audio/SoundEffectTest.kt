package com.kdevs.lastsurvivor.audio

import com.kdevs.lastsurvivor.game.audio.SoundEffect
import com.kdevs.lastsurvivor.game.engine.GameConfig
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundEffectTest {

    @Test fun `every weapon has a firing sound`() {
        for (type in WeaponType.entries) {
            assertTrue(SoundEffect.forShot(type).name.startsWith("SHOT_"))
        }
    }

    @Test fun `every weapon has three different reload sounds in order`() {
        for (type in WeaponType.entries) {
            val sounds = (0..2).map { SoundEffect.forReload(type, it) }
            assertEquals(3, sounds.toSet().size)
            assertTrue(sounds.all { it.name.startsWith("RELOAD_") })
            assertTrue(sounds[0].name.endsWith("_A"))
            assertTrue(sounds[1].name.endsWith("_B"))
            assertTrue(sounds[2].name.endsWith("_C"))
        }
    }

    @Test fun `different kinds of weapon sound different`() {
        assertNotEquals(SoundEffect.forReload(WeaponType.PISTOL, 0), SoundEffect.forReload(WeaponType.SHOTGUN, 0))
        assertNotEquals(SoundEffect.forReload(WeaponType.RIFLE, 2), SoundEffect.forReload(WeaponType.SNIPER, 2))
        assertNotEquals(SoundEffect.forShot(WeaponType.PISTOL), SoundEffect.forShot(WeaponType.REVOLVER))
    }

    @Test fun `the closing reload sound can start before the reload ends`() {
        // the lead is shorter than the shortest possible reload, so there is always room for the A and B sounds first
        val shortest = WeaponType.entries.minOf { it.reloadSeconds } * 0.5f
        assertTrue(GameConfig.RELOAD_FINISH_LEAD < shortest)
    }
}
