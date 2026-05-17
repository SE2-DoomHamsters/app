package com.doomhamsters.data
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
data class User(
    val id: String = "",
    val username: String = "",
    val avatar: String = ""
)

data class Lobby(
    val lobbyId: String = "",
    val members: List<User> = emptyList(),
    val qrCodeBase64: String? = null,
    val gameId: String? = null,
    val gameStarted: Boolean = false
)

//Macht aus dem text vom Server ein echtes Bild
fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}
