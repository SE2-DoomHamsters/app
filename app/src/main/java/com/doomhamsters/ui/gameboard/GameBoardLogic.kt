package com.doomhamsters.ui.gameboard
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player
@Stable
class GameBoardState {
    var latestError by mutableStateOf<String?>(null)
    var selectedPlayerCardIndex by mutableIntStateOf(-1)
    var doomSliderPosition by mutableFloatStateOf(0f)
    var deckCenter by mutableStateOf<Offset?>(null)
    var localHandCenter by mutableStateOf<Offset?>(null)
    var opponentHandCenter by mutableStateOf<Offset?>(null)
}

@Composable
fun rememberGameBoardState(): GameBoardState {
    return remember { GameBoardState() }
}

sealed interface GameBoardResolution {
    data class Ready(
        val state: GameState,
        val currentTurnPlayer: Player,
        val localPlayer: Player
    ) : GameBoardResolution
    data class Status(val message: String) : GameBoardResolution
}

data class HandDrawState(
    val localAnimatingCardIndex: Int,
    val opponentAnimatingCardIndex: Int,
    val isAnimatingLocalDraw: Boolean,
    val progress: Float
)

fun resolveGameBoardResolution(
    gameState: GameState?,
    latestError: String?,
    localPlayerId: String
): GameBoardResolution {
    if (gameState == null) return GameBoardResolution.Status(latestError ?: "Waiting for game state from server...")

    val currentTurnPlayer = gameState.players.firstOrNull { it.id == gameState.currentTurnPlayerId }
        ?: gameState.players.getOrNull(gameState.currentPlayerIndex)
        ?: return GameBoardResolution.Status("Game state is invalid: current player is missing.")

    val localPlayer = gameState.players.firstOrNull { it.id == localPlayerId }
        ?: return GameBoardResolution.Status(latestError ?: "Joined game, but player '$localPlayerId' is missing.")

    return GameBoardResolution.Ready(state = gameState, currentTurnPlayer = currentTurnPlayer, localPlayer = localPlayer)
}

fun offsetBetween(from: Offset?, to: Offset?): Offset? {
    return if (from != null && to != null) from - to else null
}

fun createDrawHandler(
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
fun ResetDoomInteractionState(
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

fun resolvePausedRemotePlayerName(
    rawName: String?,
    players: List<Player>,
    playerNames: Map<String, String>
): String? {
    if (rawName == null) return null
    val matchedPlayer = players.firstOrNull { player ->
        val displayName = resolvePlayerDisplayName(player.id, player.name, playerNames)
        displayName == rawName || player.name == rawName || player.id == rawName
    }
    return matchedPlayer?.let { player -> resolvePlayerDisplayName(player.id, player.name, playerNames) } ?: rawName
}

@Composable
fun rememberHandDrawState(
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
    return HandDrawState(localAnimatingCardIndex, opponentAnimatingCardIndex, isAnimatingLocalDraw, drawAnimatable.value)
}

suspend fun runDrawAnimation(drawAnimatable: Animatable<Float, AnimationVector1D>) {
    drawAnimatable.snapTo(0f)
    drawAnimatable.animateTo(targetValue = 1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
}
