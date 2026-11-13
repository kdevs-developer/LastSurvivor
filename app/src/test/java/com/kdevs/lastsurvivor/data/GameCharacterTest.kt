package com.kdevs.lastsurvivor.data

import com.kdevs.lastsurvivor.game.entities.AbilityType
import com.kdevs.lastsurvivor.game.entities.GameCharacter
import com.kdevs.lastsurvivor.game.entities.PassiveType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCharacterTest {
    @Test fun `every character is found by its saved name`() {
        for (c in GameCharacter.entries) assertEquals(c, GameCharacter.fromName(c.name))
    }

    @Test fun `unknown or missing name falls back to Survivor`() {
        assertEquals(GameCharacter.SURVIVOR, GameCharacter.fromName(null))
        assertEquals(GameCharacter.SURVIVOR, GameCharacter.fromName("RANGER")) // an old outfit name from before characters
    }

    @Test fun `there are ten characters and only Survivor is free`() {
        assertEquals(10, GameCharacter.entries.size)
        assertEquals(listOf(GameCharacter.SURVIVOR), GameCharacter.entries.filter { it.isFree })
    }

    @Test fun `prices rise from the first unlock to the last`() {
        val prices = GameCharacter.entries.drop(1).map { it.diamondPrice }
        assertEquals(prices.sorted(), prices)
        assertEquals(prices.toSet().size, prices.size)
    }

    @Test fun `each character has its own ability`() {
        assertEquals(AbilityType.entries.size, GameCharacter.entries.map { it.ability }.toSet().size)
    }

    @Test fun `every ability has a real cooldown`() {
        for (a in AbilityType.entries) assertTrue(a.cooldownSeconds > 0f)
    }

    @Test fun `each character has its own passive skill`() {
        assertEquals(PassiveType.entries.size, GameCharacter.entries.map { it.passive }.toSet().size)
        assertEquals(GameCharacter.entries.size, PassiveType.entries.size)
    }

    @Test fun `Overclock shortens the Engineer's cooldown and nobody else's`() {
        val engineer = GameCharacter.ENGINEER
        assertTrue(engineer.abilityCooldownSeconds < engineer.ability.cooldownSeconds)
        for (c in GameCharacter.entries.filter { it != engineer }) {
            assertEquals(c.ability.cooldownSeconds, c.abilityCooldownSeconds, 0.0001f)
        }
    }

    @Test fun `passive multipliers only apply to their owner`() {
        for (c in GameCharacter.entries) {
            assertEquals(c == GameCharacter.COMMANDO, c.damageMult > 1f)
            assertEquals(c == GameCharacter.SCAVENGER, c.coinMult > 1f)
            assertEquals(c == GameCharacter.SCAVENGER, c.buffDurationMult > 1f)
            assertEquals(c == GameCharacter.DEMOLITIONIST, c.blastDamageMult > 1f)
        }
    }
}
