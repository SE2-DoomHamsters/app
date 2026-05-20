package com.doomhamsters

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Creates `LobbyViewModel` instances with persistent local session storage. */
class LobbyViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(LobbyViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }

        @Suppress("UNCHECKED_CAST")
        return LobbyViewModel(
            sessionStore = SharedPrefsSessionStore(context)
        ) as T
    }
}
