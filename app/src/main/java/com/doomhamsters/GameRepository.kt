package com.doomhamsters


import android.util.Log
import com.doomhamsters.model.GameState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
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
    ),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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
    fun subscribeToGame(gameId: String): Flow<String> = flow {
        emitAll(requireSession().subscribeText("/topic/game/$gameId"))
    }

    /** Subscribes to realtime game state snapshots for a game. */
    fun subscribeToGameState(gameId: String): Flow<GameState> = flow {
        emitAll(
            requireSession()
                .subscribeText("/topic/game-state/$gameId")
                .mapNotNull { payload ->
                    Log.d(tag, "WS game-state raw gameId=$gameId payload=$payload")
                    parseGameStateOrNull(payload)?.also { state ->
                        Log.d(
                            tag,
                            "WS game-state parsed gameId=$gameId currentPlayerId=${state.currentTurnPlayerId} turnCount=${state.turnCount} status=${state.status}"
                        )
                    }
                }
        )
    }

    /** Subscribes to player-specific private game events. */
    fun subscribeToPrivateEvents(gameId: String, playerId: String): Flow<JSONObject> = flow {
        emitAll(
            requireSession()
                .subscribeText("/queue/game/$gameId/$playerId")
                .mapNotNull { payload ->
                    Log.d(tag, "WS private-event gameId=$gameId playerId=$playerId payload=$payload")
                    parsePrivateEventOrNull(payload)
                }
        )
    }

    /** Subscribes to player-specific error events for rejected or failed actions. */
    fun subscribeToErrors(gameId: String, playerId: String): Flow<String> = flow {
        emitAll(
            requireSession()
                .subscribeText("/queue/game/$gameId/$playerId/errors")
                .mapNotNull { payload ->
                    Log.d(tag, "WS error-event gameId=$gameId playerId=$playerId payload=$payload")
                    parseErrorMessageOrNull(payload)
                }
        )
    }

    /** Parses a game-state payload, logging and dropping it (returns null) when malformed. */
    internal fun parseGameStateOrNull(payload: String): GameState? =
        runCatching { GameState.fromJson(JSONObject(payload)) }
            .onFailure { Log.w(tag, "Dropping malformed game-state payload", it) }
            .getOrNull()

    /** Parses a private-event payload, logging and dropping it (returns null) when malformed. */
    internal fun parsePrivateEventOrNull(payload: String): JSONObject? =
        runCatching { JSONObject(payload) }
            .onFailure { Log.w(tag, "Dropping malformed private-event payload", it) }
            .getOrNull()

    /** Extracts the error message, logging and dropping the payload (returns null) when malformed. */
    internal fun parseErrorMessageOrNull(payload: String): String? =
        runCatching { JSONObject(payload).optString("message", "An error occurred.") }
            .onFailure { Log.w(tag, "Dropping malformed error payload", it) }
            .getOrNull()

    /** Fetches the latest game state snapshot over REST. */
    suspend fun fetchGameState(gameId: String, playerId: String): GameState {
        Log.d(tag, "REST fetch /api/game/$gameId/state?playerId=$playerId")
        val request = Request.Builder()
            .url("http://$baseUrl/api/game/$gameId/state?playerId=$playerId")
            .get()
            .build()

        return withContext(ioDispatcher) {
            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Game state fetch failed: ${response.code}" }
                val body = response.body!!.string()
                Log.d(tag, "REST response gameId=$gameId code=${response.code} body=$body")
                runCatching { GameState.fromBackendJson(JSONObject(body)) }
                    .getOrElse { error ->
                        Log.w(tag, "Malformed REST game-state body gameId=$gameId", error)
                        throw IllegalStateException("Malformed game-state response for game $gameId", error)
                    }
                    .also { state ->
                        Log.d(
                            tag,
                            "REST parsed gameId=$gameId currentPlayerId=${state.currentTurnPlayerId} turnCount=${state.turnCount} status=${state.status}"
                        )
                    }
            }
        }
    }

    /**
     * Fetches the game state, returning null if the game does not exist (HTTP 404).
     * Throws for other errors (network failure, server error, etc.).
     */
    suspend fun fetchGameStateOrNull(gameId: String, playerId: String): GameState? {
        Log.d(tag, "REST check /api/game/$gameId/state?playerId=$playerId")
        val request = Request.Builder()
            .url("http://$baseUrl/api/game/$gameId/state?playerId=$playerId")
            .get()
            .build()

        return withContext(ioDispatcher) {
            httpClient.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext null
                check(response.isSuccessful) { "Game state fetch failed: ${response.code}" }
                val body = response.body!!.string()
                runCatching { GameState.fromBackendJson(JSONObject(body)) }
                    .onFailure { Log.w(tag, "Dropping malformed REST game-state body gameId=$gameId", it) }
                    .getOrNull()
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
