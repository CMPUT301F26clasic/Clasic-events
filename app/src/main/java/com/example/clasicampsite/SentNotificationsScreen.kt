/*
 * Purpose: Shows a campsite owner every notification they have sent to their waitlists,
 * newest first, with how many entrants got each one and when (US 02.10.02).
 * Opened from My Campsites or the Notify Waitlist screen.
 * Outstanding issues: None known.
 * Parts of this file were written with help from Claude (Anthropic), 2026-11-27
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.clasicampsite.model.SentNotice

/** The Sent Notifications screen. Reloads every time you open it. */
@Composable
fun SentNotificationsScreen(navController: NavController) {
    val viewModel: SentNotificationsViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.load() }

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
        Text("Sent Notifications", fontSize = 24.sp, modifier = Modifier.testTag("sentNotificationsTitle"))

        when {
            viewModel.loading && viewModel.notices.isEmpty() -> Text("Loading...")
            viewModel.message.isNotEmpty() -> Text(viewModel.message, color = MaterialTheme.colorScheme.error)
            viewModel.notices.isEmpty() -> Text(
                "You haven't sent any notifications yet.",
                modifier = Modifier.testTag("noSentNotificationsText"),
            )
            else -> LazyColumn(
                modifier = Modifier.testTag("sentNotificationsList"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(viewModel.notices) { notice -> SentNoticeRow(notice) }
            }
        }
    }
}

/** One card in the list: the message, how many entrants got it, and when. */
@Composable
private fun SentNoticeRow(notice: SentNotice) {
    Card(modifier = Modifier.fillMaxWidth().testTag("sentNotificationItem")) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(notice.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${notice.recipientsText()} · ${notice.timeText()}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
