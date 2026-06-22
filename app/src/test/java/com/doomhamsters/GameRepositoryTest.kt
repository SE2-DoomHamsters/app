package com.doomhamsters


import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameRepositoryTest {

    private lateinit var mockStompClient: StompClient
    private lateinit var mockSession: StompSession
    private lateinit var repository: GameRepository

    @BeforeEach
    fun setUp() {
        mockStompClient = mockk()
        mockSession = mockk(relaxed = true)
        repository = GameRepository("localhost:8080", stompClient = mockStompClient)
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
            repository.subscribeToGame("GAME-1")
        }
    }

    @Test
    fun `subscribeToPrivateEvents throws when not connected`() = runTest {
        assertThrows<IllegalStateException> {
            repository.subscribeToPrivateEvents("GAME-1", "PLAYER-1")
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
}
