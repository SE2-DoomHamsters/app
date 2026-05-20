package com.doomhamsters

import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import kotlinx.coroutines.flow.first

/** Provides a lightweight helper for basic STOMP connectivity tests. */
class StompService {
    private val client = StompClient(OkHttpWebSocketClient())

    /** Opens a STOMP session to the supplied URL. */
    suspend fun connect(url: String): StompSession {
        return client.connect(url)
    }

    /** Sends a sample message and waits for the first response on the test topic. */
    suspend fun testCommunication(session: StompSession): String {
        // Subscribe to a topic
        val subscription = session.subscribeText("/topic/test")
        
        // Send a message
        session.sendText("/app/hello", "Hello Backend!")

        // Wait for the first response message
        return subscription.first()
    }
}