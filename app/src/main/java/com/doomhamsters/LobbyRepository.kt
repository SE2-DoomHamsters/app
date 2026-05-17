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

/**
 * Handles all lobby network communication:
 *  - REST (HTTP) for create / join / get  (request-response)
 *  - STOMP (WebSocket) for real-time lobby update subscription
 *
 * @param baseUrl host:port of the backend, e.g. "10.0.2.2:53217"
 */
class LobbyRepository(
    private val baseUrl: String,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val stompClient: StompClient = StompClient(OkHttpWebSocketClient())
) {
    private companion object {
        const val TAG = "LobbyDebug"
    }

    internal var session: StompSession? = null

    fun isConnected(): Boolean = session != null

    /** Opens the persistent WebSocket connection used for STOMP subscriptions. */
    suspend fun connect() {
        if (session != null) return
        Log.d(TAG, "Opening STOMP session to ws://$baseUrl/ws")
        session = stompClient.connect("ws://$baseUrl/ws")
    }

    /** Closes the WebSocket connection. */
    suspend fun disconnect() {
        Log.d(TAG, "Closing STOMP session")
        session?.disconnect()
        session = null
    }

    /**
     * Creates a new lobby via REST and returns the created [Lobby].
     * The backend generates the QR code and lobby ID.
     */
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
                check(response.isSuccessful) { "Create lobby failed: ${response.code}" }
                response.body!!.string().also { payload ->
                    Log.d(TAG, "createLobby response: ${payload.toLobbySummary()}")
                }.toLobby()
            }
        }
    }

    /**
     * Joins an existing lobby via REST. Returns the updated [Lobby], or null if
     * the lobby ID does not exist on the server.
     */
    suspend fun joinLobby(lobbyId: String, user: User): Lobby? {
        val body = user.toJson().toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://$baseUrl/api/lobby/$lobbyId/join")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body!!.string().also { payload ->
                        Log.d(TAG, "joinLobby response for $lobbyId: ${payload.toLobbySummary()}")
                    }.toLobby()
                } else {
                    Log.d(TAG, "joinLobby failed for $lobbyId with status ${response.code}")
                    null
                }
            }
        }
    }

    /**
     * Fetches the current full lobby snapshot via REST.
     */
    suspend fun getLobby(lobbyId: String): Lobby? {
        val request = Request.Builder()
            .url("http://$baseUrl/api/lobby/$lobbyId")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body!!.string().also { payload ->
                        Log.d(TAG, "getLobby response for $lobbyId: ${payload.toLobbySummary()}")
                    }.toLobby()
                } else {
                    Log.d(TAG, "getLobby failed for $lobbyId with status ${response.code}")
                    null
                }
            }
        }
    }

    /**
     * Returns a [Flow] of [Lobby] updates broadcast by the server whenever a
     * player joins. Requires [connect] to have been called first.
     */
    suspend fun subscribeLobbyUpdates(lobbyId: String): Flow<Lobby> =
        checkNotNull(session) { "Call connect() before subscribing" }
            .subscribeText("/topic/lobby/$lobbyId")
            .map { payload ->
                Log.d(TAG, "STOMP /topic/lobby/$lobbyId update: ${payload.toLobbySummary()}")
                payload.toLobby()
            }
    /**
     * Sagt dem Server, dass das Spiel für diese Lobby gestartet werden soll.
     * Aufruf durch den Host.
     */
    suspend fun triggerGameStart(lobbyId: String) {
        val body = ByteArray(0).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://$baseUrl/api/game/start?lobbyId=$lobbyId")
            .post(body)
            .build()

        withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Game start failed: ${response.code}" }
            }
        }
    }

    /**
     * Lauscht auf das Start-Event vom Server.
     * Aufruf durch ALLE Mitglieder der Lobby.
     */
    suspend fun subscribeGameStart(lobbyId: String): Flow<String> =
        checkNotNull(session) { "Call connect() before subscribing" }
            .subscribeText("/topic/game/$lobbyId")
            .map { jsonString ->
                Log.d(TAG, "STOMP /topic/game/$lobbyId start payload: $jsonString")
                JSONObject(jsonString).getString("gameId")
            }
}

// ── Helper Methods ─────────────────────────────────────────────────────────────

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
    val hostObject = json.optJSONObject("host")
    val parsedHostId = json.optNullableString("hostId")
        ?: json.optNullableString("ownerId")
        ?: json.optNullableString("createdBy")
        ?: hostObject?.optNullableString("id")
        ?: hostObject?.optNullableString("userId")
    val parsedCanStart = json.optNullableBoolean("canStart")
        ?: json.optNullableBoolean("startAllowed")
        ?: json.optNullableBoolean("mayStart")
    val parsedMaxPlayers = json.optNullableInt("maxPlayers")
        ?: json.optNullableInt("capacity")
    return Lobby(
        lobbyId = json.getString("lobbyId"),
        members = members,
        qrCodeBase64 = json.optString("qrCodeBase64").takeIf { it.isNotEmpty() },
        gameId = parsedGameId,
        gameStarted = json.optBoolean("gameStarted") ||
            json.optBoolean("started") ||
            !parsedGameId.isNullOrBlank(),
        hostId = parsedHostId,
        canStart = parsedCanStart,
        maxPlayers = parsedMaxPlayers
    )
}

private fun JSONObject.toUser() = User(
    id = getString("id"),
    username = getString("username"),
    avatar = getString("avatar")
)

private fun JSONObject.optNullableString(key: String): String? =
    optString(key).takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

private fun JSONObject.optNullableBoolean(key: String): Boolean? =
    if (has(key) && !isNull(key)) optBoolean(key) else null

private fun JSONObject.optNullableInt(key: String): Int? =
    optInt(key).takeIf { has(key) && !isNull(key) && it > 0 }

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
