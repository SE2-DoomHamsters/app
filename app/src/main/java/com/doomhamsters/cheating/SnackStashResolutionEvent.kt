package com.doomhamsters.cheating

/** Public event emitted after a Snack Stash claim resolves. */
data class SnackStashResolutionEvent(
    val outcome: SnackStashResolutionOutcome,
    val claimId: String?,
    val claimingPlayerId: String?,
    val claimingPlayerName: String?,
    val accusingPlayerIds: Set<String>,
    val lifeChanges: List<SnackStashLifeChangeEvent>,
    val affectedPlayerId: String?,
    val affectedPlayerName: String?,
    val livesBefore: Int,
    val livesAfter: Int,
    val doomDefused: Boolean,
    val message: String?
)
