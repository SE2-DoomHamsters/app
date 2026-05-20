package com.doomhamsters


import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.doomhamsters.ui.lobby.MainLobbyNavigation
import com.doomhamsters.ui.theme.DoomHamstersTheme
import androidx.lifecycle.viewmodel.compose.viewModel

//log is for debug reasons-> can be removed if later its proven to be annoying.
/** Hosts the Compose-based Doom Hamsters application. */
class MainActivity : ComponentActivity() {
    private companion object {
        const val TAG = "LobbyDebug"
    }

    /** Initializes the root Compose content for the app. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity.onCreate savedState=${savedInstanceState != null}")
        enableEdgeToEdge()
        setContent {
            DoomHamstersTheme {
                // 1. Initialisiere ViewModel
                val lobbyViewModel: LobbyViewModel = viewModel()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    // 3. Lobby Steuerung
                    Box(modifier = Modifier
                        .padding(innerPadding)) {

                        // Navigation
                        MainLobbyNavigation(lobbyViewModel)


                    }
                }
            }
        }
    }

    /** Logs that the activity moved into the started state. */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "MainActivity.onStart")
    }

    /** Logs that the activity resumed interaction. */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity.onResume")
    }

    /** Logs that the activity is pausing. */
    override fun onPause() {
        Log.d(TAG, "MainActivity.onPause")
        super.onPause()
    }

    /** Logs that the activity stopped. */
    override fun onStop() {
        Log.d(TAG, "MainActivity.onStop")
        super.onStop()
    }

    /** Logs that the activity is being destroyed. */
    override fun onDestroy() {
        Log.d(TAG, "MainActivity.onDestroy")
        super.onDestroy()
    }
}
