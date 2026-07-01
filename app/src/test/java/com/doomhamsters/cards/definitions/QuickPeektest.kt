package com.doomhamsters.cards.definitions
import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.GameEngine
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.Player
import com.doomhamsters.cards.displayName
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
class QuickPeektest {
    @BeforeEach
    fun setup() {

        mockkStatic("com.doomhamsters.cards.CardRegistryKt")
    }

    @AfterEach
    fun teardown() {
        unmockkStatic("com.doomhamsters.cards.CardRegistryKt")
    }

    @Test
    fun `definition should have correct metadata`() {
        val definition = QuickPeekCardDefinition

        assertEquals(CardType.QuickPeek, definition.type)
        assertEquals("Quick Peek", definition.displayName)
        assertEquals("Privately look at the top card of the deck.", definition.description)

        val commandDef = definition.command
        assertEquals(CardCommandId.QUICK_PEEK, commandDef.id)
        assertTrue(commandDef.privateResult, "Quick Peek must have privateResult = true")
    }

    @Test
    fun `executor should reveal top card and set private message when deck is not empty`() {
        val mockPlayer = mockk<Player>()
        every { mockPlayer.name } returns "Bob"

        val playedCard = mockk<Card>()
        val topCard = mockk<Card>()
        every { any<Card>().displayName() } returns "Sneaky Hamster"

        val mockEngine = mockk<GameEngine>(relaxed = true)
        every { mockEngine.peekTopCard() } returns topCard

        val context = mockk<CardCommandContext>()
        every { context.player } returns mockPlayer
        every { context.card } returns playedCard
        every { context.engine } returns mockEngine

        val outcome = QuickPeekCardDefinition.command.executor!!.execute(context)

        verify(exactly = 1) { mockEngine.discardFromHand(mockPlayer, playedCard) }

        assertEquals("Bob activated Quick Peek.", outcome.publicMessage)
        assertEquals("Top card: Sneaky Hamster.", outcome.privateMessage)
        assertEquals(topCard, outcome.revealedCard)
        assertFalse(outcome.endsTurn)
    }

    @Test
    fun `executor should return empty deck message when deck is empty`() {
        val mockPlayer = mockk<Player>()
        every { mockPlayer.name } returns "Bob"

        val playedCard = mockk<Card>()
        val mockEngine = mockk<GameEngine>(relaxed = true)

        every { mockEngine.peekTopCard() } returns null

        val context = mockk<CardCommandContext>()
        every { context.player } returns mockPlayer
        every { context.card } returns playedCard
        every { context.engine } returns mockEngine

        val outcome = QuickPeekCardDefinition.command.executor!!.execute(context)

        verify(exactly = 1) { mockEngine.discardFromHand(mockPlayer, playedCard) }

        assertEquals("Bob activated Quick Peek.", outcome.publicMessage)
        assertEquals("The deck is empty.", outcome.privateMessage)
        assertEquals(null, outcome.revealedCard)
        assertFalse(outcome.endsTurn)
    }
}