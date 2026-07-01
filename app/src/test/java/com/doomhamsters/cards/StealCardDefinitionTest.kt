package com.doomhamsters.cards
import com.doomhamsters.cards.definitions.StealCardDefinition
import com.doomhamsters.logic.GameEngine
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.Player
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StealCardDefinitionTest {

    @Test
    fun `verify StealCardDefinition properties`() {
        assertEquals(CardType.StealCard, StealCardDefinition.type)
        assertEquals("Tiny Thief", StealCardDefinition.displayName)
        assertEquals("Steals a random card from a player of your choice.", StealCardDefinition.description)

        val command = StealCardDefinition.command
        assertEquals(CardCommandId.STEAL_CARD, command.id)
        assertEquals(true, command.privateResult)
        assertEquals(false, command.endsTurn)
        assertEquals(true, command.requiresTargetPlayer)
        assertEquals("card/activate", command.actionPath)
    }

    @Test
    fun `execute discards card from hand and returns correct outcome`() {
        val mockPlayer = mockk<Player>()
        every { mockPlayer.name } returns "Christian"

        val mockCard = mockk<Card>()

        val mockEngine = mockk<GameEngine>(relaxed = true)

        val context = mockk<CardCommandContext>()
        every { context.player } returns mockPlayer
        every { context.card } returns mockCard
        every { context.engine } returns mockEngine

        val outcome = StealCardDefinition.command.executor?.execute(context)
        verify(exactly = 1) { mockEngine.discardFromHand(mockPlayer, mockCard) }
        assertEquals("Christian aktiviert Tiny Thief.", outcome?.publicMessage)
        assertEquals(false, outcome?.endsTurn)
    }
}
