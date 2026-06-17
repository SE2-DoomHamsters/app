package com.doomhamsters.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomhamsters.model.Player
import com.doomhamsters.ui.theme.OutlineDark
import com.doomhamsters.ui.gameboard.DrawDeck
import com.doomhamsters.ui.gameboard.TurnTracker

@Composable
fun CenterPlayArea(
    deckSize: Int,
    isLocalPlayersTurn: Boolean,
    currentTurnPlayerName: String,
    visibleTurns: List<Player>,
    playerAvatars: Map<String, String>,
    onDrawClick: () -> Unit,
    onDeckCenterMeasured: (Offset) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DrawDeck(
                deckSize = deckSize,
                onClick = onDrawClick,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    val center = position + Offset(
                        x = coordinates.size.width / 2f,
                        y = coordinates.size.height / 2f
                    )
                    onDeckCenterMeasured(center)
                }
            )
            TurnTracker(
                visibleTurns = visibleTurns,
                playerAvatars = playerAvatars
            )
        }
        Text(
            text = if (isLocalPlayersTurn) {
                "YOUR TURN - TAP DECK TO DRAW"
            } else {
                "WAITING FOR $currentTurnPlayerName"
            },
            color = OutlineDark,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )
    }
}

fun buildVisibleTurnOrder(
    players: List<Player>,
    currentTurnPlayerId: String?,
    currentPlayerIndex: Int,
    futureTurnCount: Int = 6
): List<Player> {
    if (players.isEmpty()) return emptyList()

    val currentIndex = players.indexOfFirst { it.id == currentTurnPlayerId }
        .takeIf { it >= 0 }
        ?: currentPlayerIndex.coerceIn(0, players.lastIndex)

    val alivePlayersInOrder = buildList {
        repeat(players.size) { offset ->
            val player = players[(currentIndex + offset) % players.size]
            if (player.isAlive()) add(player)
        }
    }

    if (alivePlayersInOrder.size <= 1) return alivePlayersInOrder

    return List(futureTurnCount) { index ->
        alivePlayersInOrder[index % alivePlayersInOrder.size]
    }
}