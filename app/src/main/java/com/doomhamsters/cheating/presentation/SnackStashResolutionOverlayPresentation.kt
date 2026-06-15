package com.doomhamsters.cheating.presentation

import com.doomhamsters.cheating.SnackStashResolutionEvent
import com.doomhamsters.cheating.SnackStashResolutionOutcome

data class SnackStashResolutionDisplay(
    val title: String,
    val tone: SnackStashResolutionTone,
    val message: String,
    val lifeChangeLines: List<String>
)

enum class SnackStashResolutionTone {
    DOOM,
    SNACK_STASH,
    ACCENT
}

object SnackStashResolutionOverlayPresentation {
    fun display(resolution: SnackStashResolutionEvent): SnackStashResolutionDisplay {
        return SnackStashResolutionDisplay(
            title = when (resolution.outcome) {
                SnackStashResolutionOutcome.CHEATER -> "CHEATER"
                SnackStashResolutionOutcome.LEGITIMATE_CALL -> "LEGITIMATE_CALL"
                SnackStashResolutionOutcome.UNCHALLENGED -> "UNCHALLENGED"
            },
            tone = when (resolution.outcome) {
                SnackStashResolutionOutcome.CHEATER -> SnackStashResolutionTone.DOOM
                SnackStashResolutionOutcome.LEGITIMATE_CALL -> SnackStashResolutionTone.SNACK_STASH
                SnackStashResolutionOutcome.UNCHALLENGED -> SnackStashResolutionTone.ACCENT
            },
            message = resolution.message ?: "Snack Stash claim resolved.",
            lifeChangeLines = resolution.lifeChanges.map { lifeChange ->
                val name = lifeChange.playerName ?: lifeChange.playerId ?: "Player"
                "$name: ${lifeChange.livesBefore} -> ${lifeChange.livesAfter} lives"
            }
        )
    }
}
