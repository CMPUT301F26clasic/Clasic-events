/**
 * Purpose: The popup used to write a message between a campsite owner and a camper.
 * Outstanding issues: None.
 */
package com.example.clasicampsite

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Popup for typing a message of up to 500 characters, used both by owners messaging a guest
 * (US 02.12.01) and campers messaging an owner (US 01.14.01).
 *
 * @param recipientName shown in the title, like "Message Alex"
 * @param isSending true while the message is being saved, so the buttons are turned off
 * @param error a message to show if sending failed, or null
 * @param onSend called with the typed text when Send is tapped
 * @param onDismiss called when the popup is closed without sending
 */
@Composable
fun MessageReserverDialog(
    recipientName: String,
    isSending: Boolean,
    error: String?,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = { Text("Message $recipientName") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 500) text = it },
                label = { Text("Message") },
                supportingText = { Text(error ?: "${text.length}/500") },
                isError = error != null,
                modifier = Modifier.testTag("messageField")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSend(text) },
                enabled = !isSending && text.isNotBlank(),
                modifier = Modifier.testTag("sendMessageButton")
            ) { Text(if (isSending) "Sending..." else "Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSending) { Text("Cancel") }
        }
    )
}