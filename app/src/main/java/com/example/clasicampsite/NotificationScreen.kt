/*
 * Purpose: Displays in-app notifications for the user, including campsite lottery
 * win and loss updates (#5).
 * Outstanding issues: Uses placeholder device ID if not wired to MainActivity yet.
 */
package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController

@Composable
fun NotificationScreen(
    navController: NavController,
    deviceId: String = "",
    dbController: DatabaseController = remember { DatabaseController() },
) {
    var notifications by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(deviceId) {
        try {
            isLoading = true
            val items = dbController.getNotificationsForUser(deviceId)
            notifications = items.map { it.message }
        } catch (_: Exception) {
            notifications = emptyList()
        } finally {
            isLoading = false
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton") // back button
        ) {
            Text("Back")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton"),
        ) {
            Text("Back")
        }

        Text(
            text = "Notifications",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("notificationsTitle"),
        )

        when {
            isLoading -> Text("Loading notifications...")
            notifications.isEmpty() -> Text(
                text = "No notifications yet.",
                modifier = Modifier.testTag("emptyNotificationsText"),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("notificationsList"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notifications) { msg ->
                    Card(modifier = Modifier.fillMaxWidth().testTag("notificationItem")) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}