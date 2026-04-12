package com.doomhamsters.model

class Deck {
    // Arraylist instead of Dequeue cause you can insert easier into it.
    private val cards = ArrayList<Card>()

    fun draw(): Card? {
        if (cards.isEmpty()) return null
        return cards.removeAt(cards.size - 1)
    }

    fun shuffle() {
        cards.shuffle() // built in kotlin shuffler
    }

    fun insertAt(card: Card, index: Int) {
        val Index = index.coerceIn(0, cards.size)
        cards.add(Index, card)
    }

    fun insertFromTop(card: Card, position: Int) {
        val actualPosition = (cards.size - position).coerceIn(0, cards.size)
        cards.add(actualPosition, card)
    }


}