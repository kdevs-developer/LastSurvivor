package com.kdevs.lastsurvivor.data

import com.kdevs.lastsurvivor.game.entities.AbilityType
import com.kdevs.lastsurvivor.game.entities.GameCharacter
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

    @Test fun `there are six characters and only Survivor is free`() {
        assertEquals(6, GameCharacter.entries.size)
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
}
