package com.doomhamsters.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doomhamsters.ui.theme.AccentOrange
import com.doomhamsters.ui.theme.BackgroundCream
import com.doomhamsters.ui.theme.OutlineDark
@Composable
fun GameBoardStatus(message: String, onLeaveGame: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = AccentOrange)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = OutlineDark
            )
            androidx.compose.material3.OutlinedButton(onClick = onLeaveGame) {
                Text("New Game")
            }
        }
    }
}