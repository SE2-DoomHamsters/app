package com.doomhamsters.ui.lobby

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomhamsters.BackendConfig
import com.doomhamsters.GameRepository
import com.doomhamsters.LobbyViewModel
import com.doomhamsters.R
import com.doomhamsters.data.GameSession
import com.doomhamsters.data.decodeBase64ToBitmap
import com.doomhamsters.ui.gameboard.NavGraph
import com.doomhamsters.ui.theme.DarkBrown
import com.doomhamsters.ui.theme.DoomHamstersTheme
import com.doomhamsters.ui.theme.Orange
import com.doomhamsters.ui.theme.SoftWhite
import com.doomhamsters.ui.theme.WarmAlmond
import com.doomhamsters.viewmodel.GameBoardViewModel
import com.doomhamsters.viewmodel.GameBoardViewModelFactory
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

private const val ENABLE_MANUAL_LOBBY_JOIN = true

private data class ProfileSetupUiState(
    val isProfileActionInProgress: Boolean,
    val canCreateGroup: Boolean,
    val canScanLobby: Boolean,
    val canJoinManualLobby: Boolean,
    val errorMessage: String?,
    val manualLobbyId: String
)

/** Routes between the lobby flow screens and an active game session. */
@Composable
fun MainLobbyNavigation(viewModel: LobbyViewModel) {
    val activeGameSession by viewModel.activeGameSession.collectAsState()
    val lobbyState by viewModel.lobby.collectAsState()
    val errorState by viewModel.error.collectAsState()
    val isLoadingState by viewModel.isLoading.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val playerAvatars = lobbyState?.members
        ?.flatMap { member -> listOf(member.id to member.avatar, member.username to member.avatar) }
        ?.toMap()
        .orEmpty()
    val playerNames = lobbyState?.members
        ?.associate { member -> member.id to member.username }
        .orEmpty()

    // Global Error Dialog
    errorState?.let { message ->
        ErrorDialog(
            message = message,
            onDismiss = { viewModel.clearError() },
            onRetry = {
                viewModel.retryLastAction()
            }
        )
    }

    LaunchedEffect(infoMessage) {
        infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (viewModel.currentStep) {
                1 -> StartScreen(viewModel = viewModel)
                2 -> ProfileSetupScreen(viewModel)
                3 -> ActiveLobbyScreen(viewModel)
                4 -> GameSessionContent(
                    session = activeGameSession,
                    playerAvatars = playerAvatars,
                    playerNames = playerNames,
                    onReturnToLobby = viewModel::returnToLobbyAfterGame
                )

                5 -> RulesScreen(onBackClick = { viewModel.currentStep = 1 })
            }
        }
    }
}

@Composable
private fun GameSessionContent(
    session: GameSession?,
    playerAvatars: Map<String, String>,
    playerNames: Map<String, String>,
    onReturnToLobby: () -> Unit
) {
    session?.let {
        GameBoardScreen(
            gameId = it.gameId,
            playerId = it.playerId,
            playerName = it.playerName,
            playerAvatars = playerAvatars,
            playerNames = playerNames,
            onReturnToLobby = onReturnToLobby
        )
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Collects the local player's name, avatar, and lobby action choice. */
@Composable
fun ProfileSetupScreen(viewModel: LobbyViewModel) {
    var manualLobbyId by rememberSaveable { mutableStateOf("") }
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            result.contents?.let(viewModel::joinLobby)
        }
    )
    val errorState by viewModel.error.collectAsState()
    val isLoadingState by viewModel.isLoading.collectAsState()
    val uiState = ProfileSetupUiState(
        isProfileActionInProgress = viewModel.isProfileActionInProgress,
        canCreateGroup = viewModel.username.isNotBlank() &&
            viewModel.groupName.isNotBlank() &&
            !viewModel.isProfileActionInProgress,
        canScanLobby = viewModel.username.isNotBlank() && !viewModel.isProfileActionInProgress,
        canJoinManualLobby = viewModel.username.isNotBlank() &&
            manualLobbyId.isNotBlank() &&
            !viewModel.isProfileActionInProgress,
        errorMessage = errorState,
        manualLobbyId = manualLobbyId
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)
    ) {
        Text("Lobby erstellen oder beitreten", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(20.dp))

        ProfileDetailsFields(
            username = viewModel.username,
            onUsernameChange = { viewModel.username = it },
            groupName = viewModel.groupName,
            onGroupNameChange = { viewModel.groupName = it }
        )

        Spacer(Modifier.height(20.dp))

        AvatarPicker(
            selectedAvatar = viewModel.selectedAvatar,
            onAvatarSelected = { viewModel.selectedAvatar = it }
        )

        Spacer(Modifier.height(40.dp))

        LobbyJoinActions(
            uiState = uiState,
            isLoading = isLoadingState,
            onManualLobbyIdChange = { manualLobbyId = it },
            onCreateGroup = viewModel::createGroup,
            onScanLobby = {
                val options = ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Scanne den QR-Code der Lobby")
                    setBeepEnabled(false)
                }
                scanLauncher.launch(options)
            },
            onJoinManualLobby = { viewModel.joinLobby(manualLobbyId.trim()) }
        )
    }
}

@Composable
private fun ProfileDetailsFields(
    username: String,
    onUsernameChange: (String) -> Unit,
    groupName: String,
    onGroupNameChange: (String) -> Unit
) {
    val isUsernameError = username.isEmpty()
    val isGroupNameError = groupName.isEmpty()

    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text("Dein Spielername") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isUsernameError,
        supportingText = {
            if (isUsernameError) {
                Text("Feld darf nicht leer sein", color = MaterialTheme.colorScheme.error)
            }
        }
    )

    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = groupName,
        onValueChange = onGroupNameChange,
        label = { Text("Name der Gruppe / Lobby") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isGroupNameError,
        supportingText = {
            if (isGroupNameError) {
                Text(
                    "Feld darf nicht leer sein",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvatarPicker(
    selectedAvatar: String,
    onAvatarSelected: (String) -> Unit
) {
    Text("Wähle dein Spieler-Icon:")
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        maxItemsInEachRow = 4
    ) {
        val icons = listOf("🐱", "🐶", "🐷", "🦊", "🤖", "👽", "🐭")
        icons.forEach { emoji ->
            val isSelected = selectedAvatar == emoji
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(60.dp)
                    .border(
                        3.dp,
                        if (isSelected) Color(0xFF6200EE) else Color.Transparent,
                        CircleShape
                    )
                    .clickable { onAvatarSelected(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 36.sp)
            }
        }
    }
}

@Composable
private fun LobbyJoinActions(
    uiState: ProfileSetupUiState,
    isLoading: Boolean,
    onManualLobbyIdChange: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onScanLobby: () -> Unit,
    onJoinManualLobby: () -> Unit
) {
    Button(
        onClick = onCreateGroup,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = uiState.canCreateGroup
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Gruppe erstellen")
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("ODER")
    Spacer(Modifier.height(16.dp))

    OutlinedButton(
        onClick = onScanLobby,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = uiState.canScanLobby
    ) {
        Text("QR-Code scannen & Beitreten")
    }

    if (!ENABLE_MANUAL_LOBBY_JOIN) return

    Spacer(Modifier.height(16.dp))
    Text("Testhilfe ohne Kamera")
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = uiState.manualLobbyId,
        onValueChange = onManualLobbyIdChange,
        label = { Text("Lobby ID manuell eingeben") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onJoinManualLobby,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = uiState.canJoinManualLobby
    ) {
        Text(if (uiState.isProfileActionInProgress) "Bitte warten..." else "Mit Lobby ID beitreten")
    }
}


/** Shows the active lobby membership, QR code, and start controls. */
@Composable
fun ActiveLobbyScreen(viewModel: LobbyViewModel) {
    val lobbyState by viewModel.lobby.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val startAvailabilityMessage = viewModel.startAvailabilityMessage()
    val canStartGame = viewModel.canCurrentUserStartGame()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Lobby ID: ${lobbyState?.lobbyId}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        lobbyState?.qrCodeBase64?.let { base64 ->
            decodeBase64ToBitmap(base64)?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(200.dp)
                )
            }
        } ?: Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("QR-Code wird geladen...", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        val members = lobbyState?.members.orEmpty()
        Text("Spieler in der Lobby: ${members.size}")

        if (members.size < 2) {
            val infiniteTransition = rememberInfiniteTransition(label = "waiting")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Warte auf Spieler...",
                modifier = Modifier.alpha(alpha),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        } else {
            members.forEach { member ->
                Text("${member.avatar} ${member.username}")
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        startAvailabilityMessage?.let { message ->
            Text(
                text = message,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = { viewModel.startGame() },
            modifier = Modifier.fillMaxWidth(),
            enabled = canStartGame
        ) {
            if (viewModel.isStartingGame) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Spiel für alle starten")
            }
        }

        Spacer (Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.leaveLobby() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.leave_lobby_text))
        }
    }
}

/** Creates the game board view model for an active session and shows the board flow. */
@Composable
fun GameBoardScreen(
    gameId: String,
    playerId: String,
    playerName: String,
    playerAvatars: Map<String, String> = emptyMap(),
    playerNames: Map<String, String> = emptyMap(),
    onReturnToLobby: () -> Unit
) {
    val gameBoardViewModel: GameBoardViewModel = viewModel(
        key = "game-$gameId-player-$playerId",
        factory = GameBoardViewModelFactory(
            gameId = gameId,
            playerId = playerId,
            playerName = playerName,
            repository = GameRepository(BackendConfig.BASE_URL)
        )
    )

    NavGraph(
        viewModel = gameBoardViewModel,
        playerAvatars = playerAvatars,
        playerNames = playerNames,
        onReturnToLobby = onReturnToLobby
    )
}

/** Displays the app welcome screen and entry actions. */
@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    viewModel: LobbyViewModel? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmAlmond)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.welcome_message),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Orange
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel?.currentStep = 2 },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange,
                contentColor = SoftWhite
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Text(
                stringResource(R.string.start_button_text),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel?.currentStep = 5 },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange,
                contentColor = SoftWhite
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.rules_button_text),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
        }
    }
}

/** Displays the game rules screen. */
@Composable
fun RulesScreen(modifier: Modifier = Modifier, onBackClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmAlmond)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.rules_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Orange
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.rules_content),
                    fontSize = 18.sp,
                    color = DarkBrown
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.rules_back),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
        }
    }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Oops!", color = MaterialTheme.colorScheme.error)
            }
        },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onRetry) {
                Text("Nochmal versuchen")
            }
        }
    )
}

/** Previews the start screen in Android Studio. */
@Preview(showBackground = true)
@Composable
fun StartScreenPreview() {
    DoomHamstersTheme {
        StartScreen()
    }
}

/** Previews the rules screen in Android Studio. */
@Preview(showBackground = true)
@Composable
fun RulesPreview() {
    DoomHamstersTheme {
        RulesScreen(onBackClick = {})
    }
}
