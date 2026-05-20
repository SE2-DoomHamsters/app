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
    private val repository: LobbyRepository = LobbyRepository(BackendConfig.BASE_URL),
    private val userId: String = UUID.randomUUID().toString(),
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

    var currentStep by mutableIntStateOf(1)
    var groupName by mutableStateOf(generateLobbyName())
    var username by mutableStateOf(generatePlayerName())
    var selectedAvatar by mutableStateOf("dog")
    var isProfileActionInProgress by mutableStateOf(false)
        private set
    var isStartingGame by mutableStateOf(false)
        private set

    private val _lobby = MutableStateFlow<Lobby?>(null)
    val lobby: StateFlow<Lobby?> = _lobby

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _activeGameSession = MutableStateFlow<GameSession?>(null)
    val activeGameSession: StateFlow<GameSession?> = _activeGameSession

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var observedLobbyId: String? = null
    private var lobbyUpdatesJob: Job? = null
    private var gameStartJob: Job? = null
    private var lobbyRefreshJob: Job? = null
    private var lastCompletedGameId: String? = null
    private var lastAttemptedAction: (() -> Unit)? = null
    val currentUserId: String
        get() = userId

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

                delay(2000) //zum Testen

                Log.d(TAG, "Creating lobby as user=$userId name=$username")

                val user = User(userId, username, selectedAvatar)
                val createdLobby = repository.createLobby(groupName, user)
                currentStep = 3
                applyLobbySnapshot(createdLobby)
                observeLobby(createdLobby.lobbyId)
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
                _error.value = mapThrowableToMessage(e)
            } finally {
                isProfileActionInProgress = false
                _isLoading.value = false
            }
        }
    }

    fun leaveLobby() {
        val lobbyId = _lobby.value?.lobbyId
        viewModelScope.launch {
            try {
                if (lobbyId != null) {
                    repository.leaveLobby(lobbyId, userId)
                }
            } catch (e: Exception) {

            } finally {
                lobbyUpdatesJob?.cancel()
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
                    _isLoading.value = true
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
        if (lobbyUpdatesJob?.isActive == true && gameStartJob?.isActive == true) {
            _isConnected.value = repository.isConnected()
            return
        }
        if (!repository.isConnected()) {
            try {
                _isConnected.value = false
                repository.connect()
                _isConnected.value = true
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
        _isConnected.value = repository.isConnected()
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
        _activeGameSession.value = GameSession(
            gameId = startedGameId,
            playerId = userId,
            playerName = username
        )
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
        _isConnected.value = false
    }

    private suspend fun clearObservedLobby() {
        stopLobbyObservers()
        observedLobbyId = null
        _isConnected.value = false
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

    /** Clears the current error message. */
    fun clearError() {
        _error.value = null
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
