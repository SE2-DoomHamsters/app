package com.doomhamsters.cards

import com.doomhamsters.cards.definitions.TwoHamstersCardDefinition
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.model.CardType
import com.doomhamsters.model.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TwoHamstersCardDefinitionTest {

    @Test
    fun `definition should require correct UI flags and metadata`() {
        assertEquals(CardType.Normal, TwoHamstersCardDefinition.type)
        assertEquals("Hamster Combo: 2-of-a-Kind", TwoHamstersCardDefinition.displayName)
        assertTrue(TwoHamstersCardDefinition.description.contains("Play 2 identical Hamster cards"))

        val commandDef = TwoHamstersCardDefinition.command
        assertEquals(CardCommandId.TWO_HAMSTERS, commandDef.id)

        assertEquals(CardCommandId.TWO_HAMSTERS, commandDef.executor?.id)

        assertTrue(commandDef.requiresTargetPlayer, "UI must ask for a target player")
        assertTrue(commandDef.requiresHamsterType, "UI must send the hamster type to the backend")
        assertFalse(commandDef.requiresCardType, "UI should NOT ask for a specific card")
        assertTrue(commandDef.privateResult, "UI must expect a private result to show the stolen card")
    }

    @Test
    fun `executor should return correct outcome and trigger discard`() {
        // Arrange
        val mockPlayer = mockk<Player>()
        every { mockPlayer.name } returns "Bob"

        val context = mockk<CardCommandContext>(relaxed = true)
        every { context.player } returns mockPlayer

        // Act
        val outcome = TwoHamstersCardDefinition.command.executor!!.execute(context)

        // Assert
        assertEquals("Bob played a 2-of-a-Kind Hamster Combo.", outcome.publicMessage)
        assertFalse(outcome.endsTurn)
    }
}