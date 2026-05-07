package com.doomhamsters

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.ConnectException

class StompServiceTest {

    @Test
    fun testStompConnection() = runBlocking {
        val stompService = StompService()
        
        try {
            // Test connecting to local backend
            val session = stompService.connect("ws://localhost:53217/ws")
            
            // This would send "Hello Backend!" and wait for a response on "/topic/test"
            val result = stompService.testCommunication(session)
            println("Result: $result")
            assertEquals("[BACKEND]: Hello App!\nI received from you the following text: [APP]: Hello Backend!", result)
            
            session.disconnect()
        } catch (e: ConnectException) {
            println("Backend not reachable. Start your Spring Boot app at port 53217.")
        }
    }
}