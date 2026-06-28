package com.doomhamsters


import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.json.JSONObject
import kotlinx.coroutines.flow.collect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameRepositoryTest {

    private lateinit var mockHttpClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var mockStompClient: StompClient
    private lateinit var mockSession: StompSession
    private lateinit var repository: GameRepository

    @BeforeEach
    fun setUp() {
        mockHttpClient = mockk()
        mockCall = mockk()
        mockStompClient = mockk()
        mockSession = mockk(relaxed = true)
        repository = GameRepository("localhost:8080", httpClient = mockHttpClient, stompClient = mockStompClient)
    }

    private val validBackendJson =
        """{"gameId":"g1","players":[],"gameState":"RUNNING","turnCount":0,"remainingDeckSize":5}"""

    private fun stubHttpResponse(code: Int, body: String) {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        val dummyRequest = Request.Builder().url("http://localhost:8080/test").build()
        val response = Response.Builder()
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .protocol(Protocol.HTTP_1_1)
            .request(dummyRequest)
            .body(responseBody)
            .build()
        every { mockHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns response
    }

    @Test
    fun `connect opens stomp session with correct url`() = runTest {
        coEvery { mockStompClient.connect("ws://localhost:8080/ws") } returns mockSession

        repository.connect()

        coVerify { mockStompClient.connect("ws://localhost:8080/ws") }
    }

    @Test
    fun `disconnect closes session and clears reference`() = runTest {
        coEvery { mockStompClient.connect(any()) } returns mockSession
        coEvery { mockSession.disconnect() } just Runs
        repository.connect()

        repository.disconnect()

        coVerify { mockSession.disconnect() }
        assertNull(repository.session)
    }

    @Test
    fun `disconnect does nothing when not connected`() = runTest {
        repository.disconnect()

        assertNull(repository.session)
    }

    @Test
    fun `subscribeToGame throws when not connected`() = runTest {
        assertThrows<IllegalStateException> {
            repository.subscribeToGame("GAME-1").collect()
        }
    }

    @Test
    fun `subscribeToPrivateEvents throws when not connected`() = runTest {
        assertThrows<IllegalStateException> {
            repository.subscribeToPrivateEvents("GAME-1", "PLAYER-1").collect()
        }
    }

    @Test
    fun `sendAction throws when not connected`() = runTest {
        val payload = mockk<JSONObject>(relaxed = true)

        assertThrows<IllegalStateException> {
            repository.sendAction("GAME-1", "draw", payload)
        }
    }

    @Test
    fun `parseErrorMessageOrNull returns message for valid payload`() {
        val message = repository.parseErrorMessageOrNull("{\"message\":\"Not your turn\"}")

        assertEquals("Not your turn", message)
    }

    @Test
    fun `parseErrorMessageOrNull drops malformed payload`() {
        assertNull(repository.parseErrorMessageOrNull("not json"))
    }

    @Test
    fun `parsePrivateEventOrNull parses valid payload`() {
        val event = repository.parsePrivateEventOrNull("{\"type\":\"DOOM_DRAWN\"}")

        assertEquals("DOOM_DRAWN", event?.optString("type"))
    }

    @Test
    fun `parsePrivateEventOrNull drops malformed payload`() {
        assertNull(repository.parsePrivateEventOrNull("{not valid"))
    }

    @Test
    fun `parseGameStateOrNull drops malformed payload`() {
        assertNull(repository.parseGameStateOrNull("garbage"))
    }

    // region fetchGameState

    @Test
    fun `fetchGameState returns parsed game state on 200`() = runTest {
        stubHttpResponse(200, validBackendJson)
        val state = repository.fetchGameState("g1", "p1")
        assertEquals("g1", state.id)
    }

    @Test
    fun `fetchGameState throws when server returns non-200`() = runTest {
        stubHttpResponse(500, "")
        assertThrows<IllegalStateException> {
            repository.fetchGameState("g1", "p1")
        }
    }

    @Test
    fun `fetchGameState throws when response body is malformed`() = runTest {
        stubHttpResponse(200, "not valid json")
        assertThrows<IllegalStateException> {
            repository.fetchGameState("g1", "p1")
        }
    }

    // endregion

    // region fetchGameStateOrNull

    @Test
    fun `fetchGameStateOrNull returns null for 404`() = runTest {
        stubHttpResponse(404, "")
        val result = repository.fetchGameStateOrNull("g1", "p1")
        assertNull(result)
    }

    @Test
    fun `fetchGameStateOrNull returns parsed game state on 200`() = runTest {
        stubHttpResponse(200, validBackendJson)
        val result = repository.fetchGameStateOrNull("g1", "p1")
        assertNotNull(result)
        assertEquals("g1", result?.id)
    }

    @Test
    fun `fetchGameStateOrNull throws when server returns non-200 non-404`() = runTest {
        stubHttpResponse(500, "")
        assertThrows<IllegalStateException> {
            repository.fetchGameStateOrNull("g1", "p1")
        }
    }

    @Test
    fun `fetchGameStateOrNull returns null for malformed response body`() = runTest {
        stubHttpResponse(200, "not valid json")
        val result = repository.fetchGameStateOrNull("g1", "p1")
        assertNull(result)
    }

    // endregion
}
