package com.doomhamsters.logic

import com.doomhamsters.model.*
import java.util.UUID

object GameFactory {

    const val STARTING_LIVES = 3
    private const val INITIAL_HAND_SIZE = 5
    private const val BASE_NORMAL_CARDS = 40
    private const val EXTRA_SNACK_STASH_CARDS = 4

    fun createGame(playerIds: ArrayList<String>): GameState {
        val playerCount = playerIds.size

        val players = ArrayList(playerIds.map { id -> Player(id, STARTING_LIVES) })
        val deck = Deck()

        for (i in 1..BASE_NORMAL_CARDS) {
            deck.insertAt(Card(CardType.Normal), 0)
        }

        for (i in 1..EXTRA_SNACK_STASH_CARDS) {
            deck.insertAt(Card(CardType.SnackStash), 0)
        }

        deck.shuffle()

        for (player in players) {
            player.hand.add(Card(CardType.SnackStash))
            for (i in 1..INITIAL_HAND_SIZE) {
                deck.draw()?.let { card -> player.hand.add(card) }
            }
        }


        val doomCount = playerCount - 1
        for (i in 1..doomCount) {
            deck.insertAt(Card(CardType.Doom), 0)
        }
        deck.shuffle()

        return GameState(
            id = UUID.randomUUID().toString(),
            players = players,
            deck = deck,
            discard = Discard(),
            currentPlayerIndex = (0 until playerCount).random(), //random starting player
            status = Status.Playing
        )
    }
}