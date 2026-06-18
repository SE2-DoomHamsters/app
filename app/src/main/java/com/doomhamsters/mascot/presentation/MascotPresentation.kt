package com.doomhamsters.mascot.presentation

import com.doomhamsters.viewmodel.ConnectionStatus

/** Maps current game state/events to the mascot animation that should play. */
object MascotPresentation {
    /**
     * Picks the resting animation for the current state. Higher branches win when
     * several conditions are true at once.
     *
     * @param isLocalPlayersTurn whether it is the local player's turn.
     * @param hasPendingDoom whether a Doom card is pending for the local player.
     * @param connectionStatus the live connection status.
     * @param cardPlayedActive whether a transient card-played reaction is active.
     * @return the animation that should currently play.
     */
    fun resolve(
        isLocalPlayersTurn: Boolean,
        hasPendingDoom: Boolean,
        connectionStatus: ConnectionStatus,
        cardPlayedActive: Boolean,
    ): MascotAnimation = when {
        connectionStatus != ConnectionStatus.Connected -> MascotAnimation.DISCONNECTED
        hasPendingDoom -> MascotAnimation.DOOM
        cardPlayedActive -> MascotAnimation.CARD_PLAYED
        isLocalPlayersTurn -> MascotAnimation.YOUR_TURN
        else -> MascotAnimation.IDLE
    }

    /**
     * Transition animation to play between two resting states, or null if the
     * change should be instant. IDLE <-> YOUR_TURN gets a directional turn;
     * the reverse direction plays the reversed turn.
     *
     * @param from the resting animation being left.
     * @param to the resting animation being entered.
     * @return the transition animation to play, or null for an instant swap.
     */
    fun transitionFor(from: MascotAnimation, to: MascotAnimation): MascotAnimation? = when {
        from == MascotAnimation.IDLE && to == MascotAnimation.YOUR_TURN -> MascotAnimation.TURNING
        from == MascotAnimation.YOUR_TURN && to == MascotAnimation.IDLE -> MascotAnimation.TURNING_BACK
        else -> null
    }

    /**
     * Whether a swap should play the squash-and-stretch: true when the incoming
     * animation wants it on entry or the outgoing one wants it on exit.
     *
     * @param from the animation being left.
     * @param to the animation being entered.
     * @return true if this swap should squash.
     */
    fun shouldSquash(from: MascotAnimation, to: MascotAnimation): Boolean =
        to.squashOnSwap || from.squashOnExit
}
