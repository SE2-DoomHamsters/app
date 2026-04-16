package com.doomhamsters

import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import kotlinx.coroutines.flow.first

class StompService {
    private val client = StompClient(OkHttpWebSocketClient())

    suspend fun connect(url: String): StompSession {
        return client.connect(url)
    }

    suspend fun testCommunication(session: StompSession): String {
        // Subscribe to a topic
        val subscription = session.subscribeText("/topic/test")
        
        // Send a message
        session.sendText("/app/hello", "Hello Backend!")

        // Wait for the first response message
        return subscription.first()
    }
}