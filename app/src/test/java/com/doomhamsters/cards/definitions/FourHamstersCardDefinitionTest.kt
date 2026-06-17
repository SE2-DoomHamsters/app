package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.model.CardType
import com.doomhamsters.model.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FourHamstersCardDefinitionTest {

    @Test
    fun `definition should have correct metadata`() {
        val definition = FourHamstersCardDefinition

        assertEquals(CardType.FourHamsters, definition.type)
        assertEquals("Hamster Combo: 4-of-a-Kind", definition.displayName)

        // Prüfen, ob die richtigen Flags für die UI gesetzt sind
        val commandDef = definition.command
        assertEquals(CardCommandId.FOUR_HAMSTERS, commandDef.id)
        assertTrue(commandDef.requiresTargetPlayer, "UI must ask for a target player")
        assertFalse(commandDef.requiresCardType, "UI should NOT ask for a specific card to steal")
        assertTrue(commandDef.requiresHamsterType, "UI must send the hamster type to the backend")
    }

    @Test
    fun `executor should return correct outcome without local state changes`() {
        // Arrange
        val mockPlayer = mockk<Player>()
        every { mockPlayer.name } returns "Alice"

        val context = mockk<CardCommandContext>()
        every { context.player } returns mockPlayer

        // Act
        val outcome = FourHamstersCardDefinition.command.executor!!.execute(context)

        // Assert
        assertEquals("Alice played a 4-of-a-Kind Hamster Combo.", outcome.publicMessage)
        assertFalse(outcome.endsTurn, "Combo should not end the turn immediately")
    }
}