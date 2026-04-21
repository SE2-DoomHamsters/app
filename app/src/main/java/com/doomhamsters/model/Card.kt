package com.doomhamsters.model

enum class CardType {
    Doom,
    SnackStash,
    Normal
}

data class Card(val type: CardType)