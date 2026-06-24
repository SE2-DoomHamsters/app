package com.doomhamsters.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.doomhamsters.model.Card
import com.doomhamsters.model.Player
import com.doomhamsters.ui.theme.CardDarkMaroon
import com.doomhamsters.ui.gameboard.FannedHand
import com.doomhamsters.ui.gameboard.FannedHandAnimationConfig
import com.doomhamsters.ui.gameboard.FannedHandCallbacks
import com.doomhamsters.ui.gameboard.FannedHandUiConfig
import com.doomhamsters.ui.gameboard.PlayerLabel


data class PlayerAreaContent(
    val player: Player,
    val playerName: String,
    val handUiConfig: FannedHandUiConfig,
    val handAnimationConfig: FannedHandAnimationConfig,
    val handCallbacks: FannedHandCallbacks
)

data class LocalPlayerAreaInputs(
    val player: Player,
    val playerName: String,
    val selectedPlayerCardIndex: Int,
    val pendingDoomRequiresSelection: Boolean,
    val isResolvingLocalDoom: Boolean,
    val isLifeLossDoomOverlay: Boolean,
    val isSnackStashClaimPending: Boolean,
    val localAnimatingCardIndex: Int,
    val localDrawStartOffset: Offset?,
    val drawProgress: Float
)

data class LocalPlayerAreaCallbacks(
    val onHandCenterMeasured: (Offset) -> Unit,
    val canActivateCard: (Card) -> Boolean,
    val onCardSelectionToggle: (Int) -> Unit,
    val onCardActivated: (Card) -> Unit
)

fun buildLocalPlayerAreaContent(
    inputs: LocalPlayerAreaInputs,
    callbacks: LocalPlayerAreaCallbacks
): PlayerAreaContent {
    return PlayerAreaContent(
        player = inputs.player,
        playerName = inputs.playerName,
        handUiConfig = FannedHandUiConfig(
            isOpponent = false,
            selectedIndex = inputs.selectedPlayerCardIndex,
            disableDefocus = inputs.pendingDoomRequiresSelection,
            suppressTooltip = inputs.isResolvingLocalDoom,
            interactionEnabled = !inputs.isLifeLossDoomOverlay && !inputs.isSnackStashClaimPending,
            onCenterMeasured = callbacks.onHandCenterMeasured
        ),
        handAnimationConfig = FannedHandAnimationConfig(
            drawAnimation = inputs.localAnimatingCardIndex.takeIf { it >= 0 }?.let { index ->
                index to (inputs.localDrawStartOffset ?: Offset.Zero)
            },
            drawProgress = { inputs.drawProgress }
        ),
        handCallbacks = FannedHandCallbacks(
            canActivateCard = callbacks.canActivateCard,
            onCardSelected = callbacks.onCardSelectionToggle,
            onCardActivated = callbacks.onCardActivated
        )
    )
}

@Composable
fun PlayerArea(content: PlayerAreaContent) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp).offset(y = 50.dp)) {
            drawOval(
                color = CardDarkMaroon,
                topLeft = Offset(-size.width * 0.5f, 0f),
                size = Size(size.width * 2f, size.height * 2f)
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxWidth().offset(y = 58.dp)
        ) {
            FannedHand(
                cards = content.player.hand,
                uiConfig = content.handUiConfig,
                animationConfig = content.handAnimationConfig,
                callbacks = content.handCallbacks
            )
        }

        PlayerLabel(
            name = content.playerName,
            lives = content.player.lives,
            modifier = Modifier.padding(bottom = 8.dp).zIndex(200f)
        )
    }
}