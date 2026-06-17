package com.doomhamsters.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.Player
import com.doomhamsters.ui.gameboard.FannedHand
import com.doomhamsters.ui.gameboard.FannedHandAnimationConfig
import com.doomhamsters.ui.gameboard.FannedHandCallbacks
import com.doomhamsters.ui.gameboard.FannedHandUiConfig
import com.doomhamsters.ui.gameboard.PlayerLabel

@Composable
fun OpponentArea(
    opponent: Player?,
    opponentName: String?,
    drawAnimation: Pair<Int, Offset>?,
    drawProgress: () -> Float,
    onHandCenterMeasured: (Offset) -> Unit
) {
    if (opponent == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val opponentDummyHand = List(opponent.visibleHandSize()) { Card(CardType.Normal) }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -300.dp)
                .graphicsLayer {
                    rotationZ = 180f
                }
        ) {
            FannedHand(
                cards = opponentDummyHand,
                uiConfig = FannedHandUiConfig(
                    isOpponent = true,
                    selectedIndex = -1,
                    onCenterMeasured = onHandCenterMeasured
                ),
                animationConfig = FannedHandAnimationConfig(
                    drawAnimation = drawAnimation,
                    drawProgress = drawProgress
                )
            )
        }
        PlayerLabel(
            name = opponentName ?: opponent.name,
            lives = opponent.lives,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .zIndex(1f)
        )
    }
}

@Composable
fun rememberTopOpponent(
    players: List<Player>,
    localPlayerId: String,
    currentTurnPlayerId: String
): Player? {
    val fallbackOpponent = players.firstOrNull { it.id != localPlayerId }
    val activeOpponent = players.firstOrNull { it.id == currentTurnPlayerId && it.id != localPlayerId }
    var lastActiveOpponentId by remember(localPlayerId) { mutableStateOf<String?>(null) }

    LaunchedEffect(activeOpponent?.id, fallbackOpponent?.id, players.size) {
        lastActiveOpponentId = when {
            activeOpponent != null -> activeOpponent.id
            lastActiveOpponentId == null -> fallbackOpponent?.id
            players.none { it.id == lastActiveOpponentId && it.id != localPlayerId } -> fallbackOpponent?.id
            else -> lastActiveOpponentId
        }
    }

    return players.firstOrNull { it.id == lastActiveOpponentId && it.id != localPlayerId }
        ?: fallbackOpponent
}