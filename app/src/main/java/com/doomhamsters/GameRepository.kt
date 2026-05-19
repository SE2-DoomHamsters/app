package com.doomhamsters


import android.util.Log
import com.doomhamsters.model.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import org.json.JSONObject
import java.util.concurrent.TimeUnit


/** Coordinates REST and STOMP calls for an active game session. */
class GameRepository(
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
    private val tag = "GameRepository"
    internal var session: StompSession? = null


    /** Opens the STOMP session used for game subscriptions and actions. */
    suspend fun connect() {
        Log.d(tag, "Connecting STOMP to ws://$baseUrl/ws")
        session = stompClient.connect("ws://$baseUrl/ws")
        Log.d(tag, "STOMP connected")
    }

    /** Closes the active STOMP session, if one exists. */
    suspend fun disconnect() {
        Log.d(tag, "Disconnecting STOMP session")
        session?.disconnect()
        session = null
    }

    /** Subscribes to the public game event channel. */
    suspend fun subscribeToGame(gameId: String): Flow<String> =
        requireSession()
            .subscribeText("/topic/game/$gameId")

    /** Subscribes to realtime game state snapshots for a game. */
    suspend fun subscribeToGameState(gameId: String): Flow<GameState> =
        requireSession()
            .subscribeText("/topic/game-state/$gameId")
            .map { payload ->
                Log.d(tag, "WS game-state raw gameId=$gameId payload=$payload")
                GameState.fromJson(JSONObject(payload)).also { state ->
                    Log.d(
                        tag,
                        "WS game-state parsed gameId=$gameId currentPlayerId=${state.currentTurnPlayerId} turnCount=${state.turnCount} status=${state.status}"
                    )
                }
            }

    /** Subscribes to player-specific private game events. */
    suspend fun subscribeToPrivateEvents(gameId: String, playerId: String): Flow<JSONObject> =
        requireSession()
            .subscribeText("/queue/game/$gameId/$playerId")
            .map { payload ->
                Log.d(tag, "WS private-event gameId=$gameId playerId=$playerId payload=$payload")
                JSONObject(payload)
            }

    /** Fetches the latest game state snapshot over REST. */
    suspend fun fetchGameState(gameId: String, playerId: String): GameState {
        Log.d(tag, "REST fetch /api/game/$gameId/state?playerId=$playerId")
        val request = Request.Builder()
            .url("http://$baseUrl/api/game/$gameId/state?playerId=$playerId")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Game state fetch failed: ${response.code}" }
                val body = response.body!!.string()
                Log.d(tag, "REST response gameId=$gameId code=${response.code} body=$body")
                GameState.fromBackendJson(JSONObject(body)).also { state ->
                    Log.d(
                        tag,
                        "REST parsed gameId=$gameId currentPlayerId=${state.currentTurnPlayerId} turnCount=${state.turnCount} status=${state.status}"
                    )
                }
            }
        }
    }

    /** Sends a game action payload to the backend. */
    suspend fun sendAction(gameId: String, action: String, payload: JSONObject) {
        Log.d(tag, "STOMP send action gameId=$gameId action=$action payload=$payload")
        requireSession().sendText("/app/game/$gameId/$action", payload.toString())
    }

    private fun requireSession(): StompSession =
        checkNotNull(session) { "Call connect() before using GameRepository" }
}
