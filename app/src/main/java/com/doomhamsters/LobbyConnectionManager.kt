package com.doomhamsters

import android.util.Log
import com.doomhamsters.data.Lobby
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LobbyConnectionManager(
    private val repository: LobbyRepository,
    private val connectionFlow: MutableStateFlow<Boolean>,
    private val infoMessageFlow: MutableStateFlow<String?>,
    private val enableLobbyRefresh: Boolean,
    private val scope: CoroutineScope,
    private val getCurrentLobby: () -> Lobby?,
    private val onLobbySnapshot: (Lobby) -> Unit
) {
    companion object {
        const val TAG = "LobbyDebug"
    }

    var observedLobbyId: String? = null
    var lobbyUpdatesJob: Job? = null
    var gameStartJob: Job? = null
    var lobbyRefreshJob: Job? = null

    suspend fun observeLobby(lobbyId: String) {
        val sameLobby = observedLobbyId == lobbyId
        val refreshActive = lobbyRefreshJob?.isActive == true
        val listenersActive = refreshActive &&
                lobbyUpdatesJob?.isActive == true &&
                gameStartJob?.isActive == true
        if (sameLobby && listenersActive) return

        Log.d(TAG, "Observing lobby=$lobbyId")
        stopLobbyObservers()
        observedLobbyId = lobbyId
        if (enableLobbyRefresh) {
            launchLobbyRefresh(lobbyId)
        }
        ensureRealtimeLobbyObservers(lobbyId)
    }

    suspend fun ensureRealtimeLobbyObservers(lobbyId: String) {
        if (lobbyUpdatesJob?.isActive == true && gameStartJob?.isActive == true) {
            connectionFlow.value = repository.isConnected()
            return
        }
        if (!repository.isConnected()) {
            try {
                connectionFlow.value = false
                repository.connect()
                connectionFlow.value = true
            } catch (error: Exception) {
                Log.d(TAG, "Realtime lobby connection unavailable for $lobbyId: ${error.message}")
                return
            }
        }
        if (lobbyUpdatesJob?.isActive != true) {
            launchLobbyUpdates(lobbyId)
        }
        if (gameStartJob?.isActive != true) {
            launchGameStartListener(lobbyId)
        }
        connectionFlow.value = repository.isConnected()
    }

    fun launchLobbyUpdates(lobbyId: String) {
        lobbyUpdatesJob = scope.launch {
            try {
                repository.subscribeLobbyUpdates(lobbyId)
                    .catch { error ->
                        Log.d(TAG, "Lobby updates failed for $lobbyId: ${error.message}")
                        infoMessageFlow.value = "Verbindung wird im Hintergrund aktualisiert..."
                        observedLobbyId = null
                    }
                    .collect { onLobbySnapshot(it) }
            } catch (e: Exception) {
                Log.d(TAG, "Lobby updates setup failed for $lobbyId: ${e.message}")
                infoMessageFlow.value = "Verbindung wird im Hintergrund aktualisiert..."
                observedLobbyId = null
            }
        }
        lobbyUpdatesJob?.invokeOnCompletion { error ->
            Log.d(
                TAG,
                "Lobby updates job completed for lobby=$lobbyId reason=${error?.javaClass?.simpleName ?: "normal"} message=${error?.message}"
            )
        }
    }

    fun launchGameStartListener(lobbyId: String) {
        gameStartJob = scope.launch {
            repository.subscribeGameStart(lobbyId)
                .catch { error ->
                    Log.d(TAG, "Game start listener failed for $lobbyId: ${error.message}")
                    infoMessageFlow.value = "Spielstart-Synchronisierung eingeschränkt."
                    observedLobbyId = null
                }
                .collect { newGameId ->
                    Log.d(TAG, "Received game start for lobby=$lobbyId gameId=$newGameId")
                    val snapshot = (getCurrentLobby() ?: Lobby(lobbyId = lobbyId)).copy(
                        gameId = newGameId,
                        gameStarted = true
                    )
                    onLobbySnapshot(snapshot)
                }
        }
        gameStartJob?.invokeOnCompletion { error ->
            Log.d(
                TAG,
                "Game start job completed for lobby=$lobbyId reason=${error?.javaClass?.simpleName ?: "normal"} message=${error?.message}"
            )
        }
    }

    fun launchLobbyRefresh(lobbyId: String) {
        lobbyRefreshJob = scope.launch {
            while (isActive) {
                try {
                    ensureRealtimeLobbyObservers(lobbyId)
                    repository.getLobby(lobbyId)?.let { onLobbySnapshot(it) }
                } catch (error: Exception) {
                    Log.d(TAG, "Lobby refresh failed for $lobbyId: ${error.message}")
                }
                delay(1500)
            }
        }
        lobbyRefreshJob?.invokeOnCompletion { error ->
            Log.d(
                TAG,
                "Lobby refresh job completed for lobby=$lobbyId reason=${error?.javaClass?.simpleName ?: "normal"} message=${error?.message}"
            )
        }
    }

    suspend fun stopLobbyObservers() {
        Log.d(TAG, "Stopping lobby observers for lobby=$observedLobbyId")
        lobbyUpdatesJob?.cancelAndJoin()
        gameStartJob?.cancelAndJoin()
        lobbyRefreshJob?.cancelAndJoin()
        clearLobbyObserverRefs()
    }

    fun cancelLobbyObservers() {
        Log.d(TAG, "Cancelling lobby observers for lobby=$observedLobbyId")
        lobbyUpdatesJob?.cancel()
        gameStartJob?.cancel()
        lobbyRefreshJob?.cancel()
        clearLobbyObserverRefs()
    }

    private fun clearLobbyObserverRefs() {
        lobbyUpdatesJob = null
        gameStartJob = null
        lobbyRefreshJob = null
    }

    suspend fun pauseLobbyObservationForActiveGame() {
        Log.d(TAG, "Pausing lobby observers for active game lobby=$observedLobbyId")
        stopLobbyObservers()
        repository.disconnect()
        connectionFlow.value = false
    }

    suspend fun clearObservedLobby() {
        stopLobbyObservers()
        observedLobbyId = null
        connectionFlow.value = false
    }
}