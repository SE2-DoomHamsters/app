package com.doomhamsters.ui

import com.doomhamsters.ui.gameboard.*
import com.doomhamsters.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.doomhamsters.viewmodel.GameBoardViewModel
import kotlinx.coroutines.launch


/** Renders the live game board for the local player. */
@Composable
fun GameBoard(
    viewModel: GameBoardViewModel,
    playerAvatars: Map<String, String> = emptyMap(),
    playerNames: Map<String, String> = emptyMap(),
    onGameOver: (String) -> Unit,
    onLeaveGame: () -> Unit = {}
) {
    val showTargetSelectionDialog by viewModel.showTargetSelectionDialog.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val gameState = uiState.gameState
    val isLocalPlayersTurn = uiState.isLocalPlayersTurn
    val pendingDoom = uiState.pendingDoom
    val pendingDoomMessage = uiState.pendingDoomMessage
    val pendingDoomRequiresSelection = uiState.pendingDoomRequiresSelection
    val pendingDoomRequiresInsertionUi = uiState.pendingDoomRequiresInsertionUi
    val pausedForDoomPlayerName = uiState.pausedForDoomPlayerName
    val pausedForDoomMessage = uiState.pausedForDoomMessage
    val pausedForDoomDetail = uiState.pausedForDoomDetail
    val cardCommandNotice = uiState.cardCommandNotice
    val connectionStatus = uiState.connectionStatus
    val screenState = rememberGameBoardState()

    LaunchedEffect(viewModel) {
        launch {
            viewModel.error.collect { screenState.latestError = it }
        }
        launch {
            viewModel.gameOver.collect(onGameOver)
        }
    }

    when (
        val resolution = resolveGameBoardResolution(
            gameState = gameState,
            latestError = screenState.latestError,
            localPlayerId = viewModel.localPlayerId
        )
    ) {
        is GameBoardResolution.Status -> {
            GameBoardStatus(resolution.message, onLeaveGame)
            return
        }

        is GameBoardResolution.Ready -> {
            val state = resolution.state
            val currentTurnPlayer = resolution.currentTurnPlayer
            val localPlayer = resolution.localPlayer

            val topOpponent = rememberTopOpponent(
                players = state.players,
                localPlayerId = localPlayer.id,
                currentTurnPlayerId = currentTurnPlayer.id
            )
            val currentTurnPlayerName = resolvePlayerDisplayName(
                playerId = currentTurnPlayer.id,
                fallbackName = currentTurnPlayer.name,
                playerNames = playerNames
            )
            val localPlayerName = resolvePlayerDisplayName(
                playerId = localPlayer.id,
                fallbackName = localPlayer.name,
                playerNames = playerNames
            )
            val topOpponentName = topOpponent?.let { opponent ->
                resolvePlayerDisplayName(
                    playerId = opponent.id,
                    fallbackName = opponent.name,
                    playerNames = playerNames
                )
            }
            val pausedRemotePlayerName = resolvePausedRemotePlayerName(
                rawName = pausedForDoomPlayerName,
                players = state.players,
                playerNames = playerNames
            )
            val opponentVisibleHandSize = topOpponent?.visibleHandSize() ?: 0
            val isResolvingLocalDoom =
                pendingDoom != null ||
                    state.resolvingDoomPlayerId == viewModel.localPlayerId
            val isLifeLossDoomOverlay =
                pendingDoom != null &&
                    !pendingDoomRequiresSelection &&
                    !pendingDoomRequiresInsertionUi

            ResetDoomInteractionState(
                isResolvingLocalDoom = isResolvingLocalDoom,
                pendingDoomRequiresSelection = pendingDoomRequiresSelection,
                onResetSelection = { screenState.selectedPlayerCardIndex = -1 },
                onResetSlider = { screenState.doomSliderPosition = 0f }
            )

            val handDrawState = rememberHandDrawState(
                localPlayerId = localPlayer.id,
                localHandSize = localPlayer.hand.size,
                opponentId = topOpponent?.id,
                opponentVisibleHandSize = opponentVisibleHandSize
            )

            val visibleTurns = buildVisibleTurnOrder(
                players = state.players,
                currentTurnPlayerId = state.currentTurnPlayerId,
                currentPlayerIndex = state.currentPlayerIndex
            )
            val localDrawStartOffset = remember(screenState.deckCenter, screenState.localHandCenter) {
                offsetBetween(screenState.deckCenter, screenState.localHandCenter)
            }
            val opponentDrawStartOffset = remember(screenState.deckCenter, screenState.opponentHandCenter) {
                offsetBetween(screenState.opponentHandCenter, screenState.deckCenter)
            }
            val handleDraw = createDrawHandler(
                isLocalPlayersTurn = isLocalPlayersTurn,
                isAnimatingLocalDraw = handDrawState.isAnimatingLocalDraw,
                deckSize = state.deckSize,
                isResolvingLocalDoom = isResolvingLocalDoom,
                localPlayerId = localPlayer.id,
                onResetSelection = { screenState.selectedPlayerCardIndex = -1 },
                onDraw = viewModel::draw
            )
            val overlayState = BoardOverlayState(
                pendingDoom = pendingDoom,
                pendingDoomMessage = pendingDoomMessage,
                pendingDoomRequiresSelection = pendingDoomRequiresSelection,
                pendingDoomRequiresInsertionUi = pendingDoomRequiresInsertionUi,
                pausedRemotePlayerName = pausedRemotePlayerName,
                pausedForDoomPlayerName = pausedForDoomPlayerName,
                pausedForDoomMessage = pausedForDoomMessage,
                pausedForDoomDetail = pausedForDoomDetail,
                cardCommandNotice = cardCommandNotice
            )
            val localPlayerAreaContent = buildLocalPlayerAreaContent(
                inputs = LocalPlayerAreaInputs(
                    player = localPlayer,
                    playerName = localPlayerName,
                    selectedPlayerCardIndex = screenState.selectedPlayerCardIndex,
                    pendingDoomRequiresSelection = pendingDoomRequiresSelection,
                    isResolvingLocalDoom = isResolvingLocalDoom,
                    isLifeLossDoomOverlay = isLifeLossDoomOverlay,
                    localAnimatingCardIndex = handDrawState.localAnimatingCardIndex,
                    localDrawStartOffset = localDrawStartOffset,
                    drawProgress = handDrawState.progress
                ),
                callbacks = LocalPlayerAreaCallbacks(
                    onHandCenterMeasured = { screenState.localHandCenter = it },
                    canActivateCard = viewModel::canActivateCard,
                    onCardSelectionToggle = { index ->
                        screenState.selectedPlayerCardIndex = if (screenState.selectedPlayerCardIndex == index) -1 else index
                    },
                    onCardActivated = { card ->
                        screenState.selectedPlayerCardIndex = -1
                        viewModel.activateCard(card)
                    }
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundCream)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(0.8f))

                    Box(
                        modifier = Modifier.weight(1.7f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CenterPlayArea(
                            deckSize = state.deckSize,
                            isLocalPlayersTurn = isLocalPlayersTurn,
                            currentTurnPlayerName = currentTurnPlayerName,
                            visibleTurns = visibleTurns,
                            playerAvatars = playerAvatars,
                            onDrawClick = handleDraw,
                            onDeckCenterMeasured = { screenState.deckCenter = it }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1.5f))
                }

                DecorativeSideBorders()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(3f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    OpponentArea(
                        opponent = topOpponent,
                        opponentName = topOpponentName,
                        drawAnimation = handDrawState.opponentAnimatingCardIndex.takeIf { it >= 0 }?.let { index ->
                            index to (opponentDrawStartOffset ?: Offset.Zero)
                        },
                        drawProgress = { handDrawState.progress },
                        onHandCenterMeasured = { screenState.opponentHandCenter = it }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(BackgroundCream)
                        .zIndex(4f)
                        .align(Alignment.TopCenter)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .zIndex(8f)
                ) {
                    ConnectionStatusBanner(status = connectionStatus)
                    androidx.compose.material3.TextButton(
                        onClick = onLeaveGame,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text("← Leave", fontSize = 12.sp, color = OutlineDark)
                    }
                }

                BoardOverlays(
                    overlayState = overlayState,
                    context = BoardOverlayContext(
                        currentPlayer = localPlayer,
                        deckSize = state.deckSize,
                        selectedPlayerCardIndex = screenState.selectedPlayerCardIndex,
                        doomSliderPosition = screenState.doomSliderPosition
                    ),
                    callbacks = BoardOverlayCallbacks(
                        onDoomSliderChange = { screenState.doomSliderPosition = it },
                        onDoomConfirmed = {
                            viewModel.insertDoom(screenState.doomSliderPosition.toInt())
                            screenState.doomSliderPosition = 0f
                            screenState.selectedPlayerCardIndex = -1
                        },
                        onDoomDismiss = { viewModel.dismissDoomNotice(screenState.selectedPlayerCardIndex) },
                        onCardCommandDismiss = viewModel::dismissCardCommandNotice,
                        onAcceptDoom = {},
                                onSnackStashVote = { _, _ -> },
                        onSnackStashResolutionDismiss = {}
                    )
                )
                if (showTargetSelectionDialog) {
                    val opponents = state.players.filter { it.id != viewModel.localPlayerId }

                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.dismissTargetSelection() },
                        title = { Text("Ziel auswählen", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("Von wem möchtest du eine Karte klauen?")
                                Spacer(modifier = Modifier.height(16.dp))
                                opponents.forEach { opponent ->
                                    androidx.compose.material3.Button(
                                        onClick = { viewModel.selectStealTarget(opponent.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(opponent.name)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { viewModel.dismissTargetSelection() }) {
                                Text("Abbrechen")
                            }
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (pendingDoomRequiresSelection) 7f else 0f)
                ) {
                    PlayerArea(content = localPlayerAreaContent)
                }
            }
        }
    }
}


@Composable
private fun BoxScope.DecorativeSideBorders() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(6.dp)
            .background(AccentOrange)
            .align(Alignment.CenterStart)
            .padding(start = 8.dp)
            .zIndex(5f)
    )
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(6.dp)
            .background(AccentOrange)
            .align(Alignment.CenterEnd)
            .padding(end = 8.dp)
            .zIndex(5f)
    )
}
