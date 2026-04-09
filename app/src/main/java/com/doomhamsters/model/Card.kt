package com.doomhamsters.model

enum class CardType {
    Hamster,
    Defuse
}

data class Card(val type: CardType)