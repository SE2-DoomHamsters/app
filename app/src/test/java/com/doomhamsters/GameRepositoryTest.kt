package com.doomhamsters


import android.util.Log
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameRepositoryTest {

    private lateinit var mockHttpClient: OkHttpClient
    private lateinit var mockStompClient: StompClient
    private lateinit var mockSession: StompSession
    private lateinit var repository: GameRepository

    @BeforeEach
    fun setUp() {
        mockHttpClient = mockk(relaxed = true)
        mockStompClient = mockk()
        mockSession = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        repository = GameRepository("localhost:8080", mockHttpClient, mockStompClient)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
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
}
