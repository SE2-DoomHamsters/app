package com.doomhamsters.model

class Discard {
    private val pile = ArrayList<Card>()

    fun add(card: Card) {
        pile.add(card)
    }

    fun peekTop(): Card? = pile.lastOrNull()
}