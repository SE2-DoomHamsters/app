package com.doomhamsters.cards.definitions
import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.GameEngine
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
class SignofFatetest {
    @Test
    fun `definition should have correct metadata`() {
        val definition = SignOfFateCardDefinition

        assertEquals(CardType.SignOfFate, definition.type)
        assertEquals("Sign of Fate", definition.displayName)
        assertEquals("Gain 1 life.", definition.description)

        val commandDef = definition.command
        assertEquals(CardCommandId.SIGN_OF_FATE, commandDef.id)
        assertFalse(commandDef.privateResult)
    }

    @Test
    fun `executor should discard card and return correct outcome`() {
        val mockPlayer = mockk<Player>()
        every { mockPlayer.name } returns "Alice"

        val mockCard = mockk<Card>()
        val mockEngine = mockk<GameEngine>(relaxed = true)

        val context = mockk<CardCommandContext>()
        every { context.player } returns mockPlayer
        every { context.card } returns mockCard
        every { context.engine } returns mockEngine

        val outcome = SignOfFateCardDefinition.command.executor!!.execute(context)


        verify(exactly = 1) { mockEngine.discardFromHand(mockPlayer, mockCard) }

        assertEquals("Alice activated Sign of Fate.", outcome.publicMessage)
        assertFalse(outcome.endsTurn, "Sign of Fate should not end the turn")
    }
}