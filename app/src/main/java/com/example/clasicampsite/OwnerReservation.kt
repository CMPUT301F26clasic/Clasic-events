package com.example.clasicampsite

import android.provider.Settings
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Reservation
import kotlinx.coroutines.launch

/**
 * Owner screen listing the reservations for one campsite, with a button to message each guest.
 *
 * @param navController used to navigate back
 * @param campsiteId ID of the campsite whose reservations are shown
 * @param dbController data access used to load reservations and send messages
 */
@Composable
fun OwnerReservationsScreen(
    navController: NavController,
    campsiteId: String,
    dbController: DatabaseController = remember { DatabaseController() }
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ownerId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var messageTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // id to name
    var isSending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(campsiteId) {
        try {
            reservations = dbController.getReservationsForCampsite(campsiteId)
            names = reservations.map { it.userId }.distinct().associateWith { id ->
                dbController.getUser(id).name.ifBlank { "Unnamed guest" }
            }
        } catch (e: Exception) {
            loadError = "Failed to load reservations: ${e.localizedMessage}"
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton")
        ) { Text("Back") }

        Text("Reservations", style = MaterialTheme.typography.headlineMedium)

        when {
            loadError != null -> Text(loadError!!, color = MaterialTheme.colorScheme.error)
            reservations.isEmpty() -> Text(
                "No reservations yet.",
                modifier = Modifier.testTag("noReservationsText")
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reservations, key = { it.id }) { reservation ->
                    val name = names[reservation.userId] ?: "Guest"
                    Row(
                        Modifier.fillMaxWidth().testTag("reservationRow"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name)
                        Button(
                            onClick = { messageTarget = reservation.userId to name },
                            modifier = Modifier.testTag("messageButton")
                        ) { Text("Message") }
                    }
                }
            }
        }
    }

    messageTarget?.let { (recipientId, name) ->
        MessageReserverDialog(
            recipientName = name,
            isSending = isSending,
            error = sendError,
            onDismiss = { messageTarget = null; sendError = null },
            onSend = { text ->
                scope.launch {
                    isSending = true
                    sendError = null
                    try {
                        dbController.sendMessageToReserver(ownerId, campsiteId, recipientId, text)
                        messageTarget = null
                    } catch (e: Exception) {
                        sendError = e.localizedMessage
                    } finally {
                        isSending = false
                    }
                }
            }
        )
    }
}