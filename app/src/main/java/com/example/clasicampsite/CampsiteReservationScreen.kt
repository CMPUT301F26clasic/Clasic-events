/**
 * Purpose: Shows the entrant's reservations, split into active ones that are still coming up
 * (US 01.06.01) and past ones that are over, cancelled or rejected (US 01.06.02). Tapping an
 * active reservation opens its confirmation, and active reservations can be cancelled (US 01.07.01).
 * After cancelling, the user gets a notification and can download the cancellation as a CSV
 * file (US 01.07.02).
 * Outstanding issues: The "My Lottery Win" button still opens the test lottery. It should be
 * replaced once the automatic draw (US 02.05.01) can send winners to their lottery. The
 * cancellation notification is sent even if the user turned notifications off.
 */
package com.example.clasicampsite

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The My Reservations screen. When it opens it loads this device's reservations and lists them
 * under Active (still coming up, US 01.06.01) and Past (over, cancelled or rejected,
 * US 01.06.02). Tapping an active reservation opens its confirmation. Active reservations have
 * a Cancel button, which asks first because cancelling can't be undone, then updates the list
 * so the reservation moves to Past right away (US 01.07.01). After cancelling, a notification is
 * sent and a button lets the user save the cancellation as a CSV file (US 01.07.02). Until the
 * automatic draw is done,
 * the My Lottery Win button opens the test lottery so a winner can book.
 *
 * @param navController used to go back and to open other screens
 * @param dbController reads and writes reservations in Firestore
 */
@Composable
fun CampSiteReservationScreen(
    navController: NavController,
    dbController: DatabaseController = remember { DatabaseController() }
) {
    val context = LocalContext.current
    val deviceId = AdminAccess.deviceId(context)
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val coroutineScope = rememberCoroutineScope()

    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cancelMessage by remember { mutableStateOf("") }
    var reservationToCancel by remember { mutableStateOf<Reservation?>(null) }
    var cancelledReservation by remember { mutableStateOf<Reservation?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val cancelled = cancelledReservation
        if (uri != null && cancelled != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(cancelled.toCsv().toByteArray())
                }
                Toast.makeText(context, "Cancellation saved as CSV", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not save the CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        OutlinedButton(
            onClick = { navController.navigate("available_dates/test-lottery") },
            modifier = Modifier.fillMaxWidth().testTag("availableDatesButton")
        ) {
            Text("My Lottery Win")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (cancelMessage.isNotBlank()) {
            Text(cancelMessage, modifier = Modifier.testTag("cancelMessage"))
            Spacer(modifier = Modifier.height(8.dp))
        }
        val cancelled = cancelledReservation
        if (cancelled != null) {
            OutlinedButton(
                onClick = { csvLauncher.launch("cancellation-${cancelled.id}.csv") },
                modifier = Modifier.fillMaxWidth().testTag("downloadCancelCsvButton")
            ) {
                Text("Download cancellation as CSV")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("errorText"))
        } else {
            LazyColumn(modifier = Modifier.weight(1f).testTag("reservationsList")) {
                item {
                    Text("Active", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("activeHeader"))
                }
                if (active.isEmpty()) {
                    item { Text("No active reservations.", modifier = Modifier.testTag("noActiveText")) }
                }
                items(active) { reservation ->
                    ReservationCard(
                        reservation,
                        onClick = { navController.navigate("reservation_confirmation/${reservation.id}") },
                        onCancel = if (reservation.canCancel(today)) {
                            { reservationToCancel = reservation }
                        } else {
                            null
                        },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Past", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("pastHeader"))
                }
                if (past.isEmpty()) {
                    item { Text("No past reservations.", modifier = Modifier.testTag("noPastText")) }
                }
                items(past) { reservation ->
                    ReservationCard(reservation, onClick = null, onCancel = null)
                }
            }
        }
    }

    val toCancel = reservationToCancel
    if (toCancel != null) {
        AlertDialog(
            onDismissRequest = { reservationToCancel = null },
            title = { Text("Cancel this reservation?") },
            text = { Text("Your nights at ${toCancel.campsiteName} will be given up. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        reservationToCancel = null
                        coroutineScope.launch {
                            try {
                                dbController.cancelReservation(toCancel.id)
                                val nowCancelled = toCancel.copy(status = "cancelled")
                                reservations = reservations.map {
                                    if (it.id == toCancel.id) nowCancelled else it
                                }
                                cancelledReservation = nowCancelled
                                cancelMessage = try {
                                    dbController.sendNotification(deviceId, nowCancelled.cancellationMessage())
                                    nowCancelled.cancellationMessage() + " A confirmation was sent to your notifications."
                                } catch (e: Exception) {
                                    nowCancelled.cancellationMessage() + " The notification could not be sent."
                                }
                            } catch (e: Exception) {
                                cancelMessage = "Failed to cancel: ${e.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirmCancelButton")
                ) {
                    Text("Cancel reservation")
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationToCancel = null }) {
                    Text("Keep it")
                }
            }
        )
    }
}

/**
 * One reservation in the list, showing the campsite, dates, nights and status.
 *
 * @param reservation the reservation to show
 * @param onClick called when the card is tapped, or null so past reservations can't be tapped
 * @param onCancel called when Cancel is tapped, or null to hide the Cancel button
 */
@Composable
private fun ReservationCard(reservation: Reservation, onClick: (() -> Unit)?, onCancel: (() -> Unit)?) {
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
            if (onCancel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancelButton_${reservation.id}")
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
