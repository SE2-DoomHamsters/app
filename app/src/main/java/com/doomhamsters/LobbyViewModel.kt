package com.doomhamsters

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doomhamsters.data.Lobby
import com.doomhamsters.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LobbyViewModel(
    private val repository: LobbyRepository = LobbyRepository("10.39.198.20:53217"),
    private val userId: String = UUID.randomUUID().toString()
) : ViewModel() {
    // 1 = Start, 2 = Profil-Setup, 3 = Aktive Lobby, 4 = Gameboard
    var currentStep by mutableStateOf(1)
    var groupName by mutableStateOf("")
    var username by mutableStateOf("")
    var selectedAvatar by mutableStateOf("dog")

    private val _lobby = MutableStateFlow<Lobby?>(null)
    val lobby: StateFlow<Lobby?> = _lobby

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error


    fun createGroup() {
        if (username.isBlank() || groupName.isBlank()) return
        viewModelScope.launch {
            try {
                _error.value = null
                repository.connect()
                val user = User(userId, username, selectedAvatar)
                val createdLobby = repository.createLobby(groupName, user)
                _lobby.value = createdLobby

                // Keep lobby state in sync whenever another player joins
                launch {
                    repository.subscribeLobbyUpdates(createdLobby.lobbyId)
                        .collect { updated -> _lobby.value = updated }
                }

                currentStep = 3
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    fun joinLobby(scannedLobbyId: String) {
        if (username.isBlank()) {
            _error.value = "Bitte gib zuerst deinen Spielernamen ein!"
            return
        }

        viewModelScope.launch {
            try {
                _error.value = null
                repository.connect()
                val user = User(userId, username, selectedAvatar)

                val joinedLobby = repository.joinLobby(scannedLobbyId, user)

                if (joinedLobby != null) {
                    _lobby.value = joinedLobby

                    launch {
                        repository.subscribeLobbyUpdates(joinedLobby.lobbyId)
                            .collect { updated -> _lobby.value = updated }
                    }

                    currentStep = 3
                } else {
                    _error.value = "Lobby '$scannedLobbyId' wurde nicht gefunden!"
                }

            } catch (e: Exception) {
                _error.value = "Fehler beim Beitreten: ${e.message}"
            }
        }
    }




    fun startGame() {
        currentStep = 4
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { repository.disconnect() }
    }
}
