package com.doomhamsters.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.doomhamsters.viewmodel.GameBoardViewModel

@Composable
fun GameBoard(
    viewModel: GameBoardViewModel,
    onGameOver: (String) -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.gameOver.collect { winnerId -> onGameOver(winnerId) }
    }

    val currentPlayer = gameState?.players?.getOrNull(gameState?.currentPlayerIndex ?: 0)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Current Player: ${currentPlayer?.id ?: "-"}")
        Text("Lives: ${currentPlayer?.lives ?: "-"}")
        Spacer(modifier = Modifier.height(16.dp))
        PlayerListView(
            players = gameState?.players ?: emptyList(),
            currentPlayerIndex = gameState?.currentPlayerIndex ?: 0
        )
        Button(onClick = {
            currentPlayer?.let {
                viewModel.draw(it.id)
                viewModel.advanceTurn()
            }
        }) {
            Text("Draw")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameBoardPreview() {
    val fakeViewModel = GameBoardViewModel().apply {
        startGame(arrayListOf("Fat", "Zombie", "Sleepy"))
    }
    GameBoard(viewModel = fakeViewModel, onGameOver = {})
}