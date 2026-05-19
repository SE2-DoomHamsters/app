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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

open class GameBoardViewModel(
    val gameId: String,
    initialLocalPlayerId: String,
    val localPlayerName: String,
    private val repository: GameRepository
) : ViewModel() {
    private companion object {
        val initialConnectionRetryDelaysMs = listOf(0L, 500L, 1_000L, 2_000L)
    }

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
    private val _isActivatingCard = MutableStateFlow(false)
    private var awaitingLocalDrawOutcome = false
    private var doomOutcomeLogged = false
    private var pendingPrivateDoomCard: Card? = null

    init {
        connectAndObserveRemoteState()
    }

    private fun connectAndObserveRemoteState() {
        viewModelScope.launch {
            val connected = establishInitialConnection()
            if (!connected) {
                return@launch
            }

            try {
                launch {
                    repository.subscribeToGameState(gameId)
                        .catch { e ->
                            Log.e(tag, "State sync error gameId=$gameId", e)
                            _error.emit("State sync error: ${e.message}")
                        }
                        .collect {
                            Log.d(
                                tag,
                                "WS state received gameId=$gameId currentPlayerId=${it.currentTurnPlayerId} turnCount=${it.turnCount} status=${it.status}"
                            )
                            applyGameState(it, resolvePlayerId = false)
                        }
                }

                launch {
                    repository.subscribeToGame(gameId)
                        .catch { e ->
                            Log.e(tag, "Public event sync error gameId=$gameId", e)
                            _error.emit("Event sync error: ${e.message}")
                        }
                        .collect { payload ->
                            runCatching { JSONObject(payload) }
                                .getOrNull()
                                ?.let(::handlePublicGameEvent)
                        }
                }

                launch {
                    repository.subscribeToPrivateEvents(gameId, localPlayerId)
                        .catch { e ->
                            Log.e(tag, "Private sync error gameId=$gameId playerId=$localPlayerId", e)
                            _error.emit("Private sync error: ${e.message}")
                        }
                        .collect(::handlePrivateEvent)
                }
            } catch (e: Exception) {
                Log.e(tag, "Connection error gameId=$gameId", e)
                _error.emit("Connection error: ${e.message}")
            }
        }
    }

    private suspend fun establishInitialConnection(): Boolean {
        var lastError: Exception? = null

        for ((attemptIndex, retryDelayMs) in initialConnectionRetryDelaysMs.withIndex()) {
            if (retryDelayMs > 0) {
                delay(retryDelayMs)
            }

            try {
                Log.d(
                    tag,
                    "Connecting attempt=${attemptIndex + 1}/${initialConnectionRetryDelaysMs.size} gameId=$gameId localPlayerId=$localPlayerId localPlayerName=$localPlayerName"
                )
                repository.connect()
                refreshGameState(resolvePlayerId = true)
                return true
            } catch (e: Exception) {
                lastError = e
                Log.e(
                    tag,
                    "Connection attempt failed attempt=${attemptIndex + 1}/${initialConnectionRetryDelaysMs.size} gameId=$gameId",
                    e
                )
                runCatching { repository.disconnect() }
            }
        }

        val finalError = lastError ?: IllegalStateException("Unknown connection error")
        Log.e(tag, "Connection error gameId=$gameId", finalError)
        _error.emit("Connection error: ${finalError.message}")
        return false
    }

    private fun handlePublicGameEvent(event: JSONObject) {
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
                Log.d(tag, "Updating localPlayerId from snapshot oldId=$localPlayerId newId=$resolvedPlayerId")
                localPlayerId = resolvedPlayerId
            }
        }

        val mergedSnapshot = preserveLocalHand(snapshot)
        _gameState.value = mergedSnapshot
        _isLocalPlayersTurn.value = mergedSnapshot.currentTurnPlayerId == localPlayerId
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
        val previousLocalPlayer = previousState.players.firstOrNull { it.id == localPlayerId } ?: return snapshot
        val incomingLocalPlayer = snapshot.players.firstOrNull { it.id == localPlayerId } ?: return snapshot

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
            previousLocalPlayer.hand.any { it.type == CardType.SnackStash } &&
            incomingLocalPlayer.hand.count { it.type == CardType.SnackStash } <
                previousLocalPlayer.hand.count { it.type == CardType.SnackStash }
        ) {
            incomingLocalPlayer.hand.clear()
            incomingLocalPlayer.hand.addAll(previousLocalPlayer.hand)
        }

        return snapshot
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

        _pausedForDoomMessage.value = "$resolvingPlayerName was hit by Doom."
        _pausedForDoomDetail.value = "They are acknowledging the life loss before play continues."
    }

    private fun resolveLocalDoomNotice(previousState: GameState?, currentState: GameState) {
        val hasPendingLocalDoom =
            currentState.resolvingDoomPlayerId == localPlayerId && pendingPrivateDoomCard != null

        if (hasPendingLocalDoom && _pendingDoom.value == null) {
            if (currentState.pendingDoomRequiresInsertion) {
                showDoomNotice(
                    card = pendingPrivateDoomCard ?: Card(CardType.Doom),
                    message = "Scroll to your Snack Stash and select it.",
                    requiresSelection = true,
                    requiresInsertionUi = false
                )
                pendingPrivateDoomCard = null
                awaitingLocalDrawOutcome = false
                return
            }

            showDoomNotice(
                card = pendingPrivateDoomCard ?: Card(CardType.Doom),
                message = "You lost 1 life."
            )
            pendingPrivateDoomCard = null
            awaitingLocalDrawOutcome = false
            if (!doomOutcomeLogged) {
                addLog("You drew a Doom card and lost 1 life.")
                doomOutcomeLogged = true
            }
            return
        }

        if (!awaitingLocalDrawOutcome) return

        val previousLocalPlayer = previousState?.players?.firstOrNull { it.id == localPlayerId } ?: return
        val currentLocalPlayer = currentState.players.firstOrNull { it.id == localPlayerId } ?: return

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

    open fun draw(playerId: String) {
        val isResolvingLocalDoom =
            _pendingDoom.value != null ||
                _gameState.value?.resolvingDoomPlayerId == localPlayerId

        if (playerId != localPlayerId || !_isLocalPlayersTurn.value || isResolvingLocalDoom) {
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
        awaitingLocalDrawOutcome = false
        if (_isLocalPlayersTurn.value) {
            Log.d(tag, "Auto-advancing turn after resolved draw gameId=$gameId")
            repository.sendAction(gameId, "nextTurn", JSONObject())
            broadcastLatestState()
        }
    }

    open fun insertDoom(position: Int) {
        val state = _gameState.value
        val isResolvingLocalDoom =
            state?.resolvingDoomPlayerId == localPlayerId &&
                state.pendingDoomRequiresInsertion &&
                _pendingDoomRequiresInsertionUi.value &&
                _pendingDoom.value != null

        if (!isResolvingLocalDoom) {
            Log.d(tag, "Insert doom ignored gameId=$gameId isResolvingLocalDoom=$isResolvingLocalDoom")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(tag, "Sending doom insert gameId=$gameId playerId=$localPlayerId position=$position")
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
                Log.e(tag, "Doom insert failed gameId=$gameId playerId=$localPlayerId position=$position", e)
                _error.emit("Doom insert failed: ${e.message}")
            }
        }
    }

    open fun defusePendingDoom(card: Card?) {
        viewModelScope.launch {
            _error.emit("Doom is resolved automatically by the server.")
        }
    }

    open fun acceptDoom() {
        viewModelScope.launch {
            _error.emit("Doom is resolved automatically by the server.")
        }
    }

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

    fun canActivateCard(card: Card): Boolean {
        val state = _gameState.value ?: return false
        val localPlayer = state.players.firstOrNull { it.id == localPlayerId } ?: return false
        val isResolvingLocalDoom =
            _pendingDoom.value != null ||
                state.resolvingDoomPlayerId == localPlayerId
        val command = CardRegistry.commandFor(card) ?: return false

        return !_isActivatingCard.value &&
            !isResolvingLocalDoom &&
            _isLocalPlayersTurn.value &&
            localPlayer.hand.any { it.id == card.id || (card.id == null && it == card) } &&
            command.actionPath.isNotBlank()
    }

    fun activateCard(card: Card) {
        val command = CardRegistry.commandFor(card) ?: return
        if (!canActivateCard(card)) {
            Log.d(tag, "Activate card ignored gameId=$gameId cardId=${card.id} cardType=${card.type}")
            return
        }

        viewModelScope.launch {
            try {
                _isActivatingCard.value = true
                Log.d(tag, "Sending card activation gameId=$gameId playerId=$localPlayerId cardId=${card.id} commandId=${command.id}")
                repository.sendAction(
                    gameId,
                    command.actionPath,
                    CardCommandRequest(
                        playerId = localPlayerId,
                        cardId = card.id,
                        cardType = card.type,
                        commandId = command.id
                    ).toJson()
                )
                broadcastLatestState()
            } catch (e: Exception) {
                Log.e(tag, "Card activation failed gameId=$gameId playerId=$localPlayerId cardId=${card.id}", e)
                _error.emit("Card activation failed: ${e.message}")
            } finally {
                _isActivatingCard.value = false
            }
        }
    }

    fun dismissDoomNotice(selectedPlayerCardIndex: Int = -1) {
        if (_pendingDoomRequiresSelection.value) {
            val localPlayer = _gameState.value?.players?.firstOrNull { it.id == localPlayerId }
            val selectedCard = localPlayer?.hand?.getOrNull(selectedPlayerCardIndex)
            val snackStash = selectedCard?.takeIf { it.type == CardType.SnackStash }
                ?: localPlayer?.hand?.firstOrNull { it.type == CardType.SnackStash }
            snackStash?.let { localPlayer?.hand?.remove(it) }
            _pendingDoomRequiresSelection.value = false
            _pendingDoomRequiresInsertionUi.value = true
            _pendingDoomMessage.value = null
            return
        }

        clearPendingDoomUi()

        if (_isLocalPlayersTurn.value) {
            viewModelScope.launch {
                try {
                    Log.d(tag, "Sending doom ack gameId=$gameId playerId=$localPlayerId")
                    repository.sendAction(gameId, "doom/ack", JSONObject().put("playerId", localPlayerId))
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
        pendingPrivateDoomCard = null
    }

    private fun resolveUniquePlayerIdByName(snapshot: GameState): String? {
        if (snapshot.players.any { it.id == localPlayerId }) return localPlayerId

        val matchingPlayers = snapshot.players.filter { it.name == localPlayerName }
        return matchingPlayers.singleOrNull()?.id
    }
}
