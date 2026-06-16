package com.doomhamsters.model

import org.json.JSONArray

/** Represents the mutable draw deck used during a game. */
class Deck {
    // Arraylist instead of Dequeue cause you can insert easier into it.
    private val cards = ArrayList<Card>()
    /** Returns the number of cards currently in the deck. */
    fun size(): Int = cards.size
    /** Returns the current top card without removing it. */
    fun peekTop(): Card? = cards.lastOrNull()
    /** Returns up to [n] top cards without removing them, top card first. */
    fun peekTopN(n: Int): List<Card> = cards.takeLast(n).reversed()

    /** Removes and returns the current top card. */
    fun draw(): Card? {
        if (cards.isEmpty()) return null
        return cards.removeAt(cards.size - 1)
    }

    /** Randomizes the current card order. */
    fun shuffle() {
        cards.shuffle() // built in kotlin shuffler
    }

    /** Inserts a card at an absolute deck index. */
    fun insertAt(card: Card, index: Int) {
        val index = index.coerceIn(0, cards.size)
        cards.add(index, card)
    }

    /** Inserts a card by counting positions down from the top of the deck. */
    fun insertFromTop(card: Card, position: Int) {
        val actualPosition = (cards.size - position).coerceIn(0, cards.size)
        cards.add(actualPosition, card)
    }

    /** Serializes the deck into a JSON array. */
    fun toJson(): JSONArray {
        val arr = JSONArray()
        cards.forEach { arr.put(it.toJson()) }
        return arr
    }

    companion object {
        /** Builds a deck from a JSON array. */
        fun fromJson(jsonArray: JSONArray): Deck {
            val deck = Deck()
            for (i in 0 until jsonArray.length()) {
                deck.cards.add(Card.fromJson(jsonArray.getJSONObject(i)))
            }
            return deck
        }
    }
}

