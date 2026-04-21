package com.doomhamsters
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.doomhamsters.data.Lobby
import com.doomhamsters.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LobbyViewModel : ViewModel() {
    // 1 = Profil-Setup, 2 = Aktive Lobby, 3 = Gameboard
    var currentStep by mutableStateOf(1)
    var groupName by mutableStateOf("")
    var username by mutableStateOf("")
    var selectedAvatar by mutableStateOf("dog")

    private val _lobby = MutableStateFlow<Lobby?>(null)
    val lobby: StateFlow<Lobby?> = _lobby

    fun createGroup() {
        if (username.isNotBlank() && groupName.isNotBlank()) {
            // HIER findet später der echte Netzwerk-Call statt

            // Simulation der Antwort vom Backend:
            _lobby.value = Lobby(
                lobbyId = groupName.uppercase(), // Wir nehmen den Namen als ID
                members = listOf(User("1", username, selectedAvatar)),
                qrCodeBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII="
            )
            currentStep = 2
        }
    }
    fun startGame() {
        // Logik
        currentStep = 3
    }
}