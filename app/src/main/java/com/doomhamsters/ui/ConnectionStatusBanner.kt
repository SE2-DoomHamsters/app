package com.doomhamsters.ui
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomhamsters.ui.theme.AccentOrange
import com.doomhamsters.ui.theme.DoomColor
import com.doomhamsters.ui.theme.SoftWhite
import com.doomhamsters.viewmodel.ConnectionStatus
@Composable
fun ConnectionStatusBanner(status: ConnectionStatus) {
    AnimatedVisibility(
        visible = status != ConnectionStatus.Connected,
        enter = slideInVertically(initialOffsetY = { -it}),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        val background = if (status is ConnectionStatus.Failed) DoomColor else AccentOrange
        val text = when (status) {
            is ConnectionStatus.Reconnecting ->
                "Reconnection… (${status.attempt}/${status.maxAttempts})"
            ConnectionStatus.Disconnected -> "Connection lost…"
            ConnectionStatus.Failed -> "Connection failed. The hamster has died. RIP. Please restart."
            ConnectionStatus.Connected -> ""
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status is ConnectionStatus.Reconnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = SoftWhite,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = SoftWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}