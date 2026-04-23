package com.doomhamsters.logic

import com.doomhamsters.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class GameFactoryTests {

    @Test
    fun test_createGame_initializesGameStateCorrectly() {
        val playerIds = arrayListOf("fat", "zombie", "sleepy")

        val state = GameFactory.createGame(playerIds)

        assertDoesNotThrow { UUID.fromString(state.id) }
        assertNotNull(state.discard)
        assertEquals(Status.Playing, state.status)
        assertTrue(state.currentPlayerIndex in 0 until playerIds.size)
    }

    @Test
    fun test_createGame_initializesPlayersCorrectly() {
        val playerIds = arrayListOf("fat", "zombie")

        val state = GameFactory.createGame(playerIds)

        assertEquals(2, state.players.size)
        assertEquals("fat", state.players[0].id)
        assertEquals("zombie", state.players[1].id)
        assertTrue(state.players.all { it.lives == GameFactory.STARTING_LIVES })
    }

    @Test
    fun test_createGame_dealsInitialCardsToPlayers() {
        val playerIds = arrayListOf("fat", "zombie", "sleepy")

        val state = GameFactory.createGame(playerIds)

        for (player in state.players) {
            assertEquals(6, player.hand.size)
            assertTrue(player.hand.any { it.type == CardType.SnackStash })
        }
    }

    @Test
    fun test_createGame_insertsCorrectNumberOfDoomCards() {
        val playerIds = arrayListOf("fat", "zombie", "sleepy", "ninja")

        val state = GameFactory.createGame(playerIds)

        var doomCount = 0
        var card = state.deck.draw()
        while (card != null) {
            if (card.type == CardType.Doom) {
                doomCount++
            }
            card = state.deck.draw()
        }

        assertEquals(3, doomCount)
    }

    @Test
    fun test_createGame_singlePlayer_doesNotInsertDoomCards() {
        val playerIds = arrayListOf("solo")

        val state = GameFactory.createGame(playerIds)

        var doomCount = 0
        var card = state.deck.draw()
        while (card != null) {
            if (card.type == CardType.Doom) {
                doomCount++
            }
            card = state.deck.draw()
        }

        assertEquals(0, doomCount)
    }

    @Test
    fun test_createGame_exhaustsDeck_skipsDrawLetBlock() {
        val playerIds = arrayListOf("p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10")

        val state = GameFactory.createGame(playerIds)

        val lastPlayer = state.players.last()

        assertEquals(1, lastPlayer.hand.size)
        assertEquals(CardType.SnackStash, lastPlayer.hand.first().type)
    }

    @Test
    fun test_createGame_calculatesCorrectTotalDeckSize() {
        val playerIds = arrayListOf("fat", "zombie", "sleepy")

        val state = GameFactory.createGame(playerIds)

        var remainingDeckSize = 0
        while (state.deck.draw() != null) {
            remainingDeckSize++
        }

        assertEquals(31, remainingDeckSize)
    }
}