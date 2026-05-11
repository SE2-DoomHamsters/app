package com.doomhamsters.ui.gameboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.doomhamsters.model.Player

@Composable
fun PlayerListView(players: List<Player>, currentPlayerIndex: Int) {
    Column {
        players.forEachIndexed { index, player ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .alpha(if (player.isAlive()) 1f else 0.4f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (index == currentPlayerIndex) "▶ ${player.id}" else player.id)
                Text("Lives: ${player.lives}")
                Text("Cards: ${player.hand.size}")
            }
        }
    }
}