package com.doomhamsters

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
    internal var session: StompSession? = null

    /** Opens the persistent WebSocket connection used for STOMP subscriptions. */
    suspend fun connect() {
        session = stompClient.connect("ws://$baseUrl/ws")
    }

    /** Closes the WebSocket connection. */
    suspend fun disconnect() {
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
                response.body!!.string().toLobby()
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
                if (response.isSuccessful) response.body!!.string().toLobby() else null
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
            .map { it.toLobby() }
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
                android.util.Log.d("WEBSOCKET_TEST", "RAW SIGNAL VOM SERVER: $jsonString")
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
    return Lobby(
        lobbyId = json.getString("lobbyId"),
        members = members,
        qrCodeBase64 = json.optString("qrCodeBase64").takeIf { it.isNotEmpty() }
    )
}

private fun JSONObject.toUser() = User(
    id = getString("id"),
    username = getString("username"),
    avatar = getString("avatar")
)
