package com.doomhamsters.logic

/* TEMP COMMIT NOTE
Commit with:
Task 2 / Commit 4 - feat(cards): add self-contained card definitions and command template
Delete this note after staging.
*/

import com.doomhamsters.cards.CardRegistry
import com.doomhamsters.model.*
import java.util.UUID

/** Builds fully initialized game states for new matches. */
object GameFactory {

    const val STARTING_LIVES = 3
    private const val INITIAL_HAND_SIZE = 5
    private const val BASE_NORMAL_CARDS = 40
    private const val EXTRA_SNACK_STASH_CARDS = 4

    /** Creates a shuffled game state for the supplied players. */
    fun createGame(playerIds: ArrayList<String>): GameState {
        val playerCount = playerIds.size

        val players = ArrayList(playerIds.map { id -> Player(id, STARTING_LIVES) })
        val deck = Deck()

        for (i in 1..BASE_NORMAL_CARDS) {
            deck.insertAt(createCard(CardType.Normal), 0)
        }

        for (i in 1..EXTRA_SNACK_STASH_CARDS) {
            deck.insertAt(createCard(CardType.SnackStash), 0)
        }

        deck.shuffle()

        for (player in players) {
            player.hand.add(createCard(CardType.SnackStash))
            for (i in 1..INITIAL_HAND_SIZE) {
                deck.draw()?.let { card -> player.hand.add(card) }
            }
        }


        val doomCount = playerCount - 1
        for (i in 1..doomCount) {
            deck.insertAt(createCard(CardType.Doom), 0)
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

    private fun createCard(type: CardType): Card {
        val definition = CardRegistry.definitionForType(type)
        return Card(
            type = type,
            id = UUID.randomUUID().toString(),
            name = definition.displayName,
            effectId = definition.command?.id?.name
        )
    }
}
