package com.doomhamsters.viewmodel
import android.util.Log
import com.doomhamsters.GameRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ConnectionStatus {
    object Connected : ConnectionStatus()
    object Disconnected : ConnectionStatus()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionStatus()
    object Failed : ConnectionStatus()
}
class GameConnectionManager(private val gameId: String,
                            private val repository: GameRepository
) {
    private val tag = "GameConnectionManager"

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    val initialConnectionRetryDelaysMs = listOf(0L, 5_000L, 10_000L, 15_000L)
    private val maxReconnectAttempts = 3
    val reconnectBackoffMs = listOf(5_000L, 10_000L, 15_000L)
    private suspend fun subscribeWithReconnect(localPlayerId: String,
                                               onReconnect: suspend () -> Unit,
                                               onGameStateReceived: suspend (com.doomhamsters.model.GameState) -> Unit,
                                               onPublicEventReceived: suspend (org.json.JSONObject) -> Unit,
                                               onPrivateEventReceived: suspend (org.json.JSONObject) -> Unit,
                                               onFatalError: suspend (String) -> Unit,
                                               onLog: (String) -> Unit) {
        var attempt = 0
        while (attempt <= maxReconnectAttempts) {
            if (attempt > 0) {
                _connectionStatus.value = ConnectionStatus.Reconnecting(attempt, maxReconnectAttempts)
                val delayMs = reconnectBackoffMs.getOrElse(attempt - 1) { reconnectBackoffMs.last() }
                delay(delayMs)
                try {
                    runCatching { repository.disconnect() }
                    repository.connect()

                    onReconnect()

                    _connectionStatus.value = ConnectionStatus.Connected
                    onLog("Reconnected.")

                    attempt = 0
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(tag, "Reconnect attempt $attempt/$maxReconnectAttempts failed gameId=$gameId", e)
                    attempt++
                    continue
                }
            }

            try {
                coroutineScope {
                    launch {
                        repository.subscribeToGameState(gameId)
                            .collect { state ->
                                onGameStateReceived(state)
                            }
                    }
                    launch {
                        repository.subscribeToGame(gameId)
                            .collect { payload ->
                                runCatching { org.json.JSONObject(payload) }.getOrNull()
                                    ?.let { event ->
                                        onPublicEventReceived(event)
                                    }
                            }
                    }
                    launch {
                        repository.subscribeToPrivateEvents(gameId, localPlayerId)
                            .collect { event ->
                                onPrivateEventReceived(event)
                            }
                    }
                }
                Log.w(tag, "Subscriptions completed attempt=$attempt/$maxReconnectAttempts gameId=$gameId")
                _connectionStatus.value = ConnectionStatus.Disconnected
                attempt++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(tag, "Subscription lost attempt=$attempt/$maxReconnectAttempts gameId=$gameId", e)
                _connectionStatus.value = ConnectionStatus.Disconnected
                attempt++
            }
        }
        Log.e(tag, "Max reconnect attempts exhausted gameId=$gameId")
        _connectionStatus.value = ConnectionStatus.Failed

        onFatalError("The hamster died of heart attack. RIP... Please restart the game.")
    }
    suspend fun connectAndMaintain(
        localPlayerId: String,
        localPlayerName: String,
        onInitialConnect: suspend () -> Unit,
        onReconnect: suspend () -> Unit,
        onGameStateReceived: suspend (com.doomhamsters.model.GameState) -> Unit,
        onPublicEventReceived: suspend (org.json.JSONObject) -> Unit,
        onPrivateEventReceived: suspend (org.json.JSONObject) -> Unit,
        onFatalError: suspend (String) -> Unit,
        onLog: (String) -> Unit
    ) {
        val connected = establishInitialConnection(localPlayerId, localPlayerName, onFatalError)
        if (!connected) {
            _connectionStatus.value = ConnectionStatus.Failed
            return
        }
        try {
            _connectionStatus.value = ConnectionStatus.Connected
            onInitialConnect()
        } catch (e: Exception) {
            Log.e(tag, "Error in onInitialConnect callback", e)
            onFatalError("Initialization failed: ${e.message}")
            _connectionStatus.value = ConnectionStatus.Failed
            return
        }

        subscribeWithReconnect(
            localPlayerId = localPlayerId,
            onReconnect = onReconnect,
            onGameStateReceived = onGameStateReceived,
            onPublicEventReceived = onPublicEventReceived,
            onPrivateEventReceived = onPrivateEventReceived,
            onFatalError = onFatalError,
            onLog = onLog
        )}
    private suspend fun establishInitialConnection(localPlayerId: String,
                                                   localPlayerName: String,
                                                   onFatalError: suspend (String) -> Unit): Boolean {
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
        //_error.emit("Connection error: ${finalError.message}")
        onFatalError("Connection error: ${finalError.message}")
        return false
    }
}