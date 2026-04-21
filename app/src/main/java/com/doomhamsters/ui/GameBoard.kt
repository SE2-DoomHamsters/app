package com.doomhamsters.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doomhamsters.viewmodel.GameBoardViewModel
import androidx.compose.material3.TextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun GameBoard(
    viewModel: GameBoardViewModel,
    onGameOver: (String) -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val log by viewModel.log.collectAsState()
    val pendingDoom by viewModel.pendingDoom.collectAsState()
    var doomPosition by remember { mutableStateOf("") }

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
        Spacer(modifier = Modifier.height(16.dp))

        if (pendingDoom != null) {
            Text("Where to insert Doom? (0 = top)")
            TextField(
                value = doomPosition,
                onValueChange = { doomPosition = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(onClick = {
                val pos = doomPosition.toIntOrNull() ?: 0
                viewModel.insertDoom(pos)
                viewModel.advanceTurn()
                doomPosition = ""
            }) {
                Text("Insert Doom")
            }
        } else {
            Button(onClick = {
                currentPlayer?.let {
                    viewModel.draw(it.id)
                    viewModel.advanceTurn()
                }
            }) {
                Text("Draw")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Log:")
        Column {
            log.takeLast(5).forEach { entry ->
                Text("• $entry")
            }
        }
    }
}