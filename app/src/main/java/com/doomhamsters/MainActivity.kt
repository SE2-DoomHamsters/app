package com.doomhamsters

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.doomhamsters.ui.theme.DoomHamstersTheme
import androidx.lifecycle.viewmodel.compose.viewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoomHamstersTheme {
                // 1. Initialisiere ViewModel
                val lobbyViewModel: LobbyViewModel = viewModel()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 3. Lobby Steuerung
                    Box(modifier = Modifier.padding(innerPadding)) {

                        // Navigation
                        MainLobbyNavigation(lobbyViewModel)

                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding)
                        )

                    }
                }
            }
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        DoomHamstersTheme {
            Greeting("Android")
        }
    }
}