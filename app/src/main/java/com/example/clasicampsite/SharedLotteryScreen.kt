/*
Purpose: Lets a campsite owner select multiple campsites they own and list them
together in a shared lottery (#69).
Outstanding issues: None.
 */
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.Lottery
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Screen where a campsite owner creates a shared lottery across multiple campsites.
 *
 * @param navController Navigation controller used to return to the previous screen
 * @param dbController Firestore helper for loading campsites and saving the lottery
 * @param ownerIdOverride Optional owner ID override for testing
 */
@Composable
fun SharedLotteryScreen(
    navController: NavController,
    dbController: DatabaseController = remember { DatabaseController() },
    ownerIdOverride: String? = null,
) {
    val context = LocalContext.current
    val ownerId = remember(ownerIdOverride) {
        ownerIdOverride ?: Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    val coroutineScope = rememberCoroutineScope()

    var ownedCampsites by remember { mutableStateOf<List<Campsite>>(emptyList()) }
    var selectedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var lotteryTitle by remember { mutableStateOf("") }
    var availableDatesText by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf("") }

    // Load all campsites belonging to this owner so they can pick which ones share the lottery.
    LaunchedEffect(ownerId) {
        try {
            isLoading = true
            errorMessage = null
            ownedCampsites = dbController.getCampsitesByOwner(ownerId)
                .sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            errorMessage = "Failed to load your campsites: ${e.localizedMessage}"
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
            text = "Create Shared Lottery",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("sharedLotteryTitle"),
        )

        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text("Loading your campsites...")
            }
        } else if (ownedCampsites.size < 2) {
            Text(
                text = "You need at least 2 registered campsites to create a shared lottery.",
                modifier = Modifier.testTag("notEnoughCampsitesText"),
            )
        } else {
            OutlinedTextField(
                value = lotteryTitle,
                onValueChange = {
                    lotteryTitle = it
                    errorMessage = null
                    successMessage = ""
                },
                label = { Text("Shared lottery title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("sharedLotteryTitleField"),
            )

            OutlinedTextField(
                value = availableDatesText,
                onValueChange = {
                    availableDatesText = it
                    errorMessage = null
                    successMessage = ""
                },
                label = { Text("Available dates (comma-separated yyyy-MM-dd)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("sharedLotteryDatesField"),
            )

            Text(
                text = "Select campsites to include (${selectedIds.size} selected):",
                fontWeight = FontWeight.SemiBold,
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("sharedLotteryCampsiteList"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(ownedCampsites, key = { it.id }) { site ->
                    val checked = site.id in selectedIds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (checked) selectedIds - site.id else selectedIds + site.id
                                errorMessage = null
                                successMessage = ""
                            }
                            .testTag("sharedSite_${site.id}"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(site.name.ifBlank { "(no name)" }, fontWeight = FontWeight.SemiBold)
                                Text(site.location, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("sharedLotteryError"))
            }

            if (successMessage.isNotBlank()) {
                Text(
                    text = successMessage,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("sharedLotterySuccess"),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                enabled = !isSaving,
                onClick = {
                    val parsedDates = availableDatesText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    val invalidDate = parsedDates.firstOrNull { runCatching { LocalDate.parse(it) }.isFailure }

                    when {
                        lotteryTitle.isBlank() -> errorMessage = "Please enter a title for the shared lottery."
                        selectedIds.size < 2 -> errorMessage = "Please select at least 2 campsites."
                        parsedDates.isEmpty() -> errorMessage = "Please enter at least one valid date."
                        invalidDate != null -> errorMessage = "Invalid date format: $invalidDate (use yyyy-MM-dd)."
                        else -> {
                            coroutineScope.launch {
                                try {
                                    isSaving = true
                                    errorMessage = null
                                    val newLottery = Lottery(
                                        title = lotteryTitle.trim(),
                                        campsiteIds = selectedIds,
                                        availableDates = parsedDates.distinct().sorted(),
                                    )
                                    dbController.createSharedLottery(newLottery)
                                    successMessage = "Shared lottery created for ${selectedIds.size} campsites!"
                                } catch (e: Exception) {
                                    errorMessage = "Failed to create shared lottery: ${e.localizedMessage}"
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("createSharedLotteryButton"),
            ) {
                Text(if (isSaving) "Creating..." else "Create Shared Lottery")
            }
        }
    }
}