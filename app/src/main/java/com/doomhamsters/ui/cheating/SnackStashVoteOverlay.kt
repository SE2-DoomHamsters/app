package com.doomhamsters.ui.cheating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.doomhamsters.cheating.presentation.SnackStashVoteAction
import com.doomhamsters.cheating.presentation.SnackStashVoteDisplay
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.ui.gameboard.CardFaceUp
import com.doomhamsters.ui.theme.AccentOrange
import com.doomhamsters.ui.theme.BackgroundCream
import com.doomhamsters.ui.theme.CardDarkMaroon
import com.doomhamsters.ui.theme.DoomColor
import com.doomhamsters.ui.theme.SnackStashColor

/** Shows a pending Snack Stash claim and the vote controls for eligible players. */
@Composable
fun SnackStashVoteOverlay(
    display: SnackStashVoteDisplay,
    onVote: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .zIndex(9f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .padding(top = 96.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SNACK STASH?", color = SnackStashColor, fontWeight = FontWeight.Black, fontSize = 26.sp)
            Spacer(modifier = Modifier.height(16.dp))
            CardFaceUp(card = Card(CardType.SnackStash), isSelected = false, isDefocused = false)
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CardDarkMaroon.copy(alpha = 0.96f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = display.message,
                        color = BackgroundCream,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = display.progressText,
                        color = AccentOrange,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SnackStashVoteActionContent(display.action, onVote)
        }
    }
}

@Composable
private fun SnackStashVoteActionContent(
    action: SnackStashVoteAction,
    onVote: (Boolean) -> Unit
) {
    when (action) {
        SnackStashVoteAction.LOCAL_CLAIM -> {
            Text(
                text = "Waiting for the other players to vote.",
                color = BackgroundCream,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        SnackStashVoteAction.ALREADY_VOTED -> {
            Text(
                text = "Vote submitted. Waiting for the table.",
                color = BackgroundCream,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        SnackStashVoteAction.CAN_VOTE -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = SnackStashColor),
                    onClick = { onVote(false) }
                ) {
                    Text("Accept", color = CardDarkMaroon, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DoomColor),
                    onClick = { onVote(true) }
                ) {
                    Text("Challenge", color = BackgroundCream, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
