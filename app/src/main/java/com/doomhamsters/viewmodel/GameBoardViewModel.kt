package com.doomhamsters.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doomhamsters.GameRepository
import com.doomhamsters.cards.CardRegistry
import com.doomhamsters.cards.CardCommandEvent
import com.doomhamsters.cards.CardCommandEventType
import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.cards.CardCommandNotice
import com.doomhamsters.cards.CardCommandRequest
import com.doomhamsters.cards.displayName
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Status
import com.doomhamsters.cheating.SnackStashUiEffect
import com.doomhamsters.cheating.SnackStashViewModelFeature
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.doomhamsters.ui.gameboard.GameBoardUiState
import org.json.JSONObject

/**
 * Represents the live state of the STOMP/WebSocket connection
 * Guarantees compile-tim exhaustive checks in when() expressions.
 */

/** Owns realtime game state, local Doom handling, and card actions for the game board. */
open class GameBoardViewModel(
    val gameId: String,
    initialLocalPlayerId: String,
    val localPlayerName: String,
    private val repository: GameRepository,
    private val connectionManager: GameConnectionManager = GameConnectionManager(gameId, repository)
) : ViewModel() {

    private val tag = "GameBoardViewModel"

    var localPlayerId: String = initialLocalPlayerId
        private set

    protected val _gameState = MutableStateFlow<GameState?>(null)
    open val gameState: StateFlow<GameState?> = _gameState

    private val _isLocalPlayersTurn = MutableStateFlow(false)
    val isLocalPlayersTurn: StateFlow<Boolean> = _isLocalPlayersTurn

    protected val _gameOver = MutableSharedFlow<String>()
    open val gameOver: SharedFlow<String> = _gameOver

    protected val _error = MutableSharedFlow<String>()
    open val error: SharedFlow<String> = _error
    val connectionStatus: StateFlow<ConnectionStatus> = connectionManager.connectionStatus

    protected val _log = MutableStateFlow<List<String>>(emptyList())
    open val log: StateFlow<List<String>> = _log

    protected val _pendingDoom = MutableStateFlow<Card?>(null)
    open val pendingDoom: StateFlow<Card?> = _pendingDoom
    private val _pendingDoomMessage = MutableStateFlow<String?>(null)
    val pendingDoomMessage: StateFlow<String?> = _pendingDoomMessage
    private val _pendingDoomRequiresSelection = MutableStateFlow(false)
    val pendingDoomRequiresSelection: StateFlow<Boolean> = _pendingDoomRequiresSelection
    private val _pendingDoomRequiresInsertionUi = MutableStateFlow(false)
    val pendingDoomRequiresInsertionUi: StateFlow<Boolean> = _pendingDoomRequiresInsertionUi
    private val _pausedForDoomPlayerName = MutableStateFlow<String?>(null)
    val pausedForDoomPlayerName: StateFlow<String?> = _pausedForDoomPlayerName
    private val _pausedForDoomMessage = MutableStateFlow<String?>(null)
    val pausedForDoomMessage: StateFlow<String?> = _pausedForDoomMessage
    private val _pausedForDoomDetail = MutableStateFlow<String?>(null)
    val pausedForDoomDetail: StateFlow<String?> = _pausedForDoomDetail


    private val _cardCommandNotice = MutableStateFlow<CardCommandNotice?>(null)
    val cardCommandNotice: StateFlow<CardCommandNotice?> = _cardCommandNotice
    private val _showTargetSelectionDialog = MutableStateFlow(false)
    val showTargetSelectionDialog: StateFlow<Boolean> = _showTargetSelectionDialog
    private val _selectedCardForActivation = MutableStateFlow<Card?>(null)
    val selectedCardForActivation: StateFlow<Card?> = _selectedCardForActivation
    val snackStash = SnackStashViewModelFeature(
        gameId = gameId,
        localPlayerId = { localPlayerId },
        isLocalPlayersTurn = { _isLocalPlayersTurn.value },
        pendingDoomRequiresSelection = { _pendingDoomRequiresSelection.value },
        gameState = { _gameState.value },
        sendAction = { action, payload -> repository.sendAction(gameId, action, payload) },
        refreshGameState = { broadcastLatestState() },
        showWaitingForVotes = { applySnackStashEffect(SnackStashUiEffect.WaitingForVotes) },
        clearPendingDoomUi = { clearPendingDoomUi() },
        launchAction = ::launchSnackStashAction,
        logDebug = { message -> Log.d(tag, message) }
    )
    private val _isActivatingCard = MutableStateFlow(false)

    val uiState: StateFlow<GameBoardUiState> = combine(
        _gameState,
        _isLocalPlayersTurn,
        _pendingDoom,
        _pendingDoomMessage,
        _pendingDoomRequiresSelection,
        _pendingDoomRequiresInsertionUi,
        _pausedForDoomPlayerName,
        _pausedForDoomMessage,
        _pausedForDoomDetail,
        _cardCommandNotice,
        connectionManager.connectionStatus
    ) { flows ->
        GameBoardUiState(
            gameState = flows[0] as GameState?,
            isLocalPlayersTurn = flows[1] as Boolean,
            pendingDoom = flows[2] as Card?,
            pendingDoomMessage = flows[3] as String?,
            pendingDoomRequiresSelection = flows[4] as Boolean,
            pendingDoomRequiresInsertionUi = flows[5] as Boolean,
            pausedForDoomPlayerName = flows[6] as String?,
            pausedForDoomMessage = flows[7] as String?,
            pausedForDoomDetail = flows[8] as String?,
            cardCommandNotice = flows[9] as CardCommandNotice?,
            connectionStatus = flows[10] as ConnectionStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameBoardUiState()
    )
    private var awaitingLocalDrawOutcome = false
    private var doomOutcomeLogged = false
    private var pendingPrivateDoomCard: Card? = null

    init {
        connectAndObserveRemoteState()
    }

    private val cardActivation = CardActivationHandler(
        gameId = gameId,
        repository = repository,
        scope = viewModelScope,
        error = _error,
        getLocalPlayerId = { localPlayerId },
        getGameState = { _gameState.value },
        isPendingDoom = { _pendingDoom.value != null },
        isLocalPlayersTurn = { _isLocalPlayersTurn.value },
        onStateChanged = { broadcastLatestState() }
    )

    val pendingTargetedCard: StateFlow<Card?> = cardActivation.pendingTargetedCard

    private fun connectAndObserveRemoteState() {
        viewModelScope.launch {
            connectionManager.connectAndMaintain(
                localPlayerId = localPlayerId,
                localPlayerName = localPlayerName,
                onInitialConnect = { refreshGameState(resolvePlayerId = true) },
                onReconnect = { refreshGameState(resolvePlayerId = false) },
                onGameStateReceived = { state ->
                    runCatching { applyGameState(state, resolvePlayerId = false) }
                        .onFailure { e -> Log.e(tag, "State apply error gameId=$gameId", e) }
                },
                onPublicEventReceived = { event ->
                    runCatching { handlePublicGameEvent(event) }
                },
                onPrivateEventReceived = { event ->
                    runCatching { handlePrivateEvent(event) }
                },
                onFatalError = { errorMsg -> _error.emit(errorMsg) },
                onServerError = { message -> handleServerError(message) },
                onLog = { msg -> addLog(msg) }
            )
        }
    }

    private fun handlePublicGameEvent(event: JSONObject) {
        val snackStashEventResult = snackStash.handlePublicEvent(event)
        if (snackStashEventResult.handled) {
            applySnackStashEffect(snackStashEventResult.effect)
            snackStashEventResult.logMessage?.let(::addLog)
            return
        }

        val parsedEvent = CardCommandEvent.fromJsonOrNull(event) ?: return
        if (parsedEvent.type != CardCommandEventType.CARD_COMMAND_PLAYED) return

        val message = parsedEvent.message ?: buildString {
            append(parsedEvent.playerName ?: "A player")
            parsedEvent.commandId?.let { commandId ->
                val commandName = commandId.name
                    .lowercase()
                    .split('_')
                    .joinToString(" ") { token -> token.replaceFirstChar(Char::titlecase) }
                append(" activated $commandName.")
            }
        }
        addLog(message)
    }


    private suspend fun handleServerError(message: String) {
        addLog("Error: $message")
        _error.emit(message)
    }

    private fun handlePrivateEvent(event: JSONObject) {
        when (event.optString("type").trim().uppercase()) {
            "DOOM_DRAWN" -> {
                val cardJson = event.optJSONObject("card")
                pendingPrivateDoomCard = cardJson?.let(Card::fromJson) ?: Card(CardType.Doom)
                awaitingLocalDrawOutcome = false
                if (!doomOutcomeLogged) {
                    addLog("You drew a Doom card.")
                    doomOutcomeLogged = true
                }
                _gameState.value?.let { currentState ->
                    resolveLocalDoomNotice(currentState, currentState)
                }
            }

            CardCommandEventType.CARD_COMMAND_RESULT.name -> {
                val parsedEvent = CardCommandEvent.fromJsonOrNull(event) ?: return
                when (parsedEvent.commandId) {
                    CardCommandId.QUICK_PEEK -> {
                        val title = parsedEvent.card?.displayName() ?: "Quick Peek"
                        val message = parsedEvent.message
                            ?: parsedEvent.revealedCard?.let { "Top card: ${it.displayName()}." }
                            ?: "The deck is empty."
                        _cardCommandNotice.value = CardCommandNotice(
                            title = title,
                            message = message,
                            revealedCard = parsedEvent.revealedCard
                        )
                    }

                    CardCommandId.STEAL_CARD -> {
                        val title = parsedEvent.card?.displayName() ?: "Tiny Thief"
                        val stolenCardName = parsedEvent.revealedCard?.displayName() ?: "a card"

                        _cardCommandNotice.value = CardCommandNotice(
                            title = title,
                            message = "You stole $stolenCardName!",
                            revealedCard = parsedEvent.revealedCard
                        )
                    }

                    CardCommandId.SNIFF_AHEAD -> {
                        val title = parsedEvent.card?.displayName() ?: "Sniff Ahead"
                        val message = parsedEvent.message
                            ?: if (parsedEvent.revealedCards.isEmpty()) "The deck is empty."
                            else "Top ${parsedEvent.revealedCards.size} card(s): ${
                                parsedEvent.revealedCards.joinToString(", ") { it.displayName() }
                            }."
                        _cardCommandNotice.value = CardCommandNotice(
                            title = title,
                            message = message,
                            revealedCards = parsedEvent.revealedCards
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    private suspend fun refreshGameState(resolvePlayerId: Boolean) {
        Log.d(
            tag,
            "Refreshing state gameId=$gameId localPlayerId=$localPlayerId resolvePlayerId=$resolvePlayerId"
        )
        var snapshot = repository.fetchGameState(gameId, localPlayerId)

        if (resolvePlayerId) {
            val resolvedPlayerId = resolveUniquePlayerIdByName(snapshot)

            if (!resolvedPlayerId.isNullOrBlank() && resolvedPlayerId != localPlayerId) {
                Log.d(
                    tag,
                    "Resolved local player id by name localPlayerName=$localPlayerName oldId=$localPlayerId newId=$resolvedPlayerId"
                )
                localPlayerId = resolvedPlayerId
                snapshot = repository.fetchGameState(gameId, localPlayerId)
            }
        }

        applyGameState(snapshot, resolvePlayerId = false)
    }

    private fun applyGameState(snapshot: GameState, resolvePlayerId: Boolean) {
        val previousState = _gameState.value
        if (resolvePlayerId) {
            val resolvedPlayerId = resolveUniquePlayerIdByName(snapshot)

            if (!resolvedPlayerId.isNullOrBlank() && resolvedPlayerId != localPlayerId) {
                Log.d(
                    tag,
                    "Updating localPlayerId from snapshot oldId=$localPlayerId newId=$resolvedPlayerId"
                )
                localPlayerId = resolvedPlayerId
            }
        }

        val mergedSnapshot = preserveLocalHand(snapshot)
        _gameState.value = mergedSnapshot
        _isLocalPlayersTurn.value = mergedSnapshot.currentTurnPlayerId == localPlayerId
        applySnackStashEffect(snackStash.syncFromGameState(mergedSnapshot))
        syncDoomResolutionState(mergedSnapshot)
        resolveLocalDoomNotice(previousState, mergedSnapshot)
        Log.d(
            tag,
            "Applied state gameId=$gameId localPlayerId=$localPlayerId currentPlayerId=${mergedSnapshot.currentTurnPlayerId} turnCount=${mergedSnapshot.turnCount} isLocalPlayersTurn=${_isLocalPlayersTurn.value} pendingDoom=${_pendingDoom.value != null}"
        )
        checkGameOver(mergedSnapshot)
    }

    private fun preserveLocalHand(snapshot: GameState): GameState {
        val previousState = _gameState.value ?: return snapshot
        val previousLocalPlayer =
            previousState.players.firstOrNull { it.id == localPlayerId } ?: return snapshot
        val incomingLocalPlayer =
            snapshot.players.firstOrNull { it.id == localPlayerId } ?: return snapshot

        val isResolvingLocalDoom = snapshot.resolvingDoomPlayerId == localPlayerId
        val incomingHandSize = incomingLocalPlayer.visibleHandSize()
        if (
            incomingLocalPlayer.hand.isEmpty() &&
            previousLocalPlayer.hand.isNotEmpty() &&
            incomingHandSize == previousLocalPlayer.hand.size
        ) {
            incomingLocalPlayer.hand.addAll(previousLocalPlayer.hand)
        }

        if (
            isResolvingLocalDoom &&
            !snapshot.pendingDoomRequiresInsertion &&
            previousLocalPlayer.hand.any { it.type == CardType.SnackStash } &&
            incomingLocalPlayer.hand.count { it.type == CardType.SnackStash } <
            previousLocalPlayer.hand.count { it.type == CardType.SnackStash }
        ) {
            incomingLocalPlayer.hand.clear()
            incomingLocalPlayer.hand.addAll(previousLocalPlayer.hand)
        }

        return snapshot
    }

    private fun launchSnackStashAction(userErrorMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "$userErrorMessage gameId=$gameId playerId=$localPlayerId", e)
                _error.emit("$userErrorMessage: ${e.message}")
            }
        }
    }

    private fun applySnackStashEffect(effect: SnackStashUiEffect) {
        when (effect) {
            SnackStashUiEffect.None -> Unit
            SnackStashUiEffect.WaitingForVotes -> {
                _pendingDoomRequiresSelection.value = false
                _pendingDoomMessage.value = "Waiting for votes."
            }
            SnackStashUiEffect.ClearDoomSelection -> {
                _pendingDoomRequiresSelection.value = false
                _pendingDoomRequiresInsertionUi.value = false
                _pendingDoomMessage.value = null
            }
        }
    }

    private fun syncDoomResolutionState(state: GameState) {
        val resolvingPlayerId = state.resolvingDoomPlayerId
        if (resolvingPlayerId.isNullOrBlank()) {
            _pausedForDoomPlayerName.value = null
            _pausedForDoomMessage.value = null
            _pausedForDoomDetail.value = null
            if (_pendingDoom.value == null) {
                pendingPrivateDoomCard = null
            }
            return
        }

        if (resolvingPlayerId == localPlayerId) {
            _pausedForDoomPlayerName.value = null
            _pausedForDoomMessage.value = null
            _pausedForDoomDetail.value = null
            return
        }

        val resolvingPlayerName = state.players
            .firstOrNull { it.id == resolvingPlayerId }
            ?.name
            ?: "A player"
        _pausedForDoomPlayerName.value = resolvingPlayerName
        if (state.pendingDoomRequiresInsertion) {
            _pausedForDoomMessage.value = "$resolvingPlayerName disabled Doom with Snack Stash."
            _pausedForDoomDetail.value = "They are choosing where to reinsert the Doom card."
            return
        }

        _pausedForDoomMessage.value = "$resolvingPlayerName drew Doom."
        _pausedForDoomDetail.value = "They can claim Snack Stash or accept the hit."
    }

    private fun resolveLocalDoomNotice(previousState: GameState?, currentState: GameState) {
        val hasPendingLocalDoom =
            currentState.resolvingDoomPlayerId == localPlayerId && pendingPrivateDoomCard != null

        if (
            currentState.resolvingDoomPlayerId == localPlayerId &&
            !currentState.pendingDoomRequiresInsertion &&
            currentState.pendingSnackStashClaim == null &&
            _pendingDoom.value == null
        ) {
            showDoomNotice(
                card = pendingPrivateDoomCard ?: Card(CardType.Doom),
                message = "Use Snack Stash, bluff with another card, or accept Doom.",
                requiresSelection = true
            )
            pendingPrivateDoomCard = null
            awaitingLocalDrawOutcome = false
            return
        }

        if (hasPendingLocalDoom && _pendingDoom.value == null) {
            if (currentState.pendingDoomRequiresInsertion) {
                showDoomNotice(
                    card = pendingPrivateDoomCard ?: Card(CardType.Doom),
                    message = "",
                    requiresSelection = false,
                    requiresInsertionUi = true
                )
                pendingPrivateDoomCard = null
                awaitingLocalDrawOutcome = false
                return
            }

            showDoomNotice(
                card = pendingPrivateDoomCard ?: Card(CardType.Doom),
                message = "Use Snack Stash, bluff with another card, or accept Doom.",
                requiresSelection = true
            )
            awaitingLocalDrawOutcome = false
            if (!doomOutcomeLogged) {
                addLog("You drew a Doom card.")
                doomOutcomeLogged = true
            }
            return
        }

        if (
            currentState.resolvingDoomPlayerId == localPlayerId &&
            currentState.pendingDoomRequiresInsertion &&
            _pendingDoom.value != null &&
            !_pendingDoomRequiresInsertionUi.value
        ) {
            _pendingDoomRequiresSelection.value = false
            _pendingDoomRequiresInsertionUi.value = true
            _pendingDoomMessage.value = null
            pendingPrivateDoomCard = null
            awaitingLocalDrawOutcome = false
            return
        }

        if (!awaitingLocalDrawOutcome) return

        val previousLocalPlayer =
            previousState?.players?.firstOrNull { it.id == localPlayerId } ?: return
        val currentLocalPlayer =
            currentState.players.firstOrNull { it.id == localPlayerId } ?: return

        if (currentLocalPlayer.lives < previousLocalPlayer.lives) {
            showDoomNotice(
                card = Card(CardType.Doom),
                message = "You lost 1 life."
            )
            awaitingLocalDrawOutcome = false
            addLog("You drew a Doom card and lost 1 life.")
            doomOutcomeLogged = true
            return
        }

        if (
            currentLocalPlayer.hand.size > previousLocalPlayer.hand.size ||
            currentLocalPlayer.hand.count { it.type == CardType.SnackStash } <
            previousLocalPlayer.hand.count { it.type == CardType.SnackStash } ||
            currentState.currentTurnPlayerId != localPlayerId
        ) {
            awaitingLocalDrawOutcome = false
        }
    }

    private suspend fun broadcastLatestState() {
        Log.d(tag, "Refreshing latest state gameId=$gameId")
        val snapshot = repository.fetchGameState(gameId, localPlayerId)
        applyGameState(snapshot, resolvePlayerId = false)
    }

    /** Disconnects the game repository when the view model is disposed. */
    override fun onCleared() {
        runBlocking {
            repository.disconnect()
        }
        super.onCleared()
    }

    private fun checkGameOver(state: GameState) {
        if (state.status == Status.Finished) {
            val winner = state.players.find { it.isAlive() }
            viewModelScope.launch { _gameOver.emit(winner?.id ?: "Unknown") }
        }
    }

    protected fun addLog(message: String) {
        _log.value = (_log.value + message).takeLast(10)
        Log.d(tag, "UI log: $message")
    }

    /** Attempts to draw a card for the local player. */
    open fun draw(playerId: String) {
        val isResolvingLocalDoom =
            _pendingDoom.value != null ||
                    _gameState.value?.resolvingDoomPlayerId == localPlayerId

        if (playerId != localPlayerId || !_isLocalPlayersTurn.value || isResolvingLocalDoom ||
            awaitingLocalDrawOutcome ||
            _isActivatingCard.value
        ) {
            Log.d(
                tag,
                "Draw ignored gameId=$gameId requestedPlayerId=$playerId localPlayerId=$localPlayerId isLocalPlayersTurn=${_isLocalPlayersTurn.value} isResolvingLocalDoom=$isResolvingLocalDoom"
            )
            return
        }

        viewModelScope.launch {
            try {
                Log.d(tag, "Sending draw gameId=$gameId playerId=$playerId")
                awaitingLocalDrawOutcome = true
                doomOutcomeLogged = false
                repository.sendAction(gameId, "draw", JSONObject().put("playerId", playerId))
                broadcastLatestState()
                resolveDrawOutcome()
            } catch (e: Exception) {
                awaitingLocalDrawOutcome = false
                Log.e(tag, "Draw failed gameId=$gameId playerId=$playerId", e)
                _error.emit("Action failed: ${e.message}")
            }
        }
    }

    private suspend fun resolveDrawOutcome() {
        delay(500)
        if (_pendingDoom.value != null) {
            awaitingLocalDrawOutcome = false
            return
        }
        if (_gameState.value?.resolvingDoomPlayerId == localPlayerId) {
            awaitingLocalDrawOutcome = false
            return
        }
        awaitingLocalDrawOutcome = false
        if (_isLocalPlayersTurn.value) {
            Log.d(tag, "Auto-advancing turn after resolved draw gameId=$gameId")
            repository.sendAction(gameId, "nextTurn", JSONObject())
            broadcastLatestState()
        }
    }

    /** Reinserts a resolved Doom card at the chosen deck position. */
    open fun insertDoom(position: Int) {
        val state = _gameState.value
        val isResolvingLocalDoom =
            state?.resolvingDoomPlayerId == localPlayerId &&
                    state.pendingDoomRequiresInsertion &&
                    _pendingDoomRequiresInsertionUi.value &&
                    _pendingDoom.value != null

        if (!isResolvingLocalDoom) {
            Log.d(
                tag,
                "Insert doom ignored gameId=$gameId isResolvingLocalDoom=$isResolvingLocalDoom"
            )
            return
        }

        viewModelScope.launch {
            try {
                Log.d(
                    tag,
                    "Sending doom insert gameId=$gameId playerId=$localPlayerId position=$position"
                )
                repository.sendAction(
                    gameId,
                    "doom/insert",
                    JSONObject()
                        .put("playerId", localPlayerId)
                        .put("position", position)
                )
                clearPendingDoomUi()
                broadcastLatestState()
                Log.d(tag, "Sending nextTurn after doom insert gameId=$gameId")
                repository.sendAction(gameId, "nextTurn", JSONObject())
                broadcastLatestState()
            } catch (e: Exception) {
                Log.e(
                    tag,
                    "Doom insert failed gameId=$gameId playerId=$localPlayerId position=$position",
                    e
                )
                _error.emit("Doom insert failed: ${e.message}")
            }
        }
    }

    /** Requests turn advancement when the local player is allowed to do so. */
    open fun advanceTurn() {
        if (!_isLocalPlayersTurn.value) {
            Log.d(
                tag,
                "Advance turn ignored gameId=$gameId isLocalPlayersTurn=${_isLocalPlayersTurn.value}"
            )
            return
        }

        viewModelScope.launch {
            try {
                Log.d(tag, "Sending nextTurn gameId=$gameId")
                repository.sendAction(gameId, "nextTurn", JSONObject())
                broadcastLatestState()
            } catch (e: Exception) {
                Log.e(tag, "Turn advance failed gameId=$gameId", e)
                _error.emit("Turn advance failed: ${e.message}")
            }
        }
    }

    /** Returns whether the local player can activate the supplied card right now. */
    fun canActivateCard(card: Card): Boolean = cardActivation.canActivate(card)

    /** Sends the activation request for a playable card. */
    //fun activateCard(card: Card) = cardActivation.activate(card)

    /** Called by the UI once the player has chosen a target and card type. */
    fun activateCardWithTargets(
        card: Card,
        targetPlayerId: String,
        requestedCardType: String,
        hamsterType: String? = null
    ) = cardActivation.activateWithTargets(card, targetPlayerId, requestedCardType, hamsterType)

    fun activateCard(card: Card, parameters: Map<String, Any> = emptyMap()) {
        val command = CardRegistry.commandFor(card) ?: return
        if (!canActivateCard(card)) {
            Log.d(
                tag,
                "Activate card ignored gameId=$gameId cardId=${card.id} cardType=${card.type}"
            )
            return
        }
        if (card.type == CardType.StealCard && !parameters.containsKey("targetPlayerId")) {
            _selectedCardForActivation.value = card
            _showTargetSelectionDialog.value = true
            return
        }

        /** Cancels a pending targeted-card selection. */
        fun cancelTargetedCardSelection() = cardActivation.cancelSelection()


        viewModelScope.launch {
            try {
                _isActivatingCard.value = true
                Log.d(
                    tag,
                    "Sending card activation gameId=$gameId playerId=$localPlayerId cardId=${card.id} commandId=${command.id}"
                )
                repository.sendAction(
                    gameId,
                    command.actionPath,
                    CardCommandRequest(
                        playerId = localPlayerId,
                        cardId = card.id,
                        cardType = card.type,
                        commandId = command.id
                    ).toJson().apply {
                        if (parameters.isNotEmpty()) {
                            val paramsJson = JSONObject()
                            parameters.forEach { (key, value) -> paramsJson.put(key, value) }
                            put(
                                "parameters",
                                paramsJson
                            ) // 'put' packt es direkt in das fertige toJson()
                        }
                    })
                broadcastLatestState()
            } catch (e: Exception) {
                Log.e(
                    tag,
                    "Card activation failed gameId=$gameId playerId=$localPlayerId cardId=${card.id}",
                    e
                )
                _error.emit("Card activation failed: ${e.message}")
            } finally {
                _isActivatingCard.value = false
            }
        }
    }

    fun selectStealTarget(targetPlayerId: String) {
        val card = _selectedCardForActivation.value ?: return
        _showTargetSelectionDialog.value = false // Dialog schließen

        // Jetzt feuern wir die Karte ab – diesmal MIT dem Parameter!
        activateCard(card, mapOf("targetPlayerId" to targetPlayerId))
        _selectedCardForActivation.value = null
    }

    /** Wird aufgerufen, wenn der Spieler den Auswahldialog abbricht */
    fun dismissTargetSelection() {
        _showTargetSelectionDialog.value = false
        _selectedCardForActivation.value = null
    }

    /** Advances the local Doom flow after the player acknowledges the current notice. */
    fun dismissDoomNotice(selectedPlayerCardIndex: Int = -1) {
        if (_pendingDoomRequiresSelection.value) {
            return
        }

        clearPendingDoomUi()

        if (_isLocalPlayersTurn.value) {
            viewModelScope.launch {
                try {
                    Log.d(tag, "Sending doom ack gameId=$gameId playerId=$localPlayerId")
                    repository.sendAction(
                        gameId,
                        "doom/ack",
                        JSONObject().put("playerId", localPlayerId)
                    )
                    broadcastLatestState()
                    Log.d(tag, "Sending nextTurn after doom acknowledgement gameId=$gameId")
                    repository.sendAction(gameId, "nextTurn", JSONObject())
                    broadcastLatestState()
                } catch (e: Exception) {
                    Log.e(tag, "Turn advance after doom acknowledgement failed gameId=$gameId", e)
                    _error.emit("Turn advance failed: ${e.message}")
                }
            }
        }
    }

    /** Clears the currently displayed card-command notice. */
    fun dismissCardCommandNotice() {
        _cardCommandNotice.value = null
    }

    private fun showDoomNotice(
        card: Card,
        message: String,
        requiresSelection: Boolean = false,
        requiresInsertionUi: Boolean = false
    ) {
        _pendingDoom.value = card
        _pendingDoomMessage.value = message
        _pendingDoomRequiresSelection.value = requiresSelection
        _pendingDoomRequiresInsertionUi.value = requiresInsertionUi
    }
    private fun clearPendingDoomUi() {
        _pendingDoom.value = null
        _pendingDoomMessage.value = null
        _pendingDoomRequiresSelection.value = false
        _pendingDoomRequiresInsertionUi.value = false
        _pausedForDoomPlayerName.value = null
        _pausedForDoomMessage.value = null
        _pausedForDoomDetail.value = null
        snackStash.clearClaim()
        pendingPrivateDoomCard = null
    }

    private fun resolveUniquePlayerIdByName(snapshot: GameState): String? {
        if (snapshot.players.any { it.id == localPlayerId }) return localPlayerId

        val matchingPlayers = snapshot.players.filter { it.name == localPlayerName }
        return matchingPlayers.singleOrNull()?.id
    }

}
