package com.doomhamsters.ui.gameboard

fun resolvePlayerDisplayName(
    playerId: String,
    fallbackName: String,
    playerNames: Map<String, String>
): String {
    val mappedName = playerNames[playerId]?.trim().orEmpty()
    if (mappedName.isNotBlank()) {
        return mappedName
    }

    val normalizedFallback = fallbackName.trim()
    if (normalizedFallback.isNotBlank() && normalizedFallback != playerId) {
        return normalizedFallback
    }

    return if (playerId.length > 12 || playerId.contains('-')) {
        "Player ${playerId.takeLast(4)}"
    } else {
        playerId
    }
}
