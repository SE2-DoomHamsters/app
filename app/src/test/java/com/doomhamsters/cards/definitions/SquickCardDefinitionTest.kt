package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.model.CardType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SquickCardDefinitionTest {

    @Test
    fun `type is Squick`() {
        assertEquals(CardType.Squick, SquickCardDefinition.type)
    }

    @Test
    fun `displayName matches backend`() {
        assertEquals("Squick", SquickCardDefinition.displayName)
    }

    @Test
    fun `description is not blank`() {
        assert(SquickCardDefinition.description.isNotBlank())
    }

    @Test
    fun `command id is SQUICK`() {
        assertEquals(CardCommandId.SQUICK, SquickCardDefinition.command?.id)
    }

    @Test
    fun `command has no executor — delegates entirely to backend`() {
        assertNull(SquickCardDefinition.command?.executor)
    }

    @Test
    fun `command does not end the turn`() {
        assertFalse(SquickCardDefinition.command?.endsTurn == true)
    }

    @Test
    fun `command has no private result`() {
        assertFalse(SquickCardDefinition.command?.privateResult == true)
    }

    @Test
    fun `command requires no target parameters`() {
        val command = SquickCardDefinition.command
        assertNotNull(command)
        assertFalse(command!!.requiresTargetPlayer)
        assertFalse(command.requiresCardType)
        assertFalse(command.requiresHamsterType)
    }
}
