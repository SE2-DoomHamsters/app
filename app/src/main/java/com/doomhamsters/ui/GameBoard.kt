package com.doomhamsters.ui


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import com.doomhamsters.ui.gameboard.*
import com.doomhamsters.ui.theme.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player
import com.doomhamsters.viewmodel.GameBoardViewModel
import kotlinx.coroutines.launch

private sealed interface GameBoardResolution {
    data class Ready(
        val state: GameState,
        val currentTurnPlayer: Player,
        val localPlayer: Player
    ) : GameBoardResolution

    data class Status(val message: String) : GameBoardResolution
}


private data class HandDrawState(
    val localAnimatingCardIndex: Int,
    val opponentAnimatingCardIndex: Int,
    val isAnimatingLocalDraw: Boolean,
    val progress: Float
)

/** Renders the live game board for the local player. */
@Composable
fun GameBoard(
    viewModel: GameBoardViewModel,
    playerAvatars: Map<String, String> = emptyMap(),
    playerNames: Map<String, String> = emptyMap(),
    onGameOver: (String) -> Unit,
    onLeaveGame: () -> Unit = {}
) {
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
   //val gameState by viewModel.gameState.collectAsState()
    //val isLocalPlayersTurn by viewModel.isLocalPlayersTurn.collectAsState()
    //val pendingDoom by viewModel.pendingDoom.collectAsState()
    //val pendingDoomMessage by viewModel.pendingDoomMessage.collectAsState()
    //val pendingDoomRequiresSelection by viewModel.pendingDoomRequiresSelection.collectAsState()
    //val pendingDoomRequiresInsertionUi by viewModel.pendingDoomRequiresInsertionUi.collectAsState()
    //val pausedForDoomPlayerName by viewModel.pausedForDoomPlayerName.collectAsState()
    //val pausedForDoomMessage by viewModel.pausedForDoomMessage.collectAsState()
    //val pausedForDoomDetail by viewModel.pausedForDoomDetail.collectAsState()
    //val cardCommandNotice by viewModel.cardCommandNotice.collectAsState()
    var latestError by remember { mutableStateOf<String?>(null) }
    //val connectionStatus by viewModel.connectionStatus.collectAsState()

    var selectedPlayerCardIndex by remember { mutableIntStateOf(-1) }
    var doomSliderPosition by remember { mutableFloatStateOf(0f) }
    var deckCenter by remember { mutableStateOf<Offset?>(null) }
    var localHandCenter by remember { mutableStateOf<Offset?>(null) }
    var opponentHandCenter by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(viewModel) {
        launch {
            viewModel.error.collect { latestError = it }
        }
        launch {
            viewModel.gameOver.collect(onGameOver)
        }
    }

    when (
        val resolution = resolveGameBoardResolution(
            gameState = gameState,
            latestError = latestError,
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
                onResetSelection = { selectedPlayerCardIndex = -1 },
                onResetSlider = { doomSliderPosition = 0f }
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
            val localDrawStartOffset = remember(deckCenter, localHandCenter) {
                offsetBetween(deckCenter, localHandCenter)
            }
            val opponentDrawStartOffset = remember(deckCenter, opponentHandCenter) {
                offsetBetween(opponentHandCenter, deckCenter)
            }
            val handleDraw = createDrawHandler(
                isLocalPlayersTurn = isLocalPlayersTurn,
                isAnimatingLocalDraw = handDrawState.isAnimatingLocalDraw,
                deckSize = state.deckSize,
                isResolvingLocalDoom = isResolvingLocalDoom,
                localPlayerId = localPlayer.id,
                onResetSelection = { selectedPlayerCardIndex = -1 },
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
                    selectedPlayerCardIndex = selectedPlayerCardIndex,
                    pendingDoomRequiresSelection = pendingDoomRequiresSelection,
                    isResolvingLocalDoom = isResolvingLocalDoom,
                    isLifeLossDoomOverlay = isLifeLossDoomOverlay,
                    localAnimatingCardIndex = handDrawState.localAnimatingCardIndex,
                    localDrawStartOffset = localDrawStartOffset,
                    drawProgress = handDrawState.progress
                ),
                callbacks = LocalPlayerAreaCallbacks(
                    onHandCenterMeasured = { localHandCenter = it },
                    canActivateCard = viewModel::canActivateCard,
                    onCardSelectionToggle = { index ->
                        selectedPlayerCardIndex = if (selectedPlayerCardIndex == index) -1 else index
                    },
                    onCardActivated = { card ->
                        selectedPlayerCardIndex = -1
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
                            onDeckCenterMeasured = { deckCenter = it }
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
                        onHandCenterMeasured = { opponentHandCenter = it }
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
                        selectedPlayerCardIndex = selectedPlayerCardIndex,
                        doomSliderPosition = doomSliderPosition
                    ),
                    callbacks = BoardOverlayCallbacks(
                        onDoomSliderChange = { doomSliderPosition = it },
                        onDoomConfirmed = {
                            viewModel.insertDoom(doomSliderPosition.toInt())
                            doomSliderPosition = 0f
                            selectedPlayerCardIndex = -1
                        },
                        onDoomDismiss = { viewModel.dismissDoomNotice(selectedPlayerCardIndex) },
                        onCardCommandDismiss = viewModel::dismissCardCommandNotice,
                        onAcceptDoom = {},
                                onSnackStashVote = { _, _ -> },
                        onSnackStashResolutionDismiss = {}
                    )
                )

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

private fun resolveGameBoardResolution(
    gameState: GameState?,
    latestError: String?,
    localPlayerId: String
): GameBoardResolution {
    if (gameState == null) {
        return GameBoardResolution.Status(latestError ?: "Waiting for game state from server...")
    }

    val currentTurnPlayer = gameState.players.firstOrNull { it.id == gameState.currentTurnPlayerId }
        ?: gameState.players.getOrNull(gameState.currentPlayerIndex)
        ?: return GameBoardResolution.Status("Game state is invalid: current player is missing.")

    val localPlayer = gameState.players.firstOrNull { it.id == localPlayerId }
        ?: return GameBoardResolution.Status(
            latestError ?: "Joined game, but player '$localPlayerId' is missing from game state."
        )

    return GameBoardResolution.Ready(
        state = gameState,
        currentTurnPlayer = currentTurnPlayer,
        localPlayer = localPlayer
    )
}

private fun offsetBetween(from: Offset?, to: Offset?): Offset? {
    return if (from != null && to != null) from - to else null
}

private fun createDrawHandler(
    isLocalPlayersTurn: Boolean,
    isAnimatingLocalDraw: Boolean,
    deckSize: Int,
    isResolvingLocalDoom: Boolean,
    localPlayerId: String,
    onResetSelection: () -> Unit,
    onDraw: (String) -> Unit
): () -> Unit {
    return {
        if (isLocalPlayersTurn && !isAnimatingLocalDraw && deckSize > 0 && !isResolvingLocalDoom) {
            onResetSelection()
            onDraw(localPlayerId)
        }
    }
}

@Composable
private fun ResetDoomInteractionState(
    isResolvingLocalDoom: Boolean,
    pendingDoomRequiresSelection: Boolean,
    onResetSelection: () -> Unit,
    onResetSlider: () -> Unit
) {
    LaunchedEffect(isResolvingLocalDoom) {
        if (isResolvingLocalDoom) {
            onResetSelection()
            onResetSlider()
        }
    }

    LaunchedEffect(pendingDoomRequiresSelection) {
        if (!pendingDoomRequiresSelection) {
            onResetSelection()
        }
    }
}

private fun resolvePausedRemotePlayerName(
    rawName: String?,
    players: List<Player>,
    playerNames: Map<String, String>
): String? {
    if (rawName == null) return null

    val matchedPlayer = players.firstOrNull { player ->
        val displayName = resolvePlayerDisplayName(
            playerId = player.id,
            fallbackName = player.name,
            playerNames = playerNames
        )
        displayName == rawName || player.name == rawName || player.id == rawName
    }

    return matchedPlayer?.let { player ->
        resolvePlayerDisplayName(
            playerId = player.id,
            fallbackName = player.name,
            playerNames = playerNames
        )
    } ?: rawName
}

@Composable
private fun rememberHandDrawState(
    localPlayerId: String,
    localHandSize: Int,
    opponentId: String?,
    opponentVisibleHandSize: Int
): HandDrawState {
    val drawAnimatable = remember { Animatable(0f) }
    var localAnimatingCardIndex by remember(localPlayerId) { mutableIntStateOf(-1) }
    var opponentAnimatingCardIndex by remember(opponentId) { mutableIntStateOf(-1) }
    var isAnimatingLocalDraw by remember(localPlayerId) { mutableStateOf(false) }
    var previousLocalHandSize by remember(localPlayerId) { mutableIntStateOf(localHandSize) }
    var previousOpponentHandSize by remember(opponentId) { mutableIntStateOf(opponentVisibleHandSize) }
    var hasInitializedHandTracking by remember(localPlayerId, opponentId) { mutableStateOf(false) }

    LaunchedEffect(localHandSize, opponentVisibleHandSize, opponentId) {
        if (!hasInitializedHandTracking) {
            previousLocalHandSize = localHandSize
            previousOpponentHandSize = opponentVisibleHandSize
            hasInitializedHandTracking = true
            return@LaunchedEffect
        }

        if (localHandSize > previousLocalHandSize && !isAnimatingLocalDraw) {
            localAnimatingCardIndex = localHandSize - 1
            isAnimatingLocalDraw = true
            runDrawAnimation(drawAnimatable)
            isAnimatingLocalDraw = false
            localAnimatingCardIndex = -1
        }

        if (opponentVisibleHandSize > previousOpponentHandSize) {
            opponentAnimatingCardIndex = opponentVisibleHandSize - 1
            runDrawAnimation(drawAnimatable)
            opponentAnimatingCardIndex = -1
        }

        previousLocalHandSize = localHandSize
        previousOpponentHandSize = opponentVisibleHandSize
    }

    return HandDrawState(
        localAnimatingCardIndex = localAnimatingCardIndex,
        opponentAnimatingCardIndex = opponentAnimatingCardIndex,
        isAnimatingLocalDraw = isAnimatingLocalDraw,
        progress = drawAnimatable.value
    )
}

private suspend fun runDrawAnimation(drawAnimatable: Animatable<Float, AnimationVector1D>) {
    drawAnimatable.snapTo(0f)
    drawAnimatable.animateTo(
        targetValue = 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing)
    )
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
