package com.doomhamsters.ui.gameboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomhamsters.model.Player
import com.doomhamsters.ui.theme.AccentOrange
import com.doomhamsters.ui.theme.BackgroundCream
import com.doomhamsters.ui.theme.CardDarkMaroon
import com.doomhamsters.ui.theme.OutlineDark

@Composable
fun TurnTracker(
    visibleTurns: List<Player>,
    playerAvatars: Map<String, String>,
    modifier: Modifier = Modifier
) {
    if (visibleTurns.isEmpty()) return
    val turnOrderKey = visibleTurns.joinToString(separator = "|") { player -> player.id }
    val density = LocalDensity.current
    val slotShiftPx = with(density) { (turnTrackerSlotHeight(visibleTurns.size) + 4.dp).roundToPx() }

    Column(
        modifier = modifier
            .width(86.dp)
            .height(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OutlineDark)
            .border(2.dp, CardDarkMaroon, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(BackgroundCream.copy(alpha = 0.95f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    AnimatedContent(
                        targetState = turnOrderKey,
                        transitionSpec = {
                            slideInVertically(
                                animationSpec = tween(durationMillis = 450),
                                initialOffsetY = { slotShiftPx }
                            ) togetherWith slideOutVertically(
                                animationSpec = tween(durationMillis = 450),
                                targetOffsetY = { -slotShiftPx }
                            ) using SizeTransform(clip = false)
                        },
                        label = "turnTrackerConveyor"
                    ) {
                        TurnTrackerContent(
                            visibleTurns = visibleTurns,
                            playerAvatars = playerAvatars
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(turnTrackerSlotHeight(visibleTurns.size)),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "<",
                            color = AccentOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TurnTrackerContent(
    visibleTurns: List<Player>,
    playerAvatars: Map<String, String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        visibleTurns.forEachIndexed { index, player ->
            TurnTrackerSlot(
                emoji = resolvePlayerEmoji(player, playerAvatars),
                isCurrentTurn = index == 0,
                slotHeight = turnTrackerSlotHeight(visibleTurns.size)
            )
        }
    }
}

@Composable
private fun TurnTrackerSlot(
    emoji: String,
    isCurrentTurn: Boolean,
    slotHeight: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(slotHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCurrentTurn) AccentOrange else BackgroundCream)
                .border(
                    width = 2.dp,
                    color = if (isCurrentTurn) BackgroundCream else CardDarkMaroon,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 18.sp,
                color = if (isCurrentTurn) CardDarkMaroon else Color.Unspecified
            )
        }
    }
}

private fun resolvePlayerEmoji(player: Player, playerAvatars: Map<String, String>): String {
    val fallbackInitial = player.name.firstOrNull()?.uppercase() ?: "?"
    return player.avatar.ifBlank {
        playerAvatars[player.id]
            ?: playerAvatars[player.name]
            ?: fallbackInitial
    }
}

private fun turnTrackerSlotHeight(slotCount: Int) = when {
    slotCount <= 2 -> 40.dp
    slotCount == 3 -> 34.dp
    slotCount == 4 -> 30.dp
    slotCount == 5 -> 27.dp
    else -> 24.dp
}
