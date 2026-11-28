package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.NotificationLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminNotificationLogsScreen(
    navController: NavController,
    dbController: DatabaseController = remember { DatabaseController() }
) {
    var logs by remember { mutableStateOf<List<NotificationLog>>(emptyList()) }
    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ownerMessagesOnly by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            logs = dbController.getAllNotificationLogs()
            val ids = logs.flatMap { listOf(it.recipientId, it.senderId) }
                .filter { it.isNotBlank() }.distinct()
            names = ids.associateWith { id ->
                dbController.getUser(id).name.ifBlank { "Unnamed user" }
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load notification logs: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    val shown = if (ownerMessagesOnly) logs.filter {
        it.type == "owner_message" || it.type == "entrant_message"
    } else logs
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton")
        ) { Text("Back") }

        Text(
            "Notification Logs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            if (!ownerMessagesOnly) {
                Button(onClick = {}, modifier = Modifier.testTag("filterAll")) { Text("All") }
                OutlinedButton(
                    onClick = { ownerMessagesOnly = true },
                    modifier = Modifier.testTag("filterOwnerMessages")
                ) { Text("Owner messages") }
            } else {
                OutlinedButton(
                    onClick = { ownerMessagesOnly = false },
                    modifier = Modifier.testTag("filterAll")
                ) { Text("All") }
                Button(onClick = {}, modifier = Modifier.testTag("filterOwnerMessages")) {
                    Text("Owner messages")
                }
            }
        }

        Text("${shown.size} entries", style = MaterialTheme.typography.bodyMedium)

        when {
            isLoading -> CircularProgressIndicator()
            errorMessage != null -> Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            shown.isEmpty() -> Text("No notifications found.", modifier = Modifier.testTag("emptyLogs"))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { log ->
                    Card(Modifier.fillMaxWidth().testTag("logEntry")) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                when (log.type) {
                                    "owner_message" -> "Owner message"
                                    "entrant_message" -> "Entrant message"
                                    "lottery_result" -> "Lottery result"
                                    else -> "System"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (log.senderId.isNotBlank()) {
                                Text("From: ${names[log.senderId] ?: log.senderId}")
                            }
                            Text("To: ${names[log.recipientId] ?: log.recipientId}")
                            if (log.campsiteName.isNotBlank()) Text("Campsite: ${log.campsiteName}")
                            Text(
                                dateFormat.format(Date(log.sentAt)),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(log.message)
                        }
                    }
                }
            }
        }
    }
}