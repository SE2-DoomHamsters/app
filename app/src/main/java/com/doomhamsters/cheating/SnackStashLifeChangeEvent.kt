package com.doomhamsters.cheating

data class SnackStashLifeChangeEvent(
    val playerId: String?,
    val playerName: String?,
    val livesBefore: Int,
    val livesAfter: Int
)
