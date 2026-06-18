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
import com.doomhamsters.LobbyConnectionManager
import com.doomhamsters.model.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    // 1. ZUERST alle StateFlows deklarieren, damit sie bereitstehen...
    private val _lobby = MutableStateFlow<Lobby?>(null)
    val lobby: StateFlow<Lobby?> = _lobby

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _activeGameSession = MutableStateFlow<GameSession?>(null)
    val activeGameSession: StateFlow<GameSession?> = _activeGameSession

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    // 2. ERST JETZT den connectionManager instanziieren!
    private val connectionManager = LobbyConnectionManager(
        repository = repository,
        connectionFlow = _isConnected,
        infoMessageFlow = _infoMessage,
        enableLobbyRefresh = enableLobbyRefresh,
        scope = viewModelScope,
        getCurrentLobby = { _lobby.value },
        onLobbySnapshot = { applyLobbySnapshot(it) }
    )

    init {
        viewModelScope.launch { tryResumeActiveGame() }
    }

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

    private var lastCompletedGameId: String? = null
    private var lastAttemptedAction: (() -> Unit)? = null
    val currentUserId: String
        get() = userId

    val observedLobbyId: String?
        get() = connectionManager.observedLobbyId

    /**
     * Checks whether a previous game session was saved and is still active on the backend.
     * If so, skips the lobby flow and navigates directly to the game board (step 4).
     * On a 404 or finished game the saved session is cleared.
     * On a network error the saved session is kept so the next launch can retry.
     */
    private suspend fun tryResumeActiveGame() {
        val savedGameId = sessionStore.loadActiveGameId() ?: return
        val playerName = sessionStore.loadUsername() ?: return
        val gameRepo = GameRepository(BackendConfig.BASE_URL)
        val state = try {
            gameRepo.fetchGameStateOrNull(savedGameId, userId)
        } catch (e: Exception) {
            Log.d(TAG, "Could not verify saved game $savedGameId (network?): ${e.message}")
            return
        }
        if (state == null || state.status == Status.Finished) {
            sessionStore.clearActiveGame()
            return
        }
        sessionStore.loadActiveLobbyId()?.let { savedLobbyId ->
            connectionManager.observedLobbyId = savedLobbyId
            try {
                repository.getLobby(savedLobbyId)?.let { lobby -> _lobby.value = lobby }
            } catch (e: Exception) {
                Log.d(TAG, "Could not load lobby $savedLobbyId on resume: ${e.message}")
            }
        }
        _activeGameSession.value = GameSession(
            gameId = savedGameId,
            playerId = userId,
            playerName = playerName
        )
        currentStep = 4
        Log.d(TAG, "Resumed active game $savedGameId for player $userId")
    }

    /** Creates a lobby with the currently entered profile details. */
    fun createGroup() {
        if (username.isBlank() || groupName.isBlank() || isProfileActionInProgress) return
        lastAttemptedAction = { createGroup() }

        viewModelScope.launch {
            try {
                isProfileActionInProgress = true
                _isLoading.value = true
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
                connectionManager.observeLobby(createdLobby.lobbyId)
            } catch (e: Exception) {
                _error.value = mapThrowableToMessage(e)
            } finally {
                isProfileActionInProgress = false
                _isLoading.value = false
            }
        }
    }

    /** Joins the specified lobby with the currently entered profile details. */
    fun joinLobby(scannedLobbyId: String) {
        val normalizedLobbyId = scannedLobbyId.trim()
        lastAttemptedAction = { joinLobby(normalizedLobbyId) }
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
                _isLoading.value = true
                _error.value = null
                _activeGameSession.value = null
                lastCompletedGameId = null
                isStartingGame = false
                Log.d(TAG, "Joining lobby=$normalizedLobbyId as user=$userId name=$username")
                sessionStore.saveProfile(username = username, avatar = selectedAvatar)

                connectionManager.observeLobby(normalizedLobbyId)
                val user = User(userId, username, selectedAvatar)
                val joinedLobby = repository.joinLobby(normalizedLobbyId, user)

                if (joinedLobby == null) {
                    connectionManager.clearObservedLobby()
                    _lobby.value = null
                    isStartingGame = false
                    _error.value = "Lobby '$normalizedLobbyId' wurde nicht gefunden!"
                    return@launch
                }

                currentStep = 3
                applyLobbySnapshot(joinedLobby)
                connectionManager.observeLobby(joinedLobby.lobbyId)
            } catch (e: Exception) {
                connectionManager.clearObservedLobby()
                _error.value = mapThrowableToMessage(e)
            } finally {
                isProfileActionInProgress = false
                _isLoading.value = false
            }
        }
    }

    /**
     * Verlässt die aktuelle Lobby und setzt den Navigationsstatus zurück
     * Informiert das Backend über den Austritt, bricht alle aktiven Lobby-Observer ab
     * und navigiert den Nutzer zurück zum Startbildschirm
     */
    fun leaveLobby() {
        val lobbyId = _lobby.value?.lobbyId
        viewModelScope.launch {
            try {
                if (lobbyId != null) {
                    repository.leaveLobby(lobbyId, userId)
                }
            } catch (e: Exception) {
                // Ignored
            } finally {
                connectionManager.lobbyUpdatesJob?.cancel()
                repository.disconnect()
                _lobby.value = null
                currentStep = 1
            }
        }
    }

    /** Requests game start when the current lobby is ready. */
    fun startGame() {
        val currentLobbyId = _lobby.value?.lobbyId ?: return
        lastAttemptedAction = { startGame() }
        if (isStartingGame) return
        if (!canCurrentUserStartGame()) {
            _error.value = startAvailabilityMessage()
            return
        }

        viewModelScope.launch {
            try {
                isStartingGame = true
                _isLoading.value = true
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
                    _isLoading.value = false
                    _error.value = mapThrowableToMessage(e)
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
        sessionStore.clearActiveGame()
        lastCompletedGameId = _activeGameSession.value?.gameId
        _activeGameSession.value = null
        val lobbyIdToResume = _lobby.value?.lobbyId ?: connectionManager.observedLobbyId
        if (!lobbyIdToResume.isNullOrBlank()) {
            currentStep = 3
            viewModelScope.launch {
                try {
                    connectionManager.observeLobby(lobbyIdToResume)
                } catch (error: Exception) {
                    Log.d(TAG, "Failed to resume lobby observation for $lobbyIdToResume: ${error.message}")
                }
            }
        } else {
            currentStep = 1
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
        sessionStore.saveActiveGameId(startedGameId, lobby.lobbyId)
        _activeGameSession.value = session
        currentStep = 4
        viewModelScope.launch {
            connectionManager.pauseLobbyObservationForActiveGame()
        }
    }

    /** Releases lobby observers and network resources when the view model is disposed. */
    override fun onCleared() {
        Log.d(TAG, "LobbyViewModel.onCleared for lobby=${connectionManager.observedLobbyId} user=$userId")
        connectionManager.cancelLobbyObservers()
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

    /** Clears the current error message. */
    fun clearError() {
        _error.value = null
    }

    /** Clears the current info message. */
    fun clearInfoMessage() {
        _infoMessage.value = null
    }

    /** Retries the last failed action. */
    fun retryLastAction() {
        val action = lastAttemptedAction
        clearError()
        action?.invoke()
    }

    private fun mapThrowableToMessage(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("timeout", ignoreCase = true) -> "Die Verbindung zum Server hat zu lange gedauert. Bitte versuche es erneut."
            msg.contains("Failed to connect", ignoreCase = true) -> "Der Server ist aktuell nicht erreichbar. Prüfe deine Internetverbindung oder die Server-Adresse."
            msg.contains("Lobby", ignoreCase = true) && msg.contains("not found", ignoreCase = true) -> "Die eingegebene Lobby-ID existiert nicht."
            msg.contains("full", ignoreCase = true) -> "Diese Lobby ist leider schon voll."
            else -> "Etwas ist schiefgelaufen: ${e.localizedMessage ?: "Unbekannter Fehler"}"
        }
    }
}