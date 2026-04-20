package com.doomhamsters
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomhamsters.data.decodeBase64ToBitmap

@Composable
fun MainLobbyNavigation(viewModel: LobbyViewModel) {
    // Hier wird entschieden: Welchen Screen zeigen wir gerade?
    when (viewModel.currentStep) {
        1 -> ProfileSetupScreen(viewModel)
        2 -> ActiveLobbyScreen(viewModel)
        3 -> GameBoardScreen()
    }
}

@Composable
fun ProfileSetupScreen(viewModel: LobbyViewModel) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Lobby erstellen", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(20.dp))

            // 1.NAME
            OutlinedTextField(
                value = viewModel.username,
                onValueChange = { viewModel.username = it },
                label = { Text("Dein Spielername") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // 2. GRUPPENNAME
            OutlinedTextField(
                value = viewModel.groupName, // Hier wird der Name der Gruppe gespeichert
                onValueChange = { viewModel.groupName = it },
                label = { Text("Name der Gruppe / Lobby") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(20.dp))

            // 3.EMOJIS
            Text("Wähle dein Spieler-Icon:")

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                maxItemsInEachRow = 4
            ) {
                val icons = listOf("🐱", "🐶", "🐷", "🦊", "🤖", "👽", "🐭")
                icons.forEach { emoji ->
                    val isSelected = viewModel.selectedAvatar == emoji
                    Box(
                        modifier = Modifier
                            .padding(8.dp).size(60.dp)
                            .border(
                                3.dp,
                                if (isSelected) Color(0xFF6200EE) else Color.Transparent,
                                CircleShape
                            )
                            .clickable { viewModel.selectedAvatar = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 36.sp)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // 4. BUTTON
            Button(
                onClick = { viewModel.createGroup() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                // Button ist nur klickbar, wenn Name UND Gruppenname ausgefüllt sind
                enabled = viewModel.username.isNotBlank() && viewModel.groupName.isNotBlank()
            ) {
                Text("Gruppe erstellen")
            }
        }
    }

@Composable
fun ActiveLobbyScreen(viewModel: LobbyViewModel) {
    val lobbyState by viewModel.lobby.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Lobby ID: ${lobbyState?.lobbyId}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        // Hier wird der QR-Code angezeigt, der vom Backend kommt
        lobbyState?.qrCodeBase64?.let { base64 ->
            decodeBase64ToBitmap(base64)?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(200.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Spieler bereit: ${lobbyState?.members?.size ?: 0}")

        // Liste der Spieler
        lobbyState?.members?.forEach { member ->
            Text("${member.avatar} ${member.username}")
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = {
            viewModel.startGame() }, modifier = Modifier.fillMaxWidth()) {
            Text("SPIEL STARTEN")
        }
    }
}
@Composable
fun GameBoardScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "GAMEBOARD",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}