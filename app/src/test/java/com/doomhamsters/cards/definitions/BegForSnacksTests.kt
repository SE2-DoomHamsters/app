package com.doomhamsters.cards.definitions
import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.model.CardType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
class BegForSnacksTests {
    @Test
    fun `verify BegForSnacks card properties are mapped correctly`() {
        assertEquals(CardType.BegForSnacks, BegForSnacksCardDefinition.type)
        assertEquals("Beg for Snacks", BegForSnacksCardDefinition.displayName)
        assertEquals("Name a card type and a player. If they have one, they must give it to you.", BegForSnacksCardDefinition.description)
    }

    @Test
    fun `verify BegForSnacks command definition properties`() {
        val command = BegForSnacksCardDefinition.command
        assertEquals(CardCommandId.BEG_FOR_SNACKS, command.id)
        assertTrue(command.requiresTargetPlayer)
        assertTrue(command.requiresCardType)
    }

    @Test
    fun `verify execute discards card and returns correct outcome`() {
        val mockContext = mockk<CardCommandContext>(relaxed = true)
        every { mockContext.player.name } returns "SnackLover"

        val outcome = BegForSnacksCardDefinition.command.executor!!.execute(mockContext)

        verify { mockContext.engine.discardFromHand(mockContext.player, mockContext.card) }

        assertEquals("SnackLover activated Beg for Snacks.", outcome.publicMessage)
        assertFalse(outcome.endsTurn)
    }
}