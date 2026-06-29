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
class SniffAheadcardDefinitionTest {
    @Test
    fun `verify SniffAhead card properties are mapped correctly`() {
        assertEquals(CardType.SniffAhead, SniffAheadCardDefinition.type)
        assertEquals("Sniff Ahead", SniffAheadCardDefinition.displayName)
        assertEquals("Secretly look at the top 3 cards of the deck.", SniffAheadCardDefinition.description)
    }

    @Test
    fun `verify SniffAhead command definition properties`() {
        val command = SniffAheadCardDefinition.command
        assertEquals(CardCommandId.SNIFF_AHEAD, command.id)
        assertTrue(command.privateResult) // Hier prüfen wir das Flag!
    }

    @Test
    fun `verify execute with empty deck returns correct private message`() {
        val mockContext = mockk<CardCommandContext>(relaxed = true)
        every { mockContext.player.name } returns "SherlockHamster"

        every { mockContext.engine.peekTopCards(3) } returns emptyList()

        val outcome = SniffAheadCardDefinition.command.executor!!.execute(mockContext)

        verify { mockContext.engine.discardFromHand(mockContext.player, mockContext.card) }

        assertEquals("SherlockHamster activated Sniff Ahead.", outcome.publicMessage)
        assertEquals("The deck is empty.", outcome.privateMessage)
        assertTrue(outcome.revealedCards.isEmpty())
        assertFalse(outcome.endsTurn)
    }

    @Test
    fun `verify execute with cards in deck returns formatted private message`() {
        val mockContext = mockk<CardCommandContext>(relaxed = true)
        every { mockContext.player.name } returns "SherlockHamster"
        every { mockContext.engine.peekTopCards(3) } returns listOf(
            mockk(relaxed = true),
            mockk(relaxed = true)
        )

        val outcome = SniffAheadCardDefinition.command.executor!!.execute(mockContext)

        verify { mockContext.engine.discardFromHand(mockContext.player, mockContext.card) }

        assertEquals("SherlockHamster activated Sniff Ahead.", outcome.publicMessage)
        assertTrue(outcome.privateMessage?.startsWith("Top 2 card(s):") == true)
        assertEquals(2, outcome.revealedCards.size)
        assertFalse(outcome.endsTurn)
    }
}