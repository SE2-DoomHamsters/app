package com.doomhamsters.ui.gameboard


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Displays the winner and a return-to-lobby action after a game ends. */
@Composable
fun GameOverScreen(
    winnerId: String,
    winnerName: String = winnerId,
    onRestart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Game Over!")
        Text("Winner: $winnerName")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRestart) {
            Text("Back to Lobby")
        }
    }
}
