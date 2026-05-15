package com.doomhamsters.model

import org.json.JSONArray

class Discard {
    private val pile = ArrayList<Card>()

    fun add(card: Card) {
        pile.add(card)
    }

    fun peekTop(): Card? = pile.lastOrNull()

    fun toJson(): JSONArray {
        val arr = JSONArray()
        pile.forEach { arr.put(it.toJson()) }
        return arr
    }

    companion object {
        fun fromJson(jsonArray: JSONArray): Discard {
            val discard = Discard()
            for (i in 0 until jsonArray.length()) {
                discard.pile.add(Card.fromJson(jsonArray.getJSONObject(i)))
            }
            return discard
        }
}

}