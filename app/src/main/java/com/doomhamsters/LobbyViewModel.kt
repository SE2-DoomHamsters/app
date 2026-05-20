package com.doomhamsters



import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doomhamsters.data.GameSession
import com.doomhamsters.data.Lobby
import com.doomhamsters.data.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.random.Random

/** Owns lobby setup, membership, and game-launch state for the frontend flow. */
class LobbyViewModel(
    private val sessionStore: SessionStore,
    private val repository: LobbyRepository = LobbyRepository(BackendConfig.BASE_URL),
    private val enableLobbyRefresh: Boolean = true
) : ViewModel() {
    companion object {
        const val TAG = "LobbyDebug"
        private val letters = ('A'..'Z').toList()
        private fun generatePlayerName(): String =
            buildString {
                repeat(2) { append(letters.random()) }
            }

        private fun generateLobbyName(): String =
            Random.nextInt(0, 10).toString()
    }

    private val userId: String = sessionStore.getOrCreateUserId()

    var currentStep by mutableIntStateOf(1)
    var groupName by mutableStateOf(generateLobbyName())
    private var usernameState by mutableStateOf(
        sessionStore.loadUsername() ?: generatePlayerName()
    )
    var username: String
        get() = usernameState
        set(value) {
            usernameState = value
            sessionStore.saveProfile(username = value, avatar = selectedAvatar)
        }
    private var selectedAvatarState by mutableStateOf(sessionStore.loadAvatar() ?: "dog")
    var selectedAvatar: String
        get() = selectedAvatarState
        set(value) {
            selectedAvatarState = value
            sessionStore.saveProfile(username = username, avatar = value)
        }
    var isProfileActionInProgress by mutableStateOf(false)
        private set
    var isStartingGame by mutableStateOf(false)
        private set

    private val _lobby = MutableStateFlow<Lobby?>(null)
    val lobby: StateFlow<Lobby?> = _lobby

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _activeGameSession = MutableStateFlow<GameSession?>(null)
    val activeGameSession: StateFlow<GameSession?> = _activeGameSession

    private var observedLobbyId: String? = null
    private var lobbyUpdatesJob: Job? = null
    private var gameStartJob: Job? = null
    private var lobbyRefreshJob: Job? = null
    private var lastCompletedGameId: String? = null
    val currentUserId: String
        get() = userId

    /** Creates a lobby with the currently entered profile details. */
    fun createGroup() {
        if (username.isBlank() || groupName.isBlank() || isProfileActionInProgress) return

        viewModelScope.launch {
            try {
                isProfileActionInProgress = true
                _error.value = null
                _activeGameSession.value = null
                lastCompletedGameId = null
                isStartingGame = false
                Log.d(TAG, "Creating lobby as user=$userId name=$username")
                sessionStore.saveProfile(username = username, avatar = selectedAvatar)

                val user = User(userId, username, selectedAvatar)
                val createdLobby = repository.createLobby(groupName, user)
                currentStep = 3
                applyLobbySnapshot(createdLobby)
                observeLobby(createdLobby.lobbyId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                isProfileActionInProgress = false
            }
        }
    }

    /** Joins the specified lobby with the currently entered profile details. */
    fun joinLobby(scannedLobbyId: String) {
        val normalizedLobbyId = scannedLobbyId.trim()
        if (username.isBlank()) {
            _error.value = "Bitte gib zuerst deinen Spielernamen ein!"
            return
        }
        if (normalizedLobbyId.isBlank()) {
            _error.value = "Bitte gib eine gultige Lobby-ID ein!"
            return
        }
        if (isProfileActionInProgress) return

        viewModelScope.launch {
            try {
                isProfileActionInProgress = true
                _error.value = null
                _activeGameSession.value = null
                lastCompletedGameId = null
                isStartingGame = false
                Log.d(TAG, "Joining lobby=$normalizedLobbyId as user=$userId name=$username")
                sessionStore.saveProfile(username = username, avatar = selectedAvatar)

                observeLobby(normalizedLobbyId)
                val user = User(userId, username, selectedAvatar)
                val joinedLobby = repository.joinLobby(normalizedLobbyId, user)

                if (joinedLobby == null) {
                    clearObservedLobby()
                    _lobby.value = null
                    isStartingGame = false
                    _error.value = "Lobby '$normalizedLobbyId' wurde nicht gefunden!"
                    return@launch
                }

                currentStep = 3
                applyLobbySnapshot(joinedLobby)
                observeLobby(joinedLobby.lobbyId)
            } catch (e: Exception) {
                clearObservedLobby()
                _error.value = "Fehler beim Beitreten: ${e.message}"
            } finally {
                isProfileActionInProgress = false
            }
        }
    }

    /** Requests game start when the current lobby is ready. */
    fun startGame() {
        val currentLobbyId = _lobby.value?.lobbyId ?: return
        if (isStartingGame) return
        if (!canCurrentUserStartGame()) {
            _error.value = startAvailabilityMessage()
            return
        }

        viewModelScope.launch {
            try {
                isStartingGame = true
                _error.value = null
                Log.d(TAG, "Starting game for lobby=$currentLobbyId")
                repository.triggerGameStart(currentLobbyId, userId)
                repository.getLobby(currentLobbyId)?.let(::applyLobbySnapshot)
            } catch (e: Exception) {
                val refreshedLobby = runCatching { repository.getLobby(currentLobbyId) }.getOrNull()
                if (refreshedLobby != null) {
                    applyLobbySnapshot(refreshedLobby)
                }
                if (refreshedLobby?.gameStarted != true && refreshedLobby?.gameId.isNullOrBlank()) {
                    isStartingGame = false
                    _error.value = "Konnte Spiel nicht starten: ${e.message}"
                }
            }
        }
    }

    /** Returns the UI to the active lobby after leaving a game. */
    fun returnToLobbyAfterGame() {
        Log.d(
            TAG,
            "Returning to lobby from game=${_activeGameSession.value?.gameId}, lobby=${_lobby.value?.lobbyId}"
        )
        lastCompletedGameId = _activeGameSession.value?.gameId
        _activeGameSession.value = null
        currentStep = 3
        val lobbyIdToResume = _lobby.value?.lobbyId ?: observedLobbyId
        if (!lobbyIdToResume.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    observeLobby(lobbyIdToResume)
                } catch (error: Exception) {
                    Log.d(TAG, "Failed to resume lobby observation for $lobbyIdToResume: ${error.message}")
                }
            }
        }
    }

    private suspend fun observeLobby(lobbyId: String) {
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

    private suspend fun ensureRealtimeLobbyObservers(lobbyId: String) {
        if (lobbyUpdatesJob?.isActive == true && gameStartJob?.isActive == true) return
        if (!repository.isConnected()) {
            try {
                repository.connect()
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
    }

    private fun launchLobbyUpdates(lobbyId: String) {
        lobbyUpdatesJob = viewModelScope.launch {
            repository.subscribeLobbyUpdates(lobbyId)
                .catch { error ->
                    Log.d(TAG, "Lobby updates failed for $lobbyId: ${error.message}")
                    _error.value = "Lobby-Updates abgebrochen: ${error.message}"
                    observedLobbyId = null
                }
                .collect(::applyLobbySnapshot)
        }
        lobbyUpdatesJob?.invokeOnCompletion { error ->
            Log.d(
                TAG,
                "Lobby updates job completed for lobby=$lobbyId reason=${error?.javaClass?.simpleName ?: "normal"} message=${error?.message}"
            )
        }
    }

    private fun launchGameStartListener(lobbyId: String) {
        gameStartJob = viewModelScope.launch {
            repository.subscribeGameStart(lobbyId)
                .catch { error ->
                    Log.d(TAG, "Game start listener failed for $lobbyId: ${error.message}")
                    _error.value = "Spielstart-Listener abgebrochen: ${error.message}"
                    observedLobbyId = null
                }
                .collect { newGameId ->
                    Log.d(TAG, "Received game start for lobby=$lobbyId gameId=$newGameId")
                    val snapshot = (_lobby.value ?: Lobby(lobbyId = lobbyId)).copy(
                        gameId = newGameId,
                        gameStarted = true
                    )
                    applyLobbySnapshot(snapshot)
                }
        }
        gameStartJob?.invokeOnCompletion { error ->
            Log.d(
                TAG,
                "Game start job completed for lobby=$lobbyId reason=${error?.javaClass?.simpleName ?: "normal"} message=${error?.message}"
            )
        }
    }

    private fun launchLobbyRefresh(lobbyId: String) {
        lobbyRefreshJob = viewModelScope.launch {
            while (isActive) {
                try {
                    ensureRealtimeLobbyObservers(lobbyId)
                    repository.getLobby(lobbyId)?.let(::applyLobbySnapshot)
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

    private fun applyLobbySnapshot(lobby: Lobby) {
        Log.d(
            TAG,
            "Applying lobby snapshot lobbyId=${lobby.lobbyId} members=${lobby.members.size} names=${lobby.members.map { it.username }} gameId=${lobby.gameId} started=${lobby.gameStarted} currentStep=$currentStep"
        )
        _lobby.value = lobby

        val startedGameId = lobby.gameId?.takeIf { it.isNotBlank() } ?: return
        isStartingGame = false
        if (startedGameId == lastCompletedGameId) return
        if (_activeGameSession.value?.gameId == startedGameId && currentStep == 4) return

        Log.d(TAG, "Opening game session gameId=$startedGameId for user=$userId")
        val session = GameSession(
            gameId = startedGameId,
            playerId = userId,
            playerName = username
        )
        _activeGameSession.value = session
        currentStep = 4
        viewModelScope.launch {
            pauseLobbyObservationForActiveGame()
        }
    }

    private suspend fun stopLobbyObservers() {
        Log.d(TAG, "Stopping lobby observers for lobby=$observedLobbyId")
        lobbyUpdatesJob?.cancelAndJoin()
        gameStartJob?.cancelAndJoin()
        lobbyRefreshJob?.cancelAndJoin()
        clearLobbyObserverRefs()
    }

    private fun cancelLobbyObservers() {
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

    private suspend fun pauseLobbyObservationForActiveGame() {
        Log.d(TAG, "Pausing lobby observers for active game lobby=$observedLobbyId")
        stopLobbyObservers()
        repository.disconnect()
    }

    private suspend fun clearObservedLobby() {
        stopLobbyObservers()
        observedLobbyId = null
    }

    /** Releases lobby observers and network resources when the view model is disposed. */
    override fun onCleared() {
        Log.d(TAG, "LobbyViewModel.onCleared for lobby=$observedLobbyId user=$userId")
        cancelLobbyObservers()
        runBlocking {
            repository.disconnect()
        }
        super.onCleared()
    }

    /** Returns whether the current user may start the game now. */
    fun canCurrentUserStartGame(): Boolean {
        val currentLobby = _lobby.value ?: return false
        if (isStartingGame || currentLobby.gameStarted) return false
        if (currentLobby.members.size < 2) return false
        return currentLobby.members.any { it.id == userId }
    }

    /** Explains why the game cannot be started yet, if applicable. */
    fun startAvailabilityMessage(): String? {
        val currentLobby = _lobby.value ?: return null
        if (isStartingGame || currentLobby.gameStarted) {
            return "Spielstart wird verarbeitet..."
        }
        if (currentLobby.members.size < 2) {
            return "Mindestens 2 Spieler werden zum Starten benotigt."
        }
        if (currentLobby.members.none { it.id == userId }) {
            return "Warte auf die Lobby-Synchronisierung."
        }
        return null
    }
}
