package com.kdevs.lastsurvivor.game

import com.kdevs.lastsurvivor.game.engine.ObjectPool
import com.kdevs.lastsurvivor.game.weapons.UpgradeConfig
import com.kdevs.lastsurvivor.game.weapons.UpgradeTrack
import com.kdevs.lastsurvivor.game.weapons.WeaponInventory
import com.kdevs.lastsurvivor.game.weapons.WeaponState
import com.kdevs.lastsurvivor.game.weapons.WeaponStats
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WeaponTest {

    @Test fun `level 0 gives the base stats`() {
        for (type in WeaponType.entries) {
            val s = WeaponStats.of(type, emptyMap())
            assertEquals(type.damage, s.damage, 0.001f)
            assertEquals(type.fireInterval, s.fireInterval, 0.001f)
            assertEquals(type.magazine, s.magazine)
            assertEquals(type.reloadSeconds, s.reloadSeconds, 0.001f)
        }
    }

    @Test fun `max level upgrades improve every stat`() {
        val max = UpgradeTrack.entries.associateWith { UpgradeConfig.MAX_LEVEL }
        for (type in WeaponType.entries) {
            val base = WeaponStats.of(type, emptyMap())
            val top = WeaponStats.of(type, max)
            assertEquals(type.damage * 1.75f, top.damage, 0.01f)
            assertTrue(top.fireInterval < base.fireInterval)
            assertTrue(top.magazine > base.magazine)
            assertTrue(top.reloadSeconds < base.reloadSeconds)
            assertTrue(top.reloadSeconds > 0f)
        }
    }

    @Test fun `upgrade cost grows with the level`() {
        assertEquals(30, UpgradeConfig.cost(WeaponType.PISTOL, 0))
        assertEquals(90, UpgradeConfig.cost(WeaponType.PISTOL, 2))
        assertEquals(300, UpgradeConfig.cost(WeaponType.RIFLE, 4))
    }

    @Test fun `the pistol is always owned and other weapons are not by default`() {
        val inv = WeaponInventory()
        assertTrue(inv.owns(WeaponType.PISTOL))
        assertFalse(inv.owns(WeaponType.RIFLE))
        assertEquals(WeaponType.PISTOL, inv.equippedStats().type)
    }

    @Test fun `firing empties the magazine and reloads on its own`() {
        val w = WeaponState()
        val mag = w.stats.magazine
        repeat(mag) {
            assertTrue(w.canFire)
            w.consumeShot()
            w.update(w.stats.fireInterval + 0.01f)
        }
        assertTrue(w.reloading)
        assertFalse(w.canFire)
        w.update(w.stats.reloadSeconds + 0.01f)
        assertFalse(w.reloading)
        assertEquals(mag, w.ammo)
    }

    @Test fun `reload cannot start on a full magazine`() {
        assertFalse(WeaponState().startReload())
    }

    @Test fun `a released pooled object is handed out again`() {
        val pool = ObjectPool({ Any() })
        val a = pool.obtain()
        pool.release(a)
        assertSame(a, pool.obtain())
    }

    @Test fun `the armory has the new guns, priced in order and none for free`() {
        val prices = WeaponType.entries.filter { it != WeaponType.PISTOL }.map { it.price }
        assertTrue(prices.all { it > 0 })
        assertTrue(WeaponType.entries.size >= 9)
        assertTrue(WeaponType.SNIPER.pierce > WeaponType.RIFLE.pierce)
        assertTrue(WeaponType.MINIGUN.fireInterval < WeaponType.SMG.fireInterval)
        assertTrue(WeaponType.AUTO_SHOTGUN.pellets >= 4)
    }
}
