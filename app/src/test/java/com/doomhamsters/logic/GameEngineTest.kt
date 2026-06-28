package com.doomhamsters.logic

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.cards.CardRegistry
import com.doomhamsters.cards.definitions.CardCommandDefinition
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.*
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class GameEngineTests {

    private lateinit var engine: GameEngine
    private lateinit var gameState: GameState
    private val playerIds = arrayListOf("fat", "zombie", "sleepy")

    @BeforeEach
    fun setup() {
        mockkObject(GameFactory)
        gameState = makeGameState()
        every { GameFactory.createGame(any()) } returns gameState
        engine = GameEngine(playerIds)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun test_getState_returnsACopy_notTheOriginal() {
        assertNotSame(gameState, engine.getState())
    }

    @Test
    fun test_getState_initialStatus_isPlaying() {
        assertEquals(Status.Playing, engine.getState().status)
    }

    @Test
    fun test_getState_players_matchInjectedIds() {
        val ids = engine.getState().players.map { it.id }
        assertTrue(ids.containsAll(playerIds))
    }

    @Test
    fun test_draw_unknownPlayer_throwsInvalidActionException() {
        assertThrows<InvalidActionException> {
            engine.draw("ghost")
        }
    }

    @Test
    fun test_draw_deadPlayer_throwsInvalidActionException() {
        gameState.players.first().lives = 0
        assertThrows<InvalidActionException> {
            engine.draw("fat")
        }
    }

    @Test
    fun test_draw_emptyDeck_throwsInvalidDrawException() {
        every { GameFactory.createGame(any()) } returns makeGameState(deckCards = emptyList())
        val engine = GameEngine(playerIds)
        assertThrows<InvalidDrawException> {
            engine.draw("fat")
        }
    }

    @Test
    fun test_draw_normalCard_addsCardToPlayerHand() {
        every { GameFactory.createGame(any()) } returns makeGameState(
            deckCards = listOf(Card(CardType.Normal))
        )
        val engine = GameEngine(playerIds)
        val handSizeBefore = engine.getState().players.first { it.id == "zombie" }.hand.size

        engine.draw("zombie")

        assertEquals(handSizeBefore + 1, engine.getState().players.first { it.id == "zombie" }.hand.size)
    }

    @Test
    fun test_draw_normalCard_returnsNull() {
        every { GameFactory.createGame(any()) } returns makeGameState(
            deckCards = listOf(Card(CardType.Normal))
        )
        val engine = GameEngine(playerIds)

        val result = engine.draw("zombie")

        assertNull(result)
    }

    @Test
    fun test_draw_doomCardWithSnackStash_returnsDoomCard() {
        every { GameFactory.createGame(any()) } returns makeGameState(
            deckCards = listOf(Card(CardType.Doom)),
            playerHandCards = listOf(Card(CardType.Normal), Card(CardType.SnackStash))
        )
        val engine = GameEngine(playerIds)

        val result = engine.draw("fat")

        assertNotNull(result)
        assertEquals(CardType.Doom, result!!.type)
    }

    @Test
    fun test_draw_doomCardWithSnackStash_removesOneSnackStashFromHand() {
        every { GameFactory.createGame(any()) } returns makeGameState(
            deckCards = listOf(Card(CardType.Doom)),
            playerHandCards = listOf(Card(CardType.Normal), Card(CardType.SnackStash))
        )
        val engine = GameEngine(playerIds)
        val stashBefore = engine.getState().players.first { it.id == "fat" }
            .hand.count { it.type == CardType.SnackStash }

        engine.draw("fat")

        val stashAfter = engine.getState().players.first { it.id == "fat" }
            .hand.count { it.type == CardType.SnackStash }
        assertEquals(stashBefore - 1, stashAfter)
    }

    @Test
    fun test_draw_doomCardWithSnackStash_movesSnackStashToDiscardPile() {
        every { GameFactory.createGame(any()) } returns makeGameState(
            deckCards = listOf(Card(CardType.Doom)),
            playerHandCards = listOf(Card(CardType.Normal), Card(CardType.SnackStash))
        )
        val engine = GameEngine(playerIds)

        engine.draw("fat")

        assertEquals(CardType.SnackStash, engine.getState().discard.peekTop()?.type)
    }

    @Test
    fun test_draw_doomCardWithoutSnackStash_returnsNull() {
        every { GameFactory.createGame(any()) } returns makeGameState(
            deckCards = listOf(Card(CardType.Doom)),
            playerHandCards = listOf(Card(CardType.Normal))
        )
        val engine = GameEngine(playerIds)

        val result = engine.draw("fat")

        assertNull(result)
    }

    @Test
    fun test_draw_doomCardWithoutSnackStash_decrementsPlayerLives() {
        every { GameFactory.createGame(any()) } returns makeGameState(
            deckCards = listOf(Card(CardType.Doom)),
            playerHandCards = listOf(Card(CardType.Normal))
        )
        val engine = GameEngine(playerIds)
        val livesBefore = engine.getState().players.first { it.id == "fat" }.lives

        engine.draw("fat")

        assertEquals(livesBefore - 1, engine.getState().players.first { it.id == "fat" }.lives)
    }

    @Test
    fun test_draw_doomCardWithoutSnackStash_reinsertsCardIntoDeck() {
        every { GameFactory.createGame(any()) } returns makeGameState(
            deckCards = listOf(Card(CardType.Doom)),
            playerHandCards = listOf(Card(CardType.Normal))
        )
        val engine = GameEngine(playerIds)
        val deckSizeBefore = engine.getState().deck.size()

        engine.draw("fat")

        assertEquals(deckSizeBefore, engine.getState().deck.size())
    }

    @Test
    fun test_insertCard_increasesDeckSizeByOne() {
        val sizeBefore = gameState.deck.size()

        engine.insertCard(Card(CardType.Normal), 0)

        assertEquals(sizeBefore + 1, gameState.deck.size())
    }

    @Test
    fun test_advanceTurn_movesCurrentPlayerIndex() {
        gameState.currentPlayerIndex = 0

        engine.advanceTurn()

        assertNotEquals(0, gameState.currentPlayerIndex)
    }

    @Test
    fun test_advanceTurn_skipsDeadPlayers() {
        gameState.currentPlayerIndex = 0
        gameState.players[1].lives = 0

        engine.advanceTurn()

        assertEquals(2, gameState.currentPlayerIndex)
    }

    @Test
    fun test_advanceTurn_onePlayerAlive_setsStatusToFinished() {
        gameState.players[1].lives = 0
        gameState.players[2].lives = 0

        engine.advanceTurn()

        assertEquals(Status.Finished, gameState.status)
    }

    @Test
    fun test_advanceTurn_lastPlayer_wrapsAroundToIndexZero() {
        gameState.currentPlayerIndex = gameState.players.size - 1

        engine.advanceTurn()

        assertEquals(0, gameState.currentPlayerIndex)
    }

    @Test
    fun test_advanceTurn_lastPlayer_wrapsAroundAndSkipsDeadPlayer() {
        gameState.currentPlayerIndex = gameState.players.size - 1
        gameState.players[0].lives = 0

        engine.advanceTurn()

        assertEquals(1, gameState.currentPlayerIndex)
    }

    // region activateCard

    @Test
    fun test_activateCard_unknownPlayer_throwsInvalidActionException() {
        assertThrows<InvalidActionException> {
            engine.activateCard("ghost", "c1")
        }
    }

    @Test
    fun test_activateCard_deadPlayer_throwsInvalidActionException() {
        gameState.players.first().lives = 0
        assertThrows<InvalidActionException> {
            engine.activateCard("fat", "c1")
        }
    }

    @Test
    fun test_activateCard_notPlayersTurn_throwsInvalidActionException() {
        // currentTurnPlayerId resolves to players[0].id = "fat"
        assertThrows<InvalidActionException> {
            engine.activateCard("zombie", "c1")
        }
    }

    @Test
    fun test_activateCard_cardNotInHand_throwsInvalidActionException() {
        assertThrows<InvalidActionException> {
            engine.activateCard("fat", "missing-id")
        }
    }

    @Test
    fun test_activateCard_cardHasNoCommand_throwsInvalidActionException() {
        val card = Card(CardType.Normal, id = "n1")
        gameState.players.first { it.id == "fat" }.hand.add(card)
        assertThrows<InvalidActionException> {
            engine.activateCard("fat", "n1")
        }
    }

    @Test
    fun test_activateCard_commandHasNoExecutor_throwsInvalidActionException() {
        mockkObject(CardRegistry)
        val card = Card(CardType.PowerNap, id = "pn1")
        gameState.players.first { it.id == "fat" }.hand.add(card)
        every { CardRegistry.commandFor(any()) } returns CardCommandDefinition(
            id = CardCommandId.POWER_NAP,
            executor = null
        )
        assertThrows<InvalidActionException> {
            engine.activateCard("fat", "pn1")
        }
    }

    @Test
    fun test_activateCard_returnsOutcomeFromExecutor() {
        mockkObject(CardRegistry)
        val card = Card(CardType.PowerNap, id = "pn1")
        val mockCommand = mockk<CardCommand>()
        val expectedOutcome = CardCommandOutcome(publicMessage = "test outcome")
        gameState.players.first { it.id == "fat" }.hand.add(card)
        every { CardRegistry.commandFor(any()) } returns CardCommandDefinition(
            id = CardCommandId.POWER_NAP,
            executor = mockCommand
        )
        every { mockCommand.execute(any()) } returns expectedOutcome

        val result = engine.activateCard("fat", "pn1")

        assertEquals(expectedOutcome, result)
    }

    @Test
    fun test_activateCard_endsTurnTrue_advancesTurn() {
        val card = Card(CardType.PowerNap, id = "pn1")
        gameState.players.first { it.id == "fat" }.hand.add(card)
        gameState.currentPlayerIndex = 0

        engine.activateCard("fat", "pn1")

        assertNotEquals(0, gameState.currentPlayerIndex)
    }

    @Test
    fun test_activateCard_endsTurnFalse_doesNotAdvanceTurn() {
        mockkObject(CardRegistry)
        val card = Card(CardType.PowerNap, id = "pn1")
        val mockCommand = mockk<CardCommand>()
        gameState.players.first { it.id == "fat" }.hand.add(card)
        every { CardRegistry.commandFor(any()) } returns CardCommandDefinition(
            id = CardCommandId.POWER_NAP,
            executor = mockCommand
        )
        every { mockCommand.execute(any()) } returns CardCommandOutcome(
            publicMessage = "done",
            endsTurn = false
        )
        gameState.currentPlayerIndex = 0

        engine.activateCard("fat", "pn1")

        assertEquals(0, gameState.currentPlayerIndex)
    }

    // endregion

    // region discardFromHand

    @Test
    fun test_discardFromHand_cardInHand_removesFromHandAndAddsToDiscard() {
        val card = Card(CardType.Normal, id = "n1")
        val player = gameState.players.first { it.id == "fat" }
        player.hand.add(card)

        engine.discardFromHand(player, card)

        assertFalse(player.hand.contains(card))
        assertEquals(card, gameState.discard.peekTop())
    }

    @Test
    fun test_discardFromHand_cardNotInHand_throwsInvalidActionException() {
        val card = Card(CardType.Normal, id = "notInHand")
        val player = gameState.players.first { it.id == "fat" }

        assertThrows<InvalidActionException> {
            engine.discardFromHand(player, card)
        }
    }

    // endregion

    // region peekTopCard / peekTopCards

    @Test
    fun test_peekTopCard_returnsTopCardWithoutRemoving() {
        val topCard = Card(CardType.PowerNap)
        every { GameFactory.createGame(any()) } returns makeGameState(
            deckCards = listOf(Card(CardType.Normal), topCard)
        )
        val engine = GameEngine(playerIds)
        val deckSizeBefore = engine.getState().deck.size()

        val peeked = engine.peekTopCard()

        assertEquals(CardType.PowerNap, peeked?.type)
        assertEquals(deckSizeBefore, engine.getState().deck.size())
    }

    @Test
    fun test_peekTopCards_returnsRequestedCardsWithoutRemoving() {
        val cards = listOf(Card(CardType.Normal), Card(CardType.PowerNap), Card(CardType.Doom))
        every { GameFactory.createGame(any()) } returns makeGameState(deckCards = cards)
        val engine = GameEngine(playerIds)
        val deckSizeBefore = engine.getState().deck.size()

        val peeked = engine.peekTopCards(2)

        assertEquals(2, peeked.size)
        assertEquals(deckSizeBefore, engine.getState().deck.size())
    }

    @Test
    fun test_peekTopCard_emptyDeck_returnsNull() {
        every { GameFactory.createGame(any()) } returns makeGameState(deckCards = emptyList())
        val engine = GameEngine(playerIds)

        assertNull(engine.peekTopCard())
    }

    // endregion

    private fun makeGameState(
        deckCards: List<Card> = listOf(
            Card(CardType.Normal),
            Card(CardType.Normal),
            Card(CardType.Normal)
        ),
        playerHandCards: List<Card> = listOf(Card(CardType.SnackStash))
    ): GameState {
        val deck = Deck()
        deckCards.forEachIndexed { i, card -> deck.insertAt(card, i) }

        val players = ArrayList(playerIds.map { id ->
            Player(id, GameFactory.STARTING_LIVES).also { p ->
                p.hand.addAll(playerHandCards.map { it.copy() })
            }
        })

        return GameState(
            id = "test-id",
            players = players,
            deck = deck,
            discard = Discard(),
            currentPlayerIndex = 0,
            status = Status.Playing
        )
    }
}