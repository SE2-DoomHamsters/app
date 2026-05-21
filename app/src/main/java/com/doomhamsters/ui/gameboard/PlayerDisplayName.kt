package com.doomhamsters.ui.gameboard

/** Returns a stable display name for a player based on mappings and fallbacks. */
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
