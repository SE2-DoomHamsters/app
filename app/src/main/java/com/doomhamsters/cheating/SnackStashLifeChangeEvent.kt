package com.doomhamsters.cheating

/** One player's life total change caused by a resolved Snack Stash claim. */
data class SnackStashLifeChangeEvent(
    val playerId: String?,
    val playerName: String?,
    val livesBefore: Int,
    val livesAfter: Int
)
