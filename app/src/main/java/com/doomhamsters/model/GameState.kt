package com.doomhamsters.model
import java.io.Serializable

enum class Status {
    Lobby,
    Playing,
    Finished
}

data class GameState(
    val id: String,
    val players: ArrayList<Player>,
    val deck: Deck,
    val discard: Discard,
    var currentPlayerIndex: Int,
    var status: Status
) : Serializable // TODO: Figure out how to actually implement it into the backend.