package com.doomhamsters

import android.util.Log
import com.doomhamsters.data.Lobby
import com.doomhamsters.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Handles all lobby network communication:
 *  - REST (HTTP) for create / join / get
 *  - STOMP (WebSocket) for real-time lobby subscriptions
 *
 * @param baseUrl host:port of the backend, e.g. "10.0.2.2:53217"
 */
class LobbyRepository(
    private val baseUrl: String,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val stompClient: StompClient = StompClient(
        OkHttpWebSocketClient(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .build()
        )
    )
) {
    private companion object {
        const val TAG = "LobbyDebug"
    }

    internal var session: StompSession? = null

    /** Returns whether the realtime lobby STOMP session is currently open. */
    fun isConnected(): Boolean = session != null

    /** Opens the STOMP session used for lobby subscriptions. */
    suspend fun connect() {
        if (session != null) return
        Log.d(TAG, "Opening STOMP session to ws://$baseUrl/ws")
        session = stompClient.connect("ws://$baseUrl/ws")
    }

    /** Closes the active lobby STOMP session. */
    suspend fun disconnect() {
        Log.d(TAG, "Closing STOMP session")
        session?.disconnect()
        session = null
    }

    /** Creates a new lobby for the supplied group and user. */
    suspend fun createLobby(groupName: String, user: User): Lobby {
        val body = JSONObject()
            .put("groupName", groupName)
            .put("user", user.toJson())
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://$baseUrl/api/lobby/create")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                check(response.isSuccessful) {
                    payload.toErrorMessage() ?: "Create lobby failed: ${response.code}"
                }
                payload.also {
                    Log.d(TAG, "createLobby response: ${it.toLobbySummary()}")
                }.toLobby()
            }
        }
    }

    /** Attempts to join an existing lobby and returns its snapshot on success. */
    suspend fun joinLobby(lobbyId: String, user: User): Lobby? {
        val body = user.toJson().toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://$baseUrl/api/lobby/$lobbyId/join")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> payload.also {
                        Log.d(TAG, "joinLobby response for $lobbyId: ${it.toLobbySummary()}")
                    }.toLobby()

                    response.code == 404 -> {
                        Log.d(TAG, "joinLobby failed for $lobbyId with status ${response.code}")
                        null
                    }

                    else -> {
                        val errorMessage = payload.toErrorMessage()
                            ?: "Join lobby failed: ${response.code}"
                        Log.d(
                            TAG,
                            "joinLobby failed for $lobbyId with status ${response.code}: $errorMessage"
                        )
                        error(errorMessage)
                    }
                }
            }
        }
    }

    /** Fetches the latest lobby snapshot. */
    suspend fun getLobby(lobbyId: String): Lobby? {
        val request = Request.Builder()
            .url("http://$baseUrl/api/lobby/$lobbyId")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string().orEmpty().also { payload ->
                        Log.d(TAG, "getLobby response for $lobbyId: ${payload.toLobbySummary()}")
                    }.toLobby()
                } else {
                    Log.d(TAG, "getLobby failed for $lobbyId with status ${response.code}")
                    null
                }
            }
        }
    }

    /** Subscribes to realtime lobby updates. */
    suspend fun subscribeLobbyUpdates(lobbyId: String): Flow<Lobby> =
        checkNotNull(session) { "Call connect() before subscribing" }
            .subscribeText("/topic/lobby/$lobbyId")
            .map { payload ->
                Log.d(TAG, "STOMP /topic/lobby/$lobbyId update: ${payload.toLobbySummary()}")
                payload.toLobby()
            }

    /** Requests that the backend start a game for the lobby. */
    suspend fun triggerGameStart(lobbyId: String, userId: String) {
        val body = ByteArray(0).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://$baseUrl/api/game/start?lobbyId=$lobbyId&userId=$userId")
            .post(body)
            .build()

        withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                check(response.isSuccessful) {
                    payload.toErrorMessage() ?: "Game start failed: ${response.code}"
                }
            }
        }
    }

    /** Subscribes to realtime game start notifications for a lobby. */
    suspend fun subscribeGameStart(lobbyId: String): Flow<String> =
        checkNotNull(session) { "Call connect() before subscribing" }
            .subscribeText("/topic/game/$lobbyId")
            .map { jsonString ->
                Log.d(TAG, "STOMP /topic/game/$lobbyId start payload: $jsonString")
                JSONObject(jsonString).getString("gameId")
            }

    suspend fun leaveLobby(lobbyId: String, userId: String) {
        val body = JSONObject().put("userId", userId).toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://$baseUrl/api/lobby/$lobbyId/leave")
            .post(body)
            .build()

        withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Leave lobby failed: ${response.code}")
                }
            }
        }
    }
}

private fun User.toJson() = JSONObject()
    .put("id", id)
    .put("username", username)
    .put("avatar", avatar)

private fun String.toLobby(): Lobby {
    val json = JSONObject(this)
    val members = json.optJSONArray("members")?.let { arr ->
        (0 until arr.length()).map { arr.getJSONObject(it).toUser() }
    } ?: emptyList()
    val parsedGameId = json.optNullableString("gameId")
        ?: json.optNullableString("currentGameId")
        ?: json.optNullableString("startedGameId")
    return Lobby(
        lobbyId = json.getString("lobbyId"),
        members = members,
        qrCodeBase64 = json.optString("qrCodeBase64").takeIf { it.isNotEmpty() },
        gameId = parsedGameId,
        gameStarted = json.optBoolean("gameStarted") ||
            json.optBoolean("started") ||
            !parsedGameId.isNullOrBlank()
    )
}

private fun JSONObject.toUser() = User(
    id = getString("id"),
    username = getString("username"),
    avatar = getString("avatar")
)

private fun JSONObject.optNullableString(key: String): String? =
    optString(key).takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

private fun String.toErrorMessage(): String? =
    takeIf { it.isNotBlank() }?.let { body ->
        runCatching { JSONObject(body).optString("error") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

private fun String.toLobbySummary(): String {
    val json = JSONObject(this)
    val memberNames = json.optJSONArray("members")?.let { membersArray ->
        (0 until membersArray.length()).map { index ->
            membersArray.getJSONObject(index).optString("username", "?")
        }
    }.orEmpty()
    val gameId = json.optString("gameId").ifBlank { json.optString("currentGameId") }
    return "lobbyId=${json.optString("lobbyId")}, members=${memberNames.size}, names=$memberNames, gameId=${gameId.ifBlank { "null" }}, started=${json.optBoolean("gameStarted") || json.optBoolean("started")}"
}
