package com.doomhamsters.cheating

/** Public result values sent by the backend after resolving a Snack Stash claim. */
enum class SnackStashResolutionOutcome {
    UNCHALLENGED,
    CHEATER,
    LEGITIMATE_CALL
}
