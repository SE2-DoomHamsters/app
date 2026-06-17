package com.doomhamsters.ui.lobby

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doomhamsters.LobbyViewModel
import com.doomhamsters.R
import com.doomhamsters.data.decodeBase64ToBitmap
import kotlin.collections.forEach
import kotlin.collections.orEmpty

    /** Shows the active lobby membership, QR code, and start controls. */
    @Composable
    fun ActiveLobbyScreen(viewModel: LobbyViewModel) {
        val lobbyState by viewModel.lobby.collectAsState()
        val isConnected by viewModel.isConnected.collectAsState()
        val startAvailabilityMessage = viewModel.startAvailabilityMessage()
        val canStartGame = viewModel.canCurrentUserStartGame()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Lobby ID: ${lobbyState?.lobbyId}", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))

            lobbyState?.qrCodeBase64?.let { base64 ->
                decodeBase64ToBitmap(base64)?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                }
            } ?: Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("QR-Code wird geladen...", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))
            val members = lobbyState?.members.orEmpty()
            Text("Spieler in der Lobby: ${members.size}")

            if (members.size < 2) {
                val infiniteTransition = rememberInfiniteTransition(label = "waiting")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Warte auf Spieler...",
                    modifier = Modifier.alpha(alpha),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                members.forEach { member ->
                    Text("${member.avatar} ${member.username}")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            startAvailabilityMessage?.let { message ->
                Text(
                    text = message,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = { viewModel.startGame() },
                modifier = Modifier.fillMaxWidth(),
                enabled = canStartGame
            ) {
                if (viewModel.isStartingGame) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Spiel für alle starten")
                }
            }

            Spacer (Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.leaveLobby() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.leave_lobby_text))
            }
        }
    }