/**
 * Purpose: Lets a campsite owner update their lottery listing, set the number of winners and
 * optional waitlist capacity, view the waitlist, and draw/notify entrants
 * (US 02.04.05, US 02.04.06, US 02.04.07, US 02.09.01, #5).
 * Drawing saves the winners in a lottery so they can pick their dates (US 02.05.01).
 * Outstanding issues: The owner draws by pressing a button. Drawing automatically when the
 * registration period ends is not done.
 */
package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.Lottery
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The following ui and function layouts are from, ChatGPT "Provide a layout for my lotterymanagementscreen", 2026-11-06
 *
 *
 * Screen where a campsite owner manages lottery winner count, draws winners and notifies entrants.
 *
 * @param navController used to go back
 * @param campsiteId the Firestore ID of the campsite whose lottery is managed
 * @param dbController where the campsite, entrants and lottery are loaded from and saved
 **/
@Composable
fun LotteryManagementScreen(
    navController: NavController,
    campsiteId: String,
    dbController: DatabaseController = remember { DatabaseController() }) {
    var campsiteName by remember { mutableStateOf("") }
    var winnerCount by remember { mutableStateOf("") }
    var capacityText by remember { mutableStateOf("") }
    var capacityMessage by remember { mutableStateOf("") }
    var entrantNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var entrantCount by remember { mutableStateOf(0) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    /** errorMessage is only for when the screen can't load, actionError is for a button that failed */
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf("") }
    var campsite by remember { mutableStateOf<Campsite?>(null) }
    var drawnLottery by remember { mutableStateOf<Lottery?>(null) }

    val coroutineScope = rememberCoroutineScope()

    var isSendingNotifications by remember { mutableStateOf(false) }

    /** Load the campsite and its lottery entrants. */
    LaunchedEffect(campsiteId) {
        try {
            isLoading = true
            errorMessage = null

            val loaded = dbController.getCampsite(campsiteId)

            if (loaded == null) {
                errorMessage = "Campsite not found."
            } else {
                campsite = loaded
                campsiteName = loaded.name
                winnerCount = loaded.numberOfWinners.toString()
                capacityText = loaded.lotteryCapacity?.toString() ?: ""
                drawnLottery = dbController.getLottery(campsiteId)

                val entrants = dbController.getLotteryEntrants(campsiteId)
                entrantCount = entrants.size

                entrantNames = entrants.map { entrant ->
                    val user = dbController.getUser(entrant.userId)
                    user.name.ifBlank { "Unnamed user" }
                }
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load lottery information: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton")
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Loading lottery information...")
            }
        } else if (errorMessage != null) {
            Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        } else {
            Text(text = "Manage Lottery", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = campsiteName, style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Number of Lottery Winners", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = winnerCount,
                onValueChange = { winnerCount = it.filter(Char::isDigit)
                    successMessage = ""
                    actionError = null
                },
                label = { Text("Number of winners") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("winnerCountField"))

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                enabled = !isSaving,
                onClick = {
                    val count = winnerCount.toIntOrNull()

                    if (count == null || count <= 0) {
                        actionError = "Please enter a number of winners greater than zero."
                    } else {
                        coroutineScope.launch {
                            try {
                                isSaving = true
                                actionError = null
                                successMessage = ""

                                dbController.updateNumberOfWinners(campsiteId, count)

                                successMessage = "Number of winners successfully updated!"
                            } catch (e: Exception) {
                                actionError = "Failed to update lottery settings: ${e.localizedMessage}"
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("saveWinnerCountButton")
            ) {
                Text(if (isSaving) "Saving..." else "Save Winner Count")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lottery Waitlist Capacity",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Optional. Leave blank for unlimited entrants.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = capacityText,
                onValueChange = {
                    capacityText = it.filter(Char::isDigit)
                    capacityMessage = ""
                    actionError = null
                },
                label = { Text("Maximum entrants (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("lotteryCapacityField")
            )

            Button(
                enabled = !isSaving,
                onClick = {
                    val capacity = if (capacityText.isBlank()) null else capacityText.toIntOrNull()

                    if (capacityText.isNotBlank() && (capacity == null || capacity <= 0)) {
                        capacityMessage = "Enter a capacity greater than zero, or leave it blank."
                    } else {
                        coroutineScope.launch {
                            try {
                                isSaving = true
                                capacityMessage = ""
                                actionError = null
                                dbController.updateLotteryCapacity(campsiteId, capacity)
                                campsite = campsite?.copy(lotteryCapacity = capacity)
                                capacityMessage = if (capacity == null) {
                                    "Waitlist capacity removed; unlimited entrants allowed."
                                } else {
                                    "Waitlist capacity updated to $capacity."
                                }
                            } catch (e: Exception) {
                                capacityMessage = e.localizedMessage
                                    ?: "Failed to update waitlist capacity."
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("saveLotteryCapacityButton")
            ) {
                Text(if (isSaving) "Saving..." else "Save Waitlist Capacity")
            }

            if (capacityMessage.isNotBlank()) {
                Text(
                    text = capacityMessage,
                    color = if (capacityMessage.startsWith("Waitlist capacity") ||
                        capacityMessage.startsWith("Waitlist capacity removed")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.testTag("lotteryCapacityMessage")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val alreadyDrawn = drawnLottery?.hasBeenDrawn() == true

            Button(
                enabled = !isSaving && !isSendingNotifications && !alreadyDrawn,
                onClick = {
                    val count = winnerCount.toIntOrNull()
                    val site = campsite
                    val today = LocalDate.now()
                    if (count == null || count <= 0) {
                        actionError = "Please enter a number of winners greater than zero."
                    } else if (site == null) {
                        actionError = "Campsite not found."
                    } else if (Lottery.datesBetween(site.availableFrom, site.availableTo, today).isEmpty()) {
                        actionError = "Set this campsite's available dates first, so winners have nights to book."
                    } else {
                        coroutineScope.launch {
                            try {
                                isSendingNotifications = true
                                actionError = null
                                successMessage = ""

                                val entrants = dbController.getLotteryEntrants(campsiteId)
                                val entrantIds = entrants.map { it.userId }.distinct()
                                val pickedWinners = Lottery.pickWinners(entrantIds, count)

                                if (pickedWinners.isEmpty()) {
                                    actionError = "Nobody has joined this lottery yet."
                                } else {
                                    /** save the winners first, so they can book even if a notification fails */
                                    val lottery = Lottery.forCampsite(site, pickedWinners, today)
                                    dbController.saveLottery(lottery)
                                    drawnLottery = lottery

                                    val skipped = dbController.sendLotteryOutcomeNotifications(
                                        campsiteName = campsiteName.ifBlank { "this campsite" },
                                        entrantUserIds = entrantIds,
                                        winnerUserIds = pickedWinners,
                                    )

                                    successMessage = "Drew ${pickedWinners.size} winner(s), ${entrantIds.size - pickedWinners.size} not selected." +
                                        if (skipped > 0) " $skipped entrant(s) have notifications off." else ""
                                }
                            } catch (e: Exception) {
                                actionError = "Failed to draw winners: ${e.localizedMessage}"
                            } finally {
                                isSendingNotifications = false
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("notifyEntrantsButton"),
            ) {
                Text(
                    when {
                        isSendingNotifications -> "Drawing..."
                        alreadyDrawn -> "Winners already drawn"
                        else -> "Draw & Notify Entrants"
                    }
                )
            }

            if (alreadyDrawn) {
                Text(
                    text = "${drawnLottery?.winners?.size ?: 0} winner(s) can now pick their dates.",
                    modifier = Modifier.testTag("drawnText")
                )
            }

            if (successMessage.isNotBlank()) {
                Text(
                    text = successMessage,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("lotterySuccessMessage")
                )
            }

            actionError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("lotteryActionError")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Lottery Waitlist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$entrantCount entrants",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Capacity: ${campsite?.lotteryCapacity?.toString() ?: "Unlimited"}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (entrantNames.isEmpty()) {
                Text(
                    text = "There are currently no entrants on the waitlist.",
                    modifier = Modifier.testTag("emptyWaitlistMessage")
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("lotteryEntrantList")
                ) {
                    itemsIndexed(entrantNames) { index, name ->
                        Text(text = "${index + 1}. $name", modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("entrant_${index}"))
                    }
                }
            }
        }
    }
}