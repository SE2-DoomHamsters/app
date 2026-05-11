package com.doomhamsters.ui.gameboard

import androidx.compose.foundation.background
import com.doomhamsters.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.Player

data class DoomOverlayState(
    val pendingDoom: Card,
    val deckSize: Int,
    val currentPlayer: Player,
    val selectedPlayerCardIndex: Int,
    val hasDefused: Boolean,
    val doomSliderPosition: Float
)

@Composable
fun DoomOverlay(
    state: DoomOverlayState,
    onDefuseTriggered: () -> Unit,
    onDoomSliderChange: (Float) -> Unit,
    onDoomConfirmed: () -> Unit,
    onAcceptDoom: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .zIndex(5f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(6f)
            .padding(top = 100.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!state.hasDefused) {
                ImpendingDoomContent(
                    pendingDoom = state.pendingDoom,
                    currentPlayer = state.currentPlayer,
                    selectedPlayerCardIndex = state.selectedPlayerCardIndex,
                    onDefuseTriggered = onDefuseTriggered,
                    onAcceptDoom = onAcceptDoom
                )
            } else {
                DefusedDoomContent(
                    pendingDoom = state.pendingDoom,
                    deckSize = state.deckSize,
                    doomSliderPosition = state.doomSliderPosition,
                    onDoomSliderChange = onDoomSliderChange,
                    onDoomConfirmed = onDoomConfirmed
                )
            }
        }
    }
}

@Composable
private fun ImpendingDoomContent(
    pendingDoom: Card,
    currentPlayer: Player,
    selectedPlayerCardIndex: Int,
    onDefuseTriggered: () -> Unit,
    onAcceptDoom: () -> Unit
) {
    Text("IMPENDING DOOM!", color = DoomColor, fontWeight = FontWeight.Black, fontSize = 28.sp)
    Spacer(modifier = Modifier.height(16.dp))
    CardFaceUp(card = pendingDoom, isSelected = false, isDefocused = false)

    Spacer(modifier = Modifier.height(24.dp))

    val hasSnackStash = currentPlayer.hand.any { it.type == CardType.SnackStash }
    if (hasSnackStash) {
        val selectedCard = currentPlayer.hand.getOrNull(selectedPlayerCardIndex)
        if (selectedCard != null && selectedCard.type == CardType.SnackStash) {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = SnackStashColor),
                onClick = onDefuseTriggered
            ) {
                Text("Play Snack Stash", color = CardDarkMaroon, fontWeight = FontWeight.Black)
            }
        } else {
            Text(
                text = "Select a Snack Stash from your hand!",
                color = SnackStashColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    } else {
        Button(
            colors = ButtonDefaults.buttonColors(containerColor = DoomColor),
            onClick = onAcceptDoom
        ) {
            Text("Accept Your Doom (Lose 1 Life)", color = BackgroundCream, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DefusedDoomContent(
    pendingDoom: Card,
    deckSize: Int,
    doomSliderPosition: Float,
    onDoomSliderChange: (Float) -> Unit,
    onDoomConfirmed: () -> Unit
) {
    Text("DEFUSED!", color = SnackStashColor, fontWeight = FontWeight.Black, fontSize = 28.sp)
    Spacer(modifier = Modifier.height(16.dp))
    CardFaceUp(card = pendingDoom, isSelected = false, isDefocused = false)

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Place back in deck (Position: ${doomSliderPosition.toInt()})",
        color = BackgroundCream,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        val maxDeckSize = maxOf(1f, deckSize.toFloat())
        Slider(
            value = doomSliderPosition.coerceIn(0f, maxDeckSize),
            onValueChange = onDoomSliderChange,
            valueRange = 0f..maxDeckSize,
            steps = maxOf(0, deckSize - 1),
            modifier = Modifier.width(160.dp),
            colors = SliderDefaults.colors(
                thumbColor = AccentOrange,
                activeTrackColor = BackgroundCream,
                inactiveTrackColor = OutlineDark
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            onClick = onDoomConfirmed,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Confirm", color = CardDarkMaroon, fontWeight = FontWeight.Black)
        }
    }
}