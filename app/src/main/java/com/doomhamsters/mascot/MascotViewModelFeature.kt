package com.doomhamsters.mascot

import com.doomhamsters.mascot.presentation.MascotAnimation
import com.doomhamsters.mascot.presentation.MascotPresentation
import com.doomhamsters.model.Card
import com.doomhamsters.viewmodel.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the board mascot animation state and the timing of transient reactions,
 * behind a single [animation] flow. Mirrors the SnackStashViewModelFeature
 * pattern: all state and logic live here so the composable stays a dumb renderer
 * and GameBoardViewModel only constructs it.
 *
 * @param scope coroutine scope the feature observes inputs and runs timers in (the ViewModel scope).
 * @param isLocalPlayersTurn whether it is currently the local player's turn.
 * @param pendingDoom the Doom card pending for the local player, or null.
 * @param cardPlayed emits once whenever the local player plays any card.
 * @param connectionStatus the live connection status.
 */
class MascotViewModelFeature(
    private val scope: CoroutineScope,
    isLocalPlayersTurn: StateFlow<Boolean>,
    pendingDoom: StateFlow<Card?>,
    cardPlayed: Flow<Unit>,
    connectionStatus: StateFlow<ConnectionStatus>,
) {
    // Transient one-shot flag for the "card played" reaction.
    private val cardPlayedActive = MutableStateFlow(false)
    private var cardPlayedJob: Job? = null

    private val _animation = MutableStateFlow(MascotAnimation.IDLE)

    /** The animation the mascot should currently display. */
    val animation: StateFlow<MascotAnimation> = _animation

    // Resting animation derived from the live game state (no transitions).
    private val baseAnimation: StateFlow<MascotAnimation> = combine(
        isLocalPlayersTurn,
        pendingDoom,
        connectionStatus,
        cardPlayedActive,
    ) { turn, doom, connection, played ->
        MascotPresentation.resolve(
            isLocalPlayersTurn = turn,
            hasPendingDoom = doom != null,
            connectionStatus = connection,
            cardPlayedActive = played,
        )
    }.stateIn(scope, SharingStarted.Eagerly, MascotAnimation.IDLE)

    init {
        // Any card the local player plays triggers the transient flash.
        scope.launch {
            cardPlayed.collect { flashCardPlayed() }
        }
        // Drive the visible animation, inserting the TURNING transition when the
        // resting state flips between IDLE and YOUR_TURN. collectLatest cancels an
        // in-flight transition if the resting state changes again mid-turn.
        scope.launch {
            var previous = baseAnimation.value
            baseAnimation.collectLatest { target ->
                val transition = MascotPresentation.transitionFor(previous, target)
                previous = target
                if (transition != null) {
                    _animation.value = transition
                    delay(transition.holdMs ?: 0L)
                }
                _animation.value = target
            }
        }
    }

    private fun flashCardPlayed() {
        cardPlayedJob?.cancel()
        cardPlayedJob = scope.launch {
            cardPlayedActive.value = true
            delay(MascotAnimation.CARD_PLAYED.holdMs ?: 0L)
            cardPlayedActive.value = false
        }
    }
}
