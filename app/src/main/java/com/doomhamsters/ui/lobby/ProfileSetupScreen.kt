package com.doomhamsters.ui.lobby

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomhamsters.LobbyViewModel
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