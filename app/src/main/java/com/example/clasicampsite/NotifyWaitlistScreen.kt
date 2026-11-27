/**
 * Purpose: The screen a campsite owner uses to send a notification to everyone on their
 * campsite's lottery waitlist (US 02.10.01). Opened from the owner's campsite screen.
 * Links to the Sent Notifications screen (US 02.10.02).
 * Outstanding issues: None known.
 */
package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.clasicampsite.model.WaitlistNotice

/** The Notify Waitlist screen for the campsite with [campsiteId]. */
@Composable
fun NotifyWaitlistScreen(navController: NavController, campsiteId: String) {
    val viewModel: NotifyWaitlistViewModel = viewModel()
    LaunchedEffect(campsiteId) { viewModel.start(campsiteId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton"),
        ) {
            Text("Back")
        }
        Text("Notify Waitlist", fontSize = 24.sp, modifier = Modifier.testTag("notifyWaitlistTitle"))

        if (!viewModel.loaded) {
            Text("Loading...")
            return@Column
        }

        Text(viewModel.campsiteName, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        val count = viewModel.waitlistUserIds.size
        Text(
            if (count == 1) "1 entrant on the waitlist" else "$count entrants on the waitlist",
            modifier = Modifier.testTag("waitlistCountText"),
        )

        OutlinedTextField(
            value = viewModel.message,
            onValueChange = { viewModel.message = it },
            label = { Text("Message") },
            supportingText = { Text("${viewModel.message.length}/${WaitlistNotice.MAX_LENGTH}") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("waitlistMessageField"),
        )
        Button(
            onClick = { viewModel.send() },
            enabled = viewModel.isOwner && !viewModel.sending,
            modifier = Modifier.fillMaxWidth().testTag("sendWaitlistNotificationButton"),
        ) {
            Text(if (viewModel.sending) "Sending..." else "Send to waitlist")
        }
        Text(viewModel.status, modifier = Modifier.testTag("notifyWaitlistStatus"))
        TextButton(
            onClick = { navController.navigate("sent_notifications") },
            modifier = Modifier.testTag("viewSentNotificationsButton"),
        ) {
            Text("View sent notifications")
        }
    }
}
