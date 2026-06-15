package com.doomhamsters.ui.cheating

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.doomhamsters.cheating.presentation.SnackStashClaimConfirmationCopy
import com.doomhamsters.ui.theme.AccentOrange
import com.doomhamsters.ui.theme.BackgroundCream
import com.doomhamsters.ui.theme.CardDarkMaroon

/** Confirms how the selected hand card should be used during Doom resolution. */
@Composable
fun SnackStashClaimConfirmationDialog(
    copy: SnackStashClaimConfirmationCopy,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(copy.title, fontWeight = FontWeight.Black)
        },
        text = {
            Text(copy.message)
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                onClick = onConfirm
            ) {
                Text(copy.confirmLabel, color = CardDarkMaroon, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = BackgroundCream)
            }
        },
        containerColor = CardDarkMaroon,
        titleContentColor = BackgroundCream,
        textContentColor = BackgroundCream
    )
}
