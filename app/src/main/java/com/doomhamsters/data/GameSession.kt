package com.doomhamsters.data


/** Stores the identifiers needed to resume an active game for the local player. */
data class GameSession(
    val gameId: String,
    val playerId: String,
    val playerName: String
)
