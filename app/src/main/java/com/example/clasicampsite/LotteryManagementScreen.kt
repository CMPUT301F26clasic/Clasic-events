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
import kotlinx.coroutines.launch

@Composable
fun LotteryManagementScreen(
    navController: NavController,
    campsiteId: String,
    dbController: DatabaseController = remember { DatabaseController() }) {
    var campsiteName by remember { mutableStateOf("") }
    var winnerCount by remember { mutableStateOf("") }
    var entrantNames by remember { mutableStateOf<List<String>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    // Load the campsite and its lottery entrants.
    LaunchedEffect(campsiteId) {
        try {
            isLoading = true
            errorMessage = null

            val campsite = dbController.getCampsiteLotteryDetails(campsiteId)

            if (campsite == null) {
                errorMessage = "Campsite not found."
            } else {
                campsiteName = campsite.first
                winnerCount = campsite.second.toString()

                val entrants = dbController.getLotteryEntrants(campsiteId)

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
                    errorMessage = null
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
                        errorMessage = "Please enter a number of winners greater than zero."
                    } else {
                        coroutineScope.launch {
                            try {
                                isSaving = true
                                errorMessage = null
                                successMessage = ""

                                dbController.updateNumberOfWinners(campsiteId, count)

                                successMessage = "Number of winners successfully updated!"
                            } catch (e: Exception) {
                                errorMessage = "Failed to update lottery settings: ${e.localizedMessage}"
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

            if (successMessage.isNotBlank()) {
                Text(
                    text = successMessage,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("lotterySuccessMessage")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Lottery Waitlist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${entrantNames.size} entrants",
                style = MaterialTheme.typography.bodyMedium
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