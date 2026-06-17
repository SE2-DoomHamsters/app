package com.doomhamsters.ui.gameboard
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Card
import com.doomhamsters.cards.CardCommandNotice
import com.doomhamsters.viewmodel.ConnectionStatus

data class GameBoardUiState(
    val gameState: GameState? = null,
    val isLocalPlayersTurn: Boolean = false,
    val pendingDoom: Card? = null,
    val pendingDoomMessage: String? = null,
    val pendingDoomRequiresSelection: Boolean = false,
    val pendingDoomRequiresInsertionUi: Boolean = false,
    val pausedForDoomPlayerName: String? = null,
    val pausedForDoomMessage: String? = null,
    val pausedForDoomDetail: String? = null,
    val cardCommandNotice: CardCommandNotice? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Connected
)