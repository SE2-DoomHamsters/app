package com.doomhamsters.data
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
/**
 * Repräsentiert einen Spieler im Spiel mit einer eindeutigen ID,
 * seinem gewählten Namen und einem Avatar-Emoji.
 */
data class User(
    val id: String = "",
    val username: String = "",
    val avatar: String = ""
)
/**
 * Datenmodell für eine Spielgruppe (Lobby).
 * Enthält die eindeutige Lobby-ID, die Liste der aktuell beigetretenen Mitglieder
 * sowie den vom Server generierten QR-Code als Base64-String.
 */
data class Lobby(
    val lobbyId: String = "",
    val members: List<User> = emptyList(),
    val qrCodeBase64: String? = null,
    val gameId: String? = null,
    val gameStarted: Boolean = false
)
/**
 * Konvertiert einen Base64-codierten String (z. B. den QR-Code vom Server)
 * in ein Android-[Bitmap], damit es in der Compose-UI dargestellt werden kann.
 *
 * @param base64Str Der vom Backend übermittelte Base64-String des Bildes.
 * @return Das decodierte [Bitmap] für die UI-Darstellung oder `null`, falls die Konvertierung fehlschlägt.
 */
fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}
