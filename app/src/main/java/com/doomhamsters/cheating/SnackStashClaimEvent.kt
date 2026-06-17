package com.doomhamsters.cheating

/** Public event emitted when a player claims a selected card as Snack Stash. */
data class SnackStashClaimEvent(
    val claimId: String,
    val playerId: String,
    val playerName: String,
    val votesRequired: Int,
    val votesReceived: Int,
    val votedPlayerIds: Set<String>,
    val message: String?
)
