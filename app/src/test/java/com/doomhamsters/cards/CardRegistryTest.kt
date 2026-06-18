package com.doomhamsters.cards


import com.doomhamsters.cards.definitions.StealCardDefinition
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CardRegistryTest {

    @Test
    fun `power nap is registered as turn-ending command card`() {
        val definition = CardRegistry.definitionForType(CardType.PowerNap)

        assertEquals("Power Nap", definition.displayName)
        assertEquals(CardCommandId.POWER_NAP, definition.command?.id)
        assertTrue(definition.command?.endsTurn == true)
    }

    @Test
    fun `quick peek is registered as private result command card`() {
        val definition = CardRegistry.definitionForType(CardType.QuickPeek)

        assertEquals("Quick Peek", definition.displayName)
        assertEquals(CardCommandId.QUICK_PEEK, definition.command?.id)
        assertTrue(definition.command?.privateResult == true)
    }

    @Test
    fun `sniff ahead is registered as private result command card`() {
        val definition = CardRegistry.definitionForType(CardType.SniffAhead)

        assertEquals("Sniff Ahead", definition.displayName)
        assertEquals(CardCommandId.SNIFF_AHEAD, definition.command?.id)
        assertTrue(definition.command?.privateResult == true)
        assertFalse(definition.command?.endsTurn == true)
    }

    @Test
    fun `tunnel chaos is registered as a playable command card`() {
        val definition = CardRegistry.definitionForType(CardType.TunnelChaos)

        assertEquals("Tunnel Chaos", definition.displayName)
        assertEquals(CardCommandId.TUNNEL_CHAOS, definition.command?.id)
        assertTrue(definition.command != null)
        assertFalse(definition.command?.endsTurn == true)
        assertTrue(definition.command?.executor == null)
    }

    @Test
    fun `normal card remains non activatable`() {
        val definition = CardRegistry.definitionFor(Card(CardType.Normal))

        assertFalse(definition.command != null)
        assertEquals("Normal", definition.displayName)
    }
    @Test
    fun `StealCardDefinition should have correct properties`() {
        val definition = StealCardDefinition

        assertEquals("Tiny Thief", definition.displayName)
        assertEquals(CardCommandId.STEAL_CARD, definition.command.id)
        assertTrue(definition.command.privateResult)
        assertFalse(definition.command.endsTurn)
    }
    @Test
    fun `cage swap is registered as targetless command card`() {
        val definition = CardRegistry.definitionForType(CardType.CageSwap)

        assertEquals("Cage Swap", definition.displayName)
        assertEquals(CardCommandId.CAGE_SWAP, definition.command?.id)
        assertFalse(definition.command?.privateResult == true)
        assertFalse(definition.command?.endsTurn == true)
    }

    @Test
    fun `squick is registered as a no-target no-private command card`() {
        val definition = CardRegistry.definitionForType(CardType.Squick)

        assertEquals("Squick", definition.displayName)
        assertEquals(CardCommandId.SQUICK, definition.command?.id)
        assertFalse(definition.command?.privateResult == true)
        assertFalse(definition.command?.endsTurn == true)
        assertFalse(definition.command?.requiresTargetPlayer == true)
        assertTrue(definition.command?.executor == null)
    }
}
