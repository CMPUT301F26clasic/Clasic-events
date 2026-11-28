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