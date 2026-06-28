package com.doomhamsters

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/** Persists the local player identity, profile details, and active game session. */
interface SessionStore {
    fun getOrCreateUserId(): String
    fun loadUsername(): String?
    fun loadAvatar(): String?
    fun saveProfile(username: String, avatar: String)
    fun saveActiveGameId(gameId: String, lobbyId: String?)
    fun loadActiveGameId(): String?
    fun loadActiveLobbyId(): String?
    fun clearActiveGame()
}

/** SharedPreferences-backed session store for local player identity and profile details. */
class SharedPrefsSessionStore(context: Context) : SessionStore {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun getOrCreateUserId(): String {
        val existingUserId = prefs.getString(KEY_USER_ID, null)
        if (!existingUserId.isNullOrBlank()) {
            return existingUserId
        }

        val newUserId = UUID.randomUUID().toString()
        prefs.edit { putString(KEY_USER_ID, newUserId) }
        return newUserId
    }

    override fun loadUsername(): String? = prefs.getString(KEY_USERNAME, null)

    override fun loadAvatar(): String? = prefs.getString(KEY_AVATAR, null)

    override fun saveProfile(username: String, avatar: String) {
        prefs.edit {
            putString(KEY_USERNAME, username)
            putString(KEY_AVATAR, avatar)
        }
    }

    override fun saveActiveGameId(gameId: String, lobbyId: String?) {
        prefs.edit {
            putString(KEY_ACTIVE_GAME_ID, gameId)
            putString(KEY_ACTIVE_LOBBY_ID, lobbyId)
        }
    }

    override fun loadActiveGameId(): String? = prefs.getString(KEY_ACTIVE_GAME_ID, null)

    override fun loadActiveLobbyId(): String? = prefs.getString(KEY_ACTIVE_LOBBY_ID, null)

    override fun clearActiveGame() {
        prefs.edit {
            remove(KEY_ACTIVE_GAME_ID)
            remove(KEY_ACTIVE_LOBBY_ID)
        }
    }

    private companion object {
        const val PREFS_NAME = "doomhamsters.session"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_AVATAR = "avatar"
        const val KEY_ACTIVE_GAME_ID = "active_game_id"
        const val KEY_ACTIVE_LOBBY_ID = "active_lobby_id"
    }
}
