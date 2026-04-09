package com.doomhamsters.model

class Player(
    val id: String,
    var lives: Int
) {
    val hand = ArrayList<Card>()

    fun isAlive(): Boolean {
        return lives > 0
    }
}