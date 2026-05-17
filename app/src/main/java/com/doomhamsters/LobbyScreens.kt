package com.doomhamsters

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomhamsters.data.decodeBase64ToBitmap
import com.doomhamsters.ui.theme.DarkBrown
import com.doomhamsters.ui.theme.DoomHamstersTheme
import com.doomhamsters.ui.theme.Orange
import com.doomhamsters.ui.theme.SoftWhite
import com.doomhamsters.ui.theme.WarmAlmond
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import com.doomhamsters.viewmodel.GameBoardViewModel

@Composable
fun MainLobbyNavigation(viewModel: LobbyViewModel) {
    // Hier wird entschieden: Welchen Screen zeigen wir gerade?
    var activeGameId by remember { mutableStateOf<String?>(null) }
    var activePlayerId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.navigateToGame.collect { (gameId, playerId) ->
            activeGameId = gameId
            activePlayerId = playerId
            //viewModel.currentStep = 4
        }
    }
    when (viewModel.currentStep) {
        1 -> StartScreen(viewModel = viewModel)
        2 -> ProfileSetupScreen(viewModel)
        3 -> ActiveLobbyScreen(viewModel)
        4 -> {
            if (activeGameId != null && activePlayerId != null) {
                GameBoardScreen(gameId = activeGameId!!, playerId = activePlayerId!!)
            }
        }
        5 -> RulesScreen(onBackClick = { viewModel.currentStep = 1 })
    }
}

@Composable
fun ProfileSetupScreen(viewModel: LobbyViewModel) {
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            if (result.contents != null) {
                viewModel.joinLobby(result.contents)
            }
        }
    )
    val errorState by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Lobby erstellen", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(20.dp))

        // 1.NAME
        OutlinedTextField(
            value = viewModel.username,
            onValueChange = { viewModel.username = it },
            label = { Text("Dein Spielername") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        // 2. GRUPPENNAME
        OutlinedTextField(
            value = viewModel.groupName, // Hier wird der Name der Gruppe gespeichert
            onValueChange = { viewModel.groupName = it },
            label = { Text("Name der Gruppe / Lobby") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        // 3.EMOJIS
        Text("Wähle dein Spieler-Icon:")

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            maxItemsInEachRow = 4
        ) {
            val icons = listOf("🐱", "🐶", "🐷", "🦊", "🤖", "👽", "🐭")
            icons.forEach { emoji ->
                val isSelected = viewModel.selectedAvatar == emoji
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(60.dp)
                        .border(
                            3.dp,
                            if (isSelected) Color(0xFF6200EE) else Color.Transparent,
                            CircleShape
                        )
                        .clickable { viewModel.selectedAvatar = emoji },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 36.sp)
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // 4. BUTTON
        Button(
            onClick = { viewModel.createGroup() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            // Button ist nur klickbar, wenn Name UND Gruppenname ausgefüllt sind
            enabled = viewModel.username.isNotBlank() && viewModel.groupName.isNotBlank()
        ) {
            Text("Gruppe erstellen")
        }
        Spacer(Modifier.height(16.dp))
        Text("ODER")
        Spacer(Modifier.height(16.dp))

        if (errorState != null) {
            Text(text = errorState!!, color = Color.Red)
        }
        OutlinedButton(
            onClick = {
                val options = ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Scanne den QR-Code der Lobby")
                    setBeepEnabled(false)
                }
                scanLauncher.launch(options)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = viewModel.username.isNotBlank()
        ) {
            Text("QR-Code scannen & Beitreten")
        }
    }
}

@Composable
fun ActiveLobbyScreen(viewModel: LobbyViewModel) {
    val lobbyState by viewModel.lobby.collectAsState()

    // Die Zurücktaste am Gerät führt zum Verlassen der Lobby
    BackHandler {
        viewModel.leaveLobby()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Lobby ID: ${lobbyState?.lobbyId}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        // Hier wird der QR-Code angezeigt, der vom Backend kommt
        lobbyState?.qrCodeBase64?.let { base64 ->
            decodeBase64ToBitmap(base64)?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(200.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Spieler bereit: ${lobbyState?.members?.size ?: 0}")

        // Liste der Spieler
        lobbyState?.members?.forEach { member ->
            Text("${member.avatar} ${member.username}")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = {
            viewModel.startGame()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("SPIEL STARTEN")
        }

        Spacer(Modifier.height(8.dp))

        // leave-lobby button
        OutlinedButton(
            onClick = { viewModel.leaveLobby() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.leave_lobby_text))
        }
    }
}

@Composable
fun GameBoardScreen(gameId: String, playerId: String) {
    val viewModel = remember { GameBoardViewModel(gameId, playerId) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "GAMEBOARD",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}


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

        // start-Button
        Button(
            onClick = {
                // schaltet die Navigation auf den ProfileSetupScreen um!
                viewModel?.currentStep = 2
            },
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

        // Rules-Button
        Button(
            onClick = {
                viewModel?.currentStep = 5
            },
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
            color = Color.White.copy(alpha = 0.5f), // Dein 80% Weiß
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
