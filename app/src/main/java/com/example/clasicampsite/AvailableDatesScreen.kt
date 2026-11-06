/*
Purpose: Lets a lottery winner see the dates they can book (US 01.04.01), choose a campsite and
dates when the lottery has more than one campsite (US 01.04.02), reserve them, or decline the
reservation (US 01.04.03).
Outstanding issues: Dates are shown as a list, not a calendar.
*/
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.clasicampsite.model.Lottery
import com.example.clasicampsite.model.Reservation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AvailableDatesScreen(
    navController: NavController,
    lotteryId: String,
    dbController: DatabaseController = remember { DatabaseController() }
) {
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val coroutineScope = rememberCoroutineScope()

    var lottery by remember { mutableStateOf<Lottery?>(null) }
    var campsiteNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedCampsiteId by remember { mutableStateOf("") }
    var bookedDates by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDates by remember { mutableStateOf<List<String>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var bookingMessage by remember { mutableStateOf("") }
    var showDeclineDialog by remember { mutableStateOf(false) }

    // Load the lottery and the names of its campsites when the screen opens
    LaunchedEffect(lotteryId) {
        try {
            val loaded = dbController.getLottery(lotteryId)
            if (loaded == null) {
                errorMessage = "Lottery not found."
            } else {
                lottery = loaded
                campsiteNames = loaded.campsiteIds.associateWith { id ->
                    try {
                        dbController.getCampsite(id)?.name ?: id
                    } catch (e: Exception) {
                        id // show the ID if the campsite can't be loaded
                    }
                }
                selectedCampsiteId = loaded.campsiteIds.firstOrNull() ?: ""
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load lottery: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    // Load the booked dates whenever a different campsite is picked
    LaunchedEffect(selectedCampsiteId) {
        if (selectedCampsiteId.isNotBlank()) {
            try {
                bookedDates = Reservation.bookedDates(dbController.getReservationsForCampsite(selectedCampsiteId))
            } catch (e: Exception) {
                errorMessage = "Failed to load booked dates: ${e.localizedMessage}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton")
        ) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Available Dates", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val currentLottery = lottery
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("errorText"))
        } else if (currentLottery != null) {
            Text(currentLottery.title, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (currentLottery.hasDeclined(deviceId)) {
                Text("You declined this lottery.", modifier = Modifier.testTag("declinedText"))
            } else if (!currentLottery.isWinner(deviceId)) {
                Text("You did not win this lottery.", modifier = Modifier.testTag("notWinnerText"))
            } else {
                // US 01.04.02: pick a campsite when the lottery has more than one
                if (currentLottery.hasMultipleCampsites()) {
                    Text("Pick a campsite", fontWeight = FontWeight.Bold)
                    currentLottery.campsiteIds.forEach { id ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCampsiteId = id
                                    selectedDates = emptyList()
                                }
                                .testTag("campsite_$id")
                        ) {
                            RadioButton(
                                selected = id == selectedCampsiteId,
                                onClick = {
                                    selectedCampsiteId = id
                                    selectedDates = emptyList()
                                }
                            )
                            Text(campsiteNames[id] ?: id)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(campsiteNames[selectedCampsiteId] ?: selectedCampsiteId, fontWeight = FontWeight.Bold)
                }

                // US 01.04.01: the dates that are still free
                Text("Pick your dates", fontWeight = FontWeight.Bold)
                val dates = currentLottery.freeDates(today, bookedDates)
                if (dates.isEmpty()) {
                    Text("No dates are left.", modifier = Modifier.testTag("noDatesText"))
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).testTag("datesList")) {
                        items(dates) { date ->
                            val isSelected = date in selectedDates
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedDates = if (isSelected) selectedDates - date else selectedDates + date
                                    }
                                    .testTag("date_$date")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isSelected, onCheckedChange = null)
                                    Text(date, fontSize = 16.sp, modifier = Modifier.padding(12.dp))
                                }
                            }
                        }
                    }
                }

                Text("${selectedDates.size} night(s) selected", modifier = Modifier.testTag("selectedCountText"))
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    enabled = selectedDates.isNotEmpty() && !isSaving,
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isSaving = true
                                bookingMessage = ""
                                // Check again in case someone booked these dates while we were looking
                                val latestBooked = Reservation.bookedDates(
                                    dbController.getReservationsForCampsite(selectedCampsiteId)
                                )
                                if (selectedDates.any { it in latestBooked }) {
                                    bookedDates = latestBooked
                                    selectedDates = selectedDates.filter { it !in latestBooked }
                                    bookingMessage = "Someone just booked one of those dates. Please pick again."
                                } else {
                                    val reservation = Reservation(
                                        lotteryId = currentLottery.id,
                                        campsiteId = selectedCampsiteId,
                                        campsiteName = campsiteNames[selectedCampsiteId] ?: selectedCampsiteId,
                                        userId = deviceId,
                                        dates = selectedDates.sorted(),
                                    )
                                    dbController.saveReservation(reservation)
                                    dbController.sendNotification(
                                        deviceId,
                                        "Your reservation at ${reservation.campsiteName} is confirmed for ${reservation.dates.joinToString(", ")}."
                                    )
                                    // Changing screens has to happen on the main thread
                                    withContext(Dispatchers.Main) {
                                        navController.navigate("reservation_confirmation/${reservation.id}")
                                    }
                                }
                            } catch (e: Exception) {
                                bookingMessage = "Failed to reserve: ${e.localizedMessage}"
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("reserveButton")
                ) {
                    Text(if (isSaving) "Reserving..." else "Reserve")
                }

                // US 01.04.03: decline the reservation
                OutlinedButton(
                    onClick = { showDeclineDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("declineButton")
                ) {
                    Text("Decline")
                }

                if (bookingMessage.isNotBlank()) {
                    Text(bookingMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("bookingMessage"))
                }
            }
        }
    }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = { Text("Decline this reservation?") },
            text = { Text("You will give up your spot in this lottery.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeclineDialog = false
                        coroutineScope.launch {
                            try {
                                dbController.declineLottery(lotteryId, deviceId)
                                lottery = lottery?.let { it.copy(declined = it.declined + deviceId) }
                            } catch (e: Exception) {
                                bookingMessage = "Failed to decline: ${e.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirmDeclineButton")
                ) {
                    Text("Decline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
