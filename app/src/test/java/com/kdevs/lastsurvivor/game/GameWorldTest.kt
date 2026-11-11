package com.kdevs.lastsurvivor.game

import com.kdevs.lastsurvivor.game.engine.GameConfig
import com.kdevs.lastsurvivor.game.engine.GameSettings
import com.kdevs.lastsurvivor.game.engine.GameWorld
import com.kdevs.lastsurvivor.game.engine.GoreLevel
import com.kdevs.lastsurvivor.game.engine.RunTotals
import com.kdevs.lastsurvivor.game.engine.WavePhase
import com.kdevs.lastsurvivor.game.entities.GameCharacter
import com.kdevs.lastsurvivor.game.entities.Player
import com.kdevs.lastsurvivor.game.entities.PowerUpType
import com.kdevs.lastsurvivor.game.entities.ZombieType
import com.kdevs.lastsurvivor.game.weapons.WeaponStats
import com.kdevs.lastsurvivor.game.weapons.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs the real simulation headlessly: no audio, no canvas, 60 ticks per simulated second. */
class GameWorldTest {

    private val dt = 1f / 60f

    private fun GameWorld.tick(seconds: Float) = repeat((seconds * 60f).toInt()) { update(dt) }

    private fun newWorld() = GameWorld().also { it.requestEquip(WeaponStats.of(WeaponType.PISTOL, emptyMap())) }

    // ---------------------------------------------------------------- waves

    @Test fun `a new run counts down, then starts wave 1`() {
        val w = newWorld()
        w.update(dt)
        assertEquals(1, w.hud.value.wave)
        assertEquals(WavePhase.COUNTDOWN, w.hud.value.phase)
        w.tick(GameConfig.FIRST_WAVE_COUNTDOWN + 1f)
        assertEquals(WavePhase.ACTIVE, w.hud.value.phase)
        assertTrue(w.hud.value.zombiesLeft > 0)
    }

    @Test fun `zombie types unlock at the documented waves`() {
        val unlock = ZombieType.entries.filter { !it.isBoss }.associate { it to it.unlockWave }
        assertEquals(1, unlock[ZombieType.WALKER])
        assertEquals(3, unlock[ZombieType.RUNNER])
        assertEquals(5, unlock[ZombieType.TOXIC])
        assertEquals(7, unlock[ZombieType.TANK])
    }

    // ---------------------------------------------------------------- continue (checkpoint)

    @Test fun `continue starts at the saved wave with a countdown`() {
        val w = newWorld()
        w.requestContinue(wave = 7, health = 80, grenades = 3)
        w.update(dt)
        val hud = w.hud.value
        assertEquals(7, hud.wave)
        assertEquals(WavePhase.COUNTDOWN, hud.phase)
        assertEquals(80, hud.health)
        assertEquals(3, hud.grenades)
        assertEquals(0, hud.kills)
        assertEquals(0, hud.coins)
    }

    @Test fun `continue never restores less than the minimum health`() {
        val w = newWorld()
        w.requestContinue(wave = 4, health = 3, grenades = 0)
        w.update(dt)
        assertEquals((GameConfig.PLAYER_MAX_HEALTH * GameConfig.CHECKPOINT_MIN_HEALTH_FRACTION).toInt(), w.hud.value.health)
    }

    @Test fun `continue clamps damaged values`() {
        val w = newWorld()
        w.requestContinue(wave = -5, health = 5000, grenades = 99)
        w.update(dt)
        assertEquals(1, w.hud.value.wave)
        assertEquals(GameConfig.PLAYER_MAX_HEALTH.toInt(), w.hud.value.health)
        assertEquals(GameConfig.GRENADE_MAX_COUNT, w.hud.value.grenades)
    }

    @Test fun `continuing at a boss wave spawns the first boss`() {
        val w = newWorld()
        w.requestContinue(wave = GameConfig.BOSS_WAVE_INTERVAL, health = 100, grenades = 2)
        w.tick(GameConfig.WAVE_COUNTDOWN + 0.5f)
        assertTrue(w.hud.value.bossWave)
        assertEquals(ZombieType.BRUTE, w.hud.value.boss)
        assertEquals(100, w.hud.value.bossPercent)
    }

    @Test fun `restart goes back to wave 1 and clears the totals`() {
        val w = newWorld()
        w.requestContinue(wave = 9, health = 100, grenades = 2)
        w.update(dt)
        w.requestRestart()
        w.update(dt)
        assertEquals(1, w.hud.value.wave)
        assertEquals(RunTotals(), w.runTotals())
    }

    // ---------------------------------------------------------------- totals for saving

    @Test fun `shooting a zombie shows up in the run totals`() {
        val w = newWorld()
        w.update(dt)
        w.spawnZombie(ZombieType.WALKER, w.player.x + 120f, w.player.y)
        w.input.fireHeld = true // the player faces +X, where the zombie is
        w.tick(2f)
        val t = w.runTotals()
        assertEquals(1, t.kills)
        assertEquals(ZombieType.WALKER.coinReward, t.coins)
        assertTrue(t.shotsFired >= 2)
        assertTrue(t.shotsHit >= 2)
        assertTrue(t.seconds in 1..2)
        assertEquals(0, t.bossKills)
    }

    @Test fun `time stops counting after the player dies`() {
        val w = newWorld()
        w.tick(1f)
        w.player.health = 0f
        w.update(dt)
        val atDeath = w.runTotals().seconds
        w.tick(5f)
        assertTrue(w.gameOver)
        assertTrue(w.hud.value.gameOver)
        assertEquals(atDeath, w.runTotals().seconds)
    }

    // ---------------------------------------------------------------- settings and outfit

    @Test fun `screen shake follows the setting`() {
        val on = newWorld()
        on.addShake(1f)
        on.update(dt)
        assertTrue(on.shakeX != 0f || on.shakeY != 0f)

        val off = newWorld()
        off.settings = GameSettings(screenShake = false)
        off.addShake(1f)
        off.update(dt)
        assertEquals(0f, off.shakeX, 0f)
        assertEquals(0f, off.shakeY, 0f)
    }

    @Test fun `gore setting scales the blood`() {
        fun particlesAfterKill(level: GoreLevel): Int {
            val w = newWorld()
            w.settings = GameSettings(gore = level)
            w.update(dt)
            w.spawnZombie(ZombieType.WALKER, w.player.x + 120f, w.player.y)
            w.input.fireHeld = true
            var peak = 0
            repeat(90) {
                w.update(dt)
                peak = maxOf(peak, w.particles.count)
            }
            return peak
        }
        assertTrue(particlesAfterKill(GoreLevel.OFF) < particlesAfterKill(GoreLevel.NORMAL))
    }

    @Test fun `the character is a plain property the renderer reads`() {
        val w = newWorld()
        assertEquals(GameCharacter.SURVIVOR, w.character)
        w.character = GameCharacter.MEDIC
        assertNotEquals(GameCharacter.SURVIVOR, w.character)
        assertFalse(w.gameOver)
    }

    // ---------------------------------------------------------------- characters

    @Test fun `a character's stats apply at the start of the next tick`() {
        val w = newWorld()
        w.character = GameCharacter.TANK
        w.update(dt)
        assertEquals(150, w.hud.value.maxHealth)
        assertEquals(150, w.hud.value.health)
        assertEquals(Player.BASE_SPEED * 0.85f, w.player.speed, 0.001f)
        assertEquals(com.kdevs.lastsurvivor.game.entities.AbilityType.SHOCKWAVE, w.hud.value.ability)
    }

    @Test fun `the commando gets a bigger magazine and a faster reload`() {
        val base = WeaponStats.of(WeaponType.PISTOL, emptyMap())
        val w = GameWorld()
        w.requestEquip(base)
        w.character = GameCharacter.COMMANDO
        w.update(dt)
        assertEquals(Math.round(base.magazine * 1.3f), w.weapon.stats.magazine)
        assertEquals(base.reloadSeconds * 0.7f, w.weapon.stats.reloadSeconds, 0.0001f)
        assertEquals(w.weapon.stats.magazine, w.weapon.ammo)
    }

    @Test fun `survivor keeps the weapon's own numbers`() {
        val base = WeaponStats.of(WeaponType.PISTOL, emptyMap())
        val w = GameWorld()
        w.requestEquip(base)
        w.update(dt)
        assertEquals(base, w.weapon.stats)
    }

    @Test fun `an ability starts its cooldown and cannot be used again until it is over`() {
        val w = newWorld()
        w.character = GameCharacter.RUNNER
        w.update(dt)
        assertEquals(100, w.hud.value.abilityPercent)

        w.input.requestAbility()
        w.update(dt)
        assertTrue(w.player.dashing)
        assertTrue(w.hud.value.abilityPercent < 100)
        val cooldown = w.abilityCooldown

        w.input.requestAbility() // too soon: nothing happens, the cooldown just keeps running
        w.update(dt)
        assertTrue(w.abilityCooldown < cooldown)

        w.tick(w.player.character.ability.cooldownSeconds + 1f)
        assertEquals(100, w.hud.value.abilityPercent)
    }

    @Test fun `the shockwave hurts and stuns zombies in range but not those far away`() {
        val w = newWorld()
        w.character = GameCharacter.TANK
        w.update(dt)
        val near = w.spawnZombie(ZombieType.TANK, w.player.x + 150f, w.player.y)
        val far = w.spawnZombie(ZombieType.TANK, w.player.x + 900f, w.player.y)

        w.input.requestAbility()
        w.update(dt)

        assertTrue(near.health < near.maxHealth)
        assertTrue(near.stunTimer > 1f)
        assertEquals(far.maxHealth, far.health, 0.001f)
        assertEquals(0f, far.stunTimer, 0.001f)
    }

    @Test fun `the heal pulse restores health over time`() {
        val w = newWorld()
        w.character = GameCharacter.MEDIC
        w.update(dt)
        w.player.takeDamage(60f)
        val hurt = w.player.health

        w.input.requestAbility()
        w.tick(GameConfig.HEAL_PULSE_SECONDS + 0.5f)

        assertTrue(w.player.health >= hurt + GameConfig.HEAL_PULSE_TOTAL - 1f)
    }

    @Test fun `the engineer's turret shoots zombies but does not count as the player's shots`() {
        val w = newWorld()
        w.character = GameCharacter.ENGINEER
        w.update(dt)
        w.input.requestAbility()
        w.update(dt)
        assertEquals(1, w.turrets.size)

        val t = w.turrets[0]
        val z = w.spawnZombie(ZombieType.TANK, t.x + 200f, t.y)
        w.tick(1.5f)

        assertTrue(z.health < z.maxHealth)
        assertEquals(0, w.runTotals().shotsFired)
        assertEquals(0, w.runTotals().shotsHit)

        w.tick(GameConfig.TURRET_SECONDS)
        assertTrue(w.turrets.isEmpty())
    }

    @Test fun `rage grants rapid fire and double damage`() {
        val w = newWorld()
        w.character = GameCharacter.COMMANDO
        w.update(dt)
        w.input.requestAbility()
        w.update(dt)
        assertTrue(w.player.rapidFire)
        assertTrue(w.player.damageBoost)
    }

    @Test fun `a new run resets the ability cooldown and the turret`() {
        val w = newWorld()
        w.character = GameCharacter.ENGINEER
        w.update(dt)
        w.input.requestAbility()
        w.update(dt)
        assertTrue(w.abilityCooldown > 0f)

        w.requestRestart()
        w.update(dt)
        assertEquals(0f, w.abilityCooldown, 0.001f)
        assertTrue(w.turrets.isEmpty())
    }

    // ---------------------------------------------------------------- player stats and diamonds

    @Test fun `armor reduces damage and a dash dodges it`() {
        val tank = Player(0f, 0f).apply { applyCharacter(GameCharacter.TANK) }
        assertTrue(tank.takeDamage(10f))
        assertEquals(150f - 8f, tank.health, 0.001f)

        val runner = Player(0f, 0f).apply { applyCharacter(GameCharacter.RUNNER) }
        runner.startDash(1f, 0f, GameConfig.DASH_SECONDS)
        assertFalse(runner.takeDamage(10f))
        assertEquals(runner.maxHealth, runner.health, 0.001f)
    }

    @Test fun `the medic regenerates and the others do not`() {
        val medic = Player(0f, 0f).apply { applyCharacter(GameCharacter.MEDIC) }
        medic.takeDamage(30f)
        val hurt = medic.health
        repeat(120) { medic.update(dt) }
        assertTrue(medic.health > hurt)

        val survivor = Player(0f, 0f)
        survivor.takeDamage(30f)
        val survivorHurt = survivor.health
        repeat(120) { survivor.update(dt) }
        assertEquals(survivorHurt, survivor.health, 0.001f)
    }

    @Test fun `diamonds drop only from the zombies' own chance and bosses, never from the normal table`() {
        assertEquals(0, PowerUpType.DIAMOND.weight)
        assertTrue(ZombieType.entries.filter { !it.isBoss }.all { it.diamondChance > 0f && it.diamondChance < 0.5f })
        assertTrue(ZombieType.TANK.diamondChance > ZombieType.WALKER.diamondChance)
        assertTrue(GameConfig.BOSS_DIAMOND_DROPS > 0)
    }
}
