package com.doomhamsters

import android.content.Context
import java.util.UUID

/** Persists the local player identity and profile details. */
interface SessionStore {
    fun getOrCreateUserId(): String
    fun loadUsername(): String?
    fun loadAvatar(): String?
    fun saveProfile(username: String, avatar: String)
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
        prefs.edit().putString(KEY_USER_ID, newUserId).apply()
        return newUserId
    }

    override fun loadUsername(): String? = prefs.getString(KEY_USERNAME, null)

    override fun loadAvatar(): String? = prefs.getString(KEY_AVATAR, null)

    override fun saveProfile(username: String, avatar: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_AVATAR, avatar)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "doomhamsters.session"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_AVATAR = "avatar"
    }
}
