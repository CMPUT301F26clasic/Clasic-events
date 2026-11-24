/*
Purpose: Shows the entrant's reservations, split into active ones that are still coming up
(US 01.06.01) and past ones that are over, cancelled or rejected (US 01.06.02). Tapping an
active reservation opens its confirmation.
Outstanding issues: The "My Lottery Win" button still opens the test lottery. It should be
replaced once the automatic draw (US 02.05.01) can send winners to their lottery.
*/
package com.example.clasicampsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Reservation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The My Reservations screen. Lists this device's reservations under Active and Past
 * (US 01.06.01, US 01.06.02).
 */
@Composable
fun CampSiteReservationScreen(
    navController: NavController,
    dbController: DatabaseController = remember { DatabaseController() }
) {
    val context = LocalContext.current
    val deviceId = AdminAccess.deviceId(context)
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load this user's reservations when the screen opens
    LaunchedEffect(deviceId) {
        try {
            reservations = dbController.getReservationsForUser(deviceId)
        } catch (e: Exception) {
            errorMessage = "Failed to load reservations: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    val active = Reservation.activeReservations(reservations, today)
    val past = Reservation.pastReservations(reservations, today)

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton")
        ) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("My Reservations", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Until the automatic draw is done, this opens the test lottery so a winner can book
        OutlinedButton(
            onClick = { navController.navigate("available_dates/test-lottery") },
            modifier = Modifier.fillMaxWidth().testTag("availableDatesButton")
        ) {
            Text("My Lottery Win")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("errorText"))
        } else {
            LazyColumn(modifier = Modifier.weight(1f).testTag("reservationsList")) {
                // US 01.06.01: reservations that are still coming up
                item {
                    Text("Active", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("activeHeader"))
                }
                if (active.isEmpty()) {
                    item { Text("No active reservations.", modifier = Modifier.testTag("noActiveText")) }
                }
                items(active) { reservation ->
                    ReservationCard(reservation) {
                        navController.navigate("reservation_confirmation/${reservation.id}")
                    }
                }

                // US 01.06.02: reservations that are over, cancelled or rejected
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Past", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("pastHeader"))
                }
                if (past.isEmpty()) {
                    item { Text("No past reservations.", modifier = Modifier.testTag("noPastText")) }
                }
                items(past) { reservation ->
                    ReservationCard(reservation, onClick = null)
                }
            }
        }
    }
}

/**
 * One reservation in the list, showing the campsite, dates, nights and status.
 * [onClick] is null for past reservations, so they can't be tapped.
 */
@Composable
private fun ReservationCard(reservation: Reservation, onClick: (() -> Unit)?) {
    val tapModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(tapModifier)
            .testTag("reservation_${reservation.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(reservation.campsiteName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Dates: ${reservation.dates.sorted().joinToString(", ")}")
            Text("Nights: ${reservation.nights()}")
            Text("Status: ${reservation.status}")
        }
    }
}
