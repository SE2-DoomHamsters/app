package com.doomhamsters.cards.definitions
import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.model.CardType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
class HyperModeCardDefinitionTest {
    @Test
    fun `verify HyperMode card properties are mapped correctly`() {
        assertEquals(CardType.HyperMode, HyperModeCardDefinition.type)
        assertEquals("Hyper Mode", HyperModeCardDefinition.displayName)
        assertEquals("Adds +1 turn to the next player.", HyperModeCardDefinition.description)
    }

    @Test
    fun `verify HyperMode command definition properties`() {
        val command = HyperModeCardDefinition.command
        assertEquals(CardCommandId.HYPER_MODE, command.id)
    }

    @Test
    fun `verify execute discards card and returns correct outcome`() {
        val mockContext = mockk<CardCommandContext>(relaxed = true)
        every { mockContext.player.name } returns "DoomSlayer"
        val outcome = HyperModeCardDefinition.command.executor!!.execute(mockContext)
        verify { mockContext.engine.discardFromHand(mockContext.player, mockContext.card) }
        assertEquals("DoomSlayer activated Hyper Mode.", outcome.publicMessage)
        assertFalse(outcome.endsTurn)
    }
}