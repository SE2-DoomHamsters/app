package com.doomhamsters.ui.lobby

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.doomhamsters.LobbyViewModel
import com.doomhamsters.data.GameSession
import kotlin.collections.orEmpty

    /** Routes between the lobby flow screens and an active game session. */
    @Composable
    fun MainLobbyNavigation(viewModel: LobbyViewModel) {
        val activeGameSession by viewModel.activeGameSession.collectAsState()
        val lobbyState by viewModel.lobby.collectAsState()
        val errorState by viewModel.error.collectAsState()
        val isLoadingState by viewModel.isLoading.collectAsState()
        val infoMessage by viewModel.infoMessage.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        val playerAvatars = lobbyState?.members
            ?.flatMap { member -> listOf(member.id to member.avatar, member.username to member.avatar) }
            ?.toMap()
            .orEmpty()
        val playerNames = lobbyState?.members
            ?.associate { member -> member.id to member.username }
            .orEmpty()

        // Global Error Dialog
        errorState?.let { message ->
            ErrorDialog(
                message = message,
                onDismiss = { viewModel.clearError() },
                onRetry = {
                    viewModel.retryLastAction()
                }
            )
        }

        LaunchedEffect(infoMessage) {
            infoMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearInfoMessage()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {
                when (viewModel.currentStep) {
                    1 -> StartScreen(viewModel = viewModel)
                    2 -> ProfileSetupScreen(viewModel)
                    3 -> ActiveLobbyScreen(viewModel)
                    4 -> GameSessionContent(
                        session = activeGameSession,
                        playerAvatars = playerAvatars,
                        playerNames = playerNames,
                        onReturnToLobby = viewModel::returnToLobbyAfterGame
                    )

                    5 -> RulesScreen(onBackClick = { viewModel.currentStep = 1 })
                }
            }
        }
    }

    @Composable
    private fun GameSessionContent(
        session: GameSession?,
        playerAvatars: Map<String, String>,
        playerNames: Map<String, String>,
        onReturnToLobby: () -> Unit
    ) {
        session?.let {
            GameBoardScreen(
                gameId = it.gameId,
                playerId = it.playerId,
                playerName = it.playerName,
                playerAvatars = playerAvatars,
                playerNames = playerNames,
                onReturnToLobby = onReturnToLobby
            )
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    @Composable
    private fun ErrorDialog(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Oops!", color = MaterialTheme.colorScheme.error)
                }
            },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onRetry) {
                    Text("Nochmal versuchen")
                }
            }
        )
    }