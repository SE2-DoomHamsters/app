package com.doomhamsters.ui.cheating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.doomhamsters.model.Card
import com.doomhamsters.ui.gameboard.CardFaceUp
import com.doomhamsters.ui.theme.AccentOrange
import com.doomhamsters.ui.theme.BackgroundCream
import com.doomhamsters.ui.theme.CardDarkMaroon
import com.doomhamsters.ui.theme.DoomColor

/** Shows the local Doom state while the player chooses whether to use or bluff Snack Stash. */
@Composable
fun SnackStashClaimPromptOverlay(
    pendingDoom: Card,
    message: String,
    onAcceptDoom: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .zIndex(5f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(6f)
            .padding(top = 92.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DOOM!", color = DoomColor, fontWeight = FontWeight.Black, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(16.dp))
            CardFaceUp(card = pendingDoom, isSelected = false, isDefocused = false)
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CardDarkMaroon.copy(alpha = 0.96f)
            ) {
                Text(
                    text = message,
                    color = BackgroundCream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tap Snack Stash to use it, or any other card to bluff.",
                color = AccentOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = DoomColor),
                onClick = onAcceptDoom
            ) {
                Text("Accept Doom", color = BackgroundCream, fontWeight = FontWeight.Black)
            }
        }
    }
}
