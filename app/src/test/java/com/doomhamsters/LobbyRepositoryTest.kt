package com.doomhamsters

import com.doomhamsters.data.User
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.collect
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
import org.hildan.krossbow.stomp.subscribeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LobbyRepositoryTest {

    private lateinit var mockHttpClient: OkHttpClient
    private lateinit var mockStompClient: StompClient
    private lateinit var mockSession: StompSession
    private lateinit var mockCall: Call
    private lateinit var repository: LobbyRepository

    private val testUser = User("u1", "HamsterPro", "hamster")
    private val testLobbyJson = """
        {"lobbyId":"TEST","members":[{"id":"u1","username":"HamsterPro","avatar":"hamster"}],"qrCodeBase64":"base64qr==","gameId":"game-1","gameStarted":true}
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        mockHttpClient = mockk()
        mockStompClient = mockk()
        mockSession = mockk(relaxed = true)
        mockCall = mockk()
        repository = LobbyRepository("localhost:8080", mockHttpClient, mockStompClient)
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
    fun `triggerGameStart sends post request to correct url`() = runTest {
        stubHttpResponse(200, "")

        repository.triggerGameStart("TEST_LOBBY", "u1")

        coVerify {
            mockHttpClient.newCall(match { request ->
                val url = request.url.toString()
                url.contains("/api/game/start") &&
                    url.contains("lobbyId=TEST_LOBBY") &&
                    url.contains("userId=u1") &&
                    request.method == "POST"
            })
        }
    }

    @Test
    fun `joinLobby throws backend error when join is rejected`() = runTest {
        stubHttpResponse(409, """{"error":"Game already started"}""")

        val error = try {
            repository.joinLobby("TEST", testUser)
            fail("Expected joinLobby to throw")
        } catch (error: IllegalStateException) {
            error
        }

        assertEquals("Game already started", error.message)
    }

    @Test
    fun `subscribeGameStart extracts game id from json`() = runTest {
        mockkStatic("org.hildan.krossbow.stomp.StompSessionKt")
        coEvery { mockStompClient.connect(any()) } returns mockSession
        repository.connect()

        val stompFlow = kotlinx.coroutines.flow.flowOf("""{"gameId":"neue-game-uuid-123"}""")
        coEvery { mockSession.subscribeText(any()) } returns stompFlow

        val repoFlow = repository.subscribeGameStart("TEST_LOBBY")

        repoFlow.collect { extractedId ->
            assertEquals("neue-game-uuid-123", extractedId)
        }
    }

    @Test
    fun `createLobby returns parsed lobby on success`() = runTest {
        stubHttpResponse(200, testLobbyJson)

        val lobby = repository.createLobby("Test", testUser)

        assertEquals("TEST", lobby.lobbyId)
        assertEquals(1, lobby.members.size)
        assertEquals("HamsterPro", lobby.members[0].username)
        assertEquals("base64qr==", lobby.qrCodeBase64)
        assertEquals("game-1", lobby.gameId)
        assertEquals(true, lobby.gameStarted)
    }

    @Test
    fun `createLobby parses null qrCode as null`() = runTest {
        val jsonWithoutQr = """{"lobbyId":"NOQUR","members":[],"qrCodeBase64":""}"""
        stubHttpResponse(200, jsonWithoutQr)

        val lobby = repository.createLobby("NoQr", testUser)

        assertNull(lobby.qrCodeBase64)
    }

    @Test
    fun `createLobby infers game start when payload contains game id alias`() = runTest {
        stubHttpResponse(
            200,
            """{"lobbyId":"TEST","members":[],"currentGameId":"game-alias"}"""
        )

        val lobby = repository.createLobby("Alias", testUser)

        assertEquals("game-alias", lobby.gameId)
        assertEquals(true, lobby.gameStarted)
    }

    @Test
    fun `createLobby throws on server error`() = runTest {
        stubHttpResponse(500, "")

        assertThrows<IllegalStateException> {
            repository.createLobby("Fail", testUser)
        }
    }

    @Test
    fun `joinLobby returns parsed lobby on success`() = runTest {
        stubHttpResponse(200, testLobbyJson)

        val lobby = repository.joinLobby("TEST", testUser)

        assertEquals("TEST", lobby?.lobbyId)
        assertEquals("HamsterPro", lobby?.members?.get(0)?.username)
        assertEquals("game-1", lobby?.gameId)
    }

    @Test
    fun `joinLobby returns null when lobby not found`() = runTest {
        stubHttpResponse(404, "")

        val result = repository.joinLobby("MISSING", testUser)

        assertNull(result)
    }

    @Test
    fun `getLobby returns parsed lobby on success`() = runTest {
        stubHttpResponse(200, testLobbyJson)

        val lobby = repository.getLobby("TEST")

        assertEquals("TEST", lobby?.lobbyId)
        assertEquals(1, lobby?.members?.size)
        assertEquals("game-1", lobby?.gameId)
    }

    @Test
    fun `subscribeLobbyUpdates throws when not connected`() = runTest {
        assertThrows<IllegalStateException> {
            repository.subscribeLobbyUpdates("TEST").collect()
        }
    }

    @Test
    fun `leaveLobby sends post request to correct url`() = runTest {
        stubHttpResponse(200, "")

        repository.leaveLobby("TEST_LOBBY", "u1")

        coVerify {
            mockHttpClient.newCall(match { request ->
                request.url.toString().contains("/api/lobby/TEST_LOBBY/leave") &&
                    request.method == "POST"
            })
        }
    }

    @Test
    fun `leaveLobby handles server error gracefully without throwing`() = runTest {
        stubHttpResponse(500, "")

        repository.leaveLobby("TEST_LOBBY", "u1")
        // Should complete without throwing
    }

    @Test
    fun `getLobby returns null for non-200 response`() = runTest {
        stubHttpResponse(404, "")

        val result = repository.getLobby("MISSING")

        assertNull(result)
    }

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
}
