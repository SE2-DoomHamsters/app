package com.doomhamsters

import com.doomhamsters.data.Lobby
import com.doomhamsters.data.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
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
import org.junit.jupiter.api.Assertions.assertEquals
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

    private val testUser = User("u1", "HamsterPro", "🐹")
    private val testLobbyJson = """
        {"lobbyId":"TEST","members":[{"id":"u1","username":"HamsterPro","avatar":"🐹"}],"qrCodeBase64":"base64qr=="}
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        mockHttpClient = mockk()
        mockStompClient = mockk()
        mockSession = mockk(relaxed = true)
        mockCall = mockk()
        repository = LobbyRepository("localhost:8080", mockHttpClient, mockStompClient)
    }

    // ── connect / disconnect ──────────────────────────────────────────────────

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
        // Should not throw even though session is null
        repository.disconnect()

        assertNull(repository.session)
    }

    // ── createLobby ──────────────────────────────────────────────────────────

    @Test
    fun `createLobby returns parsed lobby on success`() = runTest {
        stubHttpResponse(200, testLobbyJson)

        val lobby = repository.createLobby("Test", testUser)

        assertEquals("TEST", lobby.lobbyId)
        assertEquals(1, lobby.members.size)
        assertEquals("HamsterPro", lobby.members[0].username)
        assertEquals("base64qr==", lobby.qrCodeBase64)
    }

    @Test
    fun `createLobby parses null qrCode as null`() = runTest {
        val jsonWithoutQr =
            """{"lobbyId":"NOQUR","members":[],"qrCodeBase64":""}"""
        stubHttpResponse(200, jsonWithoutQr)

        val lobby = repository.createLobby("NoQr", testUser)

        assertNull(lobby.qrCodeBase64)
    }

    @Test
    fun `createLobby throws on server error`() = runTest {
        stubHttpResponse(500, "")

        assertThrows<IllegalStateException> {
            repository.createLobby("Fail", testUser)
        }
    }

    // ── joinLobby ─────────────────────────────────────────────────────────────

    @Test
    fun `joinLobby returns parsed lobby on success`() = runTest {
        stubHttpResponse(200, testLobbyJson)

        val lobby = repository.joinLobby("TEST", testUser)

        assertEquals("TEST", lobby?.lobbyId)
        assertEquals("HamsterPro", lobby?.members?.get(0)?.username)
    }

    @Test
    fun `joinLobby returns null when lobby not found`() = runTest {
        stubHttpResponse(404, "")

        val result = repository.joinLobby("MISSING", testUser)

        assertNull(result)
    }

    // ── subscribeLobbyUpdates ─────────────────────────────────────────────────

    @Test
    fun `subscribeLobbyUpdates throws when not connected`() = runTest {
        assertThrows<IllegalStateException> {
            repository.subscribeLobbyUpdates("TEST")
        }
    }

    // ── Helper Methods ───────────────────────────────────────────────────────────────

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
