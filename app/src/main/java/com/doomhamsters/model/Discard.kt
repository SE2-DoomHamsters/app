package com.doomhamsters.model

import org.json.JSONArray

/** Represents the mutable discard pile for a game. */
class Discard {
    private val pile = ArrayList<Card>()

    /** Adds a card to the top of the discard pile. */
    fun add(card: Card) {
        pile.add(card)
    }

    /** Returns the most recently discarded card. */
    fun peekTop(): Card? = pile.lastOrNull()

    /** Serializes the discard pile into a JSON array. */
    fun toJson(): JSONArray {
        val arr = JSONArray()
        pile.forEach { arr.put(it.toJson()) }
        return arr
    }

    companion object {
        /** Builds a discard pile from a JSON array. */
        fun fromJson(jsonArray: JSONArray): Discard {
            val discard = Discard()
            for (i in 0 until jsonArray.length()) {
                discard.pile.add(Card.fromJson(jsonArray.getJSONObject(i)))
            }
            return discard
        }
}

}