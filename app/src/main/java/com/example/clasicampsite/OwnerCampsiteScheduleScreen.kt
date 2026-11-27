/**
 * Purpose: Lets a campsite owner view currently booked dates (#25) and filter
 * past, active, and cancelled reservations (#26) for one of their campsites, and reject an
 * upcoming reservation (US 02.08.01). Rejecting frees the dates and notifies the camper,
 * unless they turned notifications off.
 * Outstanding issues: Rejected reservations are listed under the Cancelled filter.
 */
package com.example.clasicampsite

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Reservation
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The filter chips shown above the reservation list (#26). */
private enum class ReservationFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    PAST("Past"),
    CANCELLED("Cancelled"),
}

/**
 * Returns the booked nights from [today] on, with no repeats, in order (#25).
 *
 * @param reservations every reservation for the campsite
 * @param today today's date, like "2027-05-07"
 * @return the nights taken by active reservations
 */
private fun upcomingBookedDates(reservations: List<Reservation>, today: String): List<String> =
    Reservation.bookedDates(reservations).filter { it >= today }.distinct().sorted()

/**
 * Shows booked dates and reservation history for [campsiteId], and lets the owner reject an
 * upcoming reservation (US 02.08.01).
 *
 * @param navController used to go back to My Campsites
 * @param campsiteId the Firestore ID of the campsite whose bookings are shown
 * @param dbController where the campsite and reservations are loaded from and saved
 */
@Composable
fun OwnerCampsiteScheduleScreen(
    navController: NavController,
    campsiteId: String,
    dbController: DatabaseController = remember { DatabaseController() },
) {
    val scope = rememberCoroutineScope()

    var campsiteName by remember { mutableStateOf("") }
    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var bookedDates by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf(ReservationFilter.ALL) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reservationToReject by remember { mutableStateOf<Reservation?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var rejectMessage by remember { mutableStateOf<String?>(null) }

    val todayStr = remember { LocalDate.now().toString() }

    LaunchedEffect(campsiteId) {
        try {
            isLoading = true
            errorMessage = null

            val campsite = dbController.getCampsite(campsiteId)
            campsiteName = campsite?.name ?: "Campsite"

            val loaded = dbController.getReservationsForCampsite(campsiteId)
            reservations = loaded
            bookedDates = upcomingBookedDates(loaded, todayStr)
        } catch (e: Exception) {
            errorMessage = "Failed to load schedule: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    val filteredReservations = remember(reservations, selectedFilter, todayStr) {
        reservations.filter { res ->
            when (selectedFilter) {
                ReservationFilter.ALL -> true
                ReservationFilter.CANCELLED -> res.isCancelledOrRejected()
                ReservationFilter.PAST -> !res.isCancelledOrRejected() &&
                        res.dates.maxOrNull()?.let { it < todayStr } == true
                ReservationFilter.ACTIVE -> !res.isCancelledOrRejected() &&
                        res.dates.maxOrNull()?.let { it >= todayStr } != false
            }
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
            text = "Bookings & Reservations",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("ownerScheduleTitle"),
        )
        if (campsiteName.isNotBlank()) {
            Text(campsiteName, style = MaterialTheme.typography.titleMedium)
        }

        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text("Loading bookings...")
                }
            }

            errorMessage != null -> {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            }

            else -> {
                Text(
                    text = "Currently Booked Dates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (bookedDates.isEmpty()) {
                    Text(
                        text = "No upcoming booked dates.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("noBookedDatesText"),
                    )
                } else {
                    Text(
                        text = bookedDates.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("bookedDatesList"),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Reservations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReservationFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
                        )
                    }
                }

                rejectMessage?.let { message ->
                    Text(message, modifier = Modifier.testTag("rejectMessage"))
                }

                if (filteredReservations.isEmpty()) {
                    Text(
                        text = "No ${selectedFilter.label.lowercase()} reservations.",
                        modifier = Modifier.testTag("emptyReservationsText"),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).testTag("ownerReservationsList"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredReservations, key = { it.id.ifBlank { it.hashCode().toString() } }) { res ->
                            val isPast = !res.isCancelledOrRejected() &&
                                    res.dates.maxOrNull()?.let { it < todayStr } == true
                            val displayStatus = when {
                                res.status.equals("rejected", ignoreCase = true) -> "Rejected"
                                res.status.equals("cancelled", ignoreCase = true) -> "Cancelled"
                                isPast -> "Past"
                                else -> "Active"
                            }

                            Card(modifier = Modifier.fillMaxWidth().testTag("ownerReservationItem")) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Status: $displayStatus", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (res.dates.isEmpty()) "No dates" else res.dates.sorted().joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = "Guest ID: ${res.userId} · ${res.nights()} night(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (res.canReject(todayStr)) {
                                        OutlinedButton(
                                            onClick = {
                                                reservationToReject = res
                                                rejectReason = ""
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error,
                                            ),
                                            modifier = Modifier.padding(top = 8.dp).testTag("rejectButton_${res.id}"),
                                        ) {
                                            Text("Reject")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reservationToReject?.let { res ->
        AlertDialog(
            onDismissRequest = { reservationToReject = null },
            title = { Text("Reject this reservation?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${res.dates.sorted().joinToString(", ")}. The nights open up again and the camper is notified.")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason (optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("rejectReasonField"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val reason = rejectReason
                        reservationToReject = null
                        scope.launch {
                            try {
                                dbController.rejectReservation(res.id)
                                val nowRejected = res.copy(status = "rejected")
                                reservations = reservations.map { if (it.id == res.id) nowRejected else it }
                                bookedDates = upcomingBookedDates(reservations, todayStr)
                                rejectMessage = try {
                                    /** campers who turned notifications off aren't sent one (US 01.10.01) */
                                    if (dbController.wantsNotifications(res.userId)) {
                                        dbController.sendNotification(res.userId, nowRejected.rejectionMessage(reason))
                                        "Reservation rejected. The camper was notified."
                                    } else {
                                        "Reservation rejected. The camper has notifications turned off."
                                    }
                                } catch (e: Exception) {
                                    "Reservation rejected, but the notification could not be sent."
                                }
                            } catch (e: Exception) {
                                rejectMessage = "Could not reject the reservation: ${e.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirmRejectButton"),
                ) {
                    Text("Reject reservation", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationToReject = null }) {
                    Text("Keep it")
                }
            },
        )
    }
}
