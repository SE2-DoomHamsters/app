package com.doomhamsters
import com.doomhamsters.data.User
import com.doomhamsters.data.decodeBase64ToBitmap
import org.junit.jupiter.api.Assertions.* // JUnit 5 statt Assert.*
import org.junit.jupiter.api.Test        // JUnit 5 statt org.junit.Test


class LobbyDataTest {

    @Test
    fun testInvalidString() {
        val result = decodeBase64ToBitmap("dies-ist-kein-bild")
        assertNull(result)
    }

    @Test
    fun testEmptyString() {
        val result = decodeBase64ToBitmap("")
        assertNull(result)
    }

    @Test
    fun testUserDefaults() {
        val user = User()
        assertEquals("", user.username)
        assertEquals("", user.avatar)
    }

}