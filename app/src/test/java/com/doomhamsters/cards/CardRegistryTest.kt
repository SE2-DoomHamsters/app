package com.doomhamsters.cards


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
    fun `normal card remains non activatable`() {
        val definition = CardRegistry.definitionFor(Card(CardType.Normal))

        assertFalse(definition.command != null)
        assertEquals("Normal", definition.displayName)
    }
}
