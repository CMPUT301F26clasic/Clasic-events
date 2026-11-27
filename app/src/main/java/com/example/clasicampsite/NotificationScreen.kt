/**
 * Purpose: Displays in-app notifications for the user, including campsite lottery
 * win and loss updates (#5).
 * Outstanding issues: None.
 */
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
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

/**
 * Shows this user's notifications, newest first, like lottery wins and losses (US 01.03.01).
 *
 * @param navController used to go back
 * @param deviceId the device ID of the user whose notifications are shown, or "" for this phone
 * @param dbController where the notifications are loaded from
 */
@Composable
fun NotificationScreen(
    navController: NavController,
    deviceId: String = "",
    dbController: DatabaseController = remember { DatabaseController() },
) {
    var notifications by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    /**
     * The app's "notifications" route doesn't pass an ID, so fall back to this phone's ID
     * (the same ID the rest of the app uses to identify users, US 01.09.01).
     */
    val context = LocalContext.current
    val userId = remember(deviceId) {
        deviceId.ifBlank {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        }
    }

    LaunchedEffect(userId) {
        try {
            isLoading = true
            val items = dbController.getNotificationsForUser(userId)
            notifications = items.map { it.message }
        } catch (_: Exception) {
            notifications = emptyList()
        } finally {
            isLoading = false
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