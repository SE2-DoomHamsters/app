package com.doomhamsters


import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameRepositoryTest {

    private val mockHttpClient = mockk<OkHttpClient>(relaxed = true)
    private val mockStompClient = mockk<StompClient>()
    private val mockSession = mockk<StompSession>(relaxed = true)
    private val repository = GameRepository("localhost:8080", mockHttpClient, mockStompClient)

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
        assertThrows<IllegalStateException> {
            repository.sendAction("GAME-1", "draw", JSONObject().put("playerId", "PLAYER-1"))
        }
    }
}
